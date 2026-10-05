using System.Net;
using System.Security.Claims;
using System.Text.RegularExpressions;
using GoldTimeApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GoldTimeApi.Controllers;

[ApiController]
public class PayFastController(PayFastService payments, ILogger<PayFastController> logger) : ControllerBase
{
//POST api/orders/{id}/payment - starts a PayFast sandbox payment for one of the user's orders
//requires a signed-in user
    [Authorize]
    [HttpPost("api/orders/{id}/payment")]
    public async Task<IActionResult> Begin(string id, CancellationToken token)
    {
        var uid = User.FindFirstValue("user_id") ?? User.FindFirstValue("sub");
        if (string.IsNullOrWhiteSpace(uid)) return Unauthorized();
        if (!Regex.IsMatch(id, "^[a-f0-9]{64}$")) return NotFound();
        Response.Headers.CacheControl = "no-store";
        try { return Ok(await payments.BeginAsync(id, uid, token)); }
        catch (CheckoutException ex) { return Problem(statusCode: ex.StatusCode, detail: ex.Message); }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Could not prepare sandbox payment.");
            return Problem(statusCode: 503, detail: "Could not start the sandbox payment. Please retry.");
        }
    }
//POST api/payments/payfast/notify - the ITN (Instant Transaction Notification) webhook that PayFast's servers call
//anonymous because PayFast cannot sign in; the request is verified inside the service instead
    [AllowAnonymous]
    [HttpPost("api/payments/payfast/notify")]
    [RequestSizeLimit(32768)]
    [Consumes("application/x-www-form-urlencoded")]
    public async Task<IActionResult> Notify(CancellationToken token)
    {
        try
        {
            //preserves the received field order and blank values for signature checking
            using var reader = new StreamReader(Request.Body);
            var body = await reader.ReadToEndAsync(token);
            var pieces = body.Split('&');
            if (pieces.Length is < 1 or > 100) return BadRequest();
            var fields = new List<KeyValuePair<string, string>>();
            foreach (var piece in pieces)
            {
                var separator = piece.IndexOf('=');
                if (separator <= 0) return BadRequest();
                fields.Add(new(WebUtility.UrlDecode(piece[..separator]), WebUtility.UrlDecode(piece[(separator + 1)..])));
            }
            await payments.NotifyAsync(fields, HttpContext.Items[PayFastSource.SourceItem] as IPAddress, token);
            //acknowledges only after validation and the database transaction have succeeded
            return Ok();
        }
        //rejected notifications are logged as warnings (not errors) and return only the status code, giving nothing away to an attacker
        catch (CheckoutException ex)
        {
            logger.LogWarning("Sandbox notification rejected with status {StatusCode}.", ex.StatusCode);
            return StatusCode(ex.StatusCode);
        }
        catch (Exception ex) when (ex is not OperationCanceledException || !token.IsCancellationRequested)
        {
            logger.LogError(ex, "Could not process sandbox payment notification.");
            return StatusCode(503);
        }
    }

    //returning from the browser never changes the payment status
//GET api/payments/payfast/return - the page the user lands on after finishing payment on PayFast
//it only shows a message; the real payment status is set by the verified notify webhook above
    [AllowAnonymous]
    [HttpGet("api/payments/payfast/return")]
    public ContentResult Return() => Content("Sandbox checkout finished. Return to GoldTime to check the verified payment status.", "text/plain");

//GET api/payments/payfast/cancel - the page the user lands on if they cancel or close the PayFast checkout
//it also only shows a message and does not clear the cart or change the order
    [AllowAnonymous]
    [HttpGet("api/payments/payfast/cancel")]
    public ContentResult Cancel() => Content("Sandbox checkout closed. Return to GoldTime. Your cart has not been cleared; check the order status before retrying.", "text/plain");
}

/*
 References
Gideon, 2012. Difference between ApiController and Controller in ASP.NET MVC. [Online] 
Available at: https://stackoverflow.com/questions/9494966/difference-between-apicontroller-and-controller-in-asp-net-mvc
Microsoft, 2024. Tutorial: Create a controller-based web API with ASP.NET Core. [Online] 
Available at: https://learn.microsoft.com/en-us/aspnet/core/tutorials/first-web-api?view=aspnetcore-10.0&tabs=visual-studio
Microsoft, 2026. Create web APIs with ASP.NET Core. [Online] 
Available at: https://learn.microsoft.com/en-us/aspnet/core/web-api/?view=aspnetcore-10.0


 */
