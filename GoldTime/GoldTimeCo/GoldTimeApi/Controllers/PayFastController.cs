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
    [AllowAnonymous]
    [HttpGet("api/payments/payfast/return")]
    public ContentResult Return() => Content("Sandbox checkout finished. Return to GoldTime to check the verified payment status.", "text/plain");

    [AllowAnonymous]
    [HttpGet("api/payments/payfast/cancel")]
    public ContentResult Cancel() => Content("Sandbox checkout closed. Return to GoldTime. Your cart has not been cleared; check the order status before retrying.", "text/plain");
}
