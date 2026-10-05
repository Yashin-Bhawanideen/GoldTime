using System.Security.Claims;
using System.Text.RegularExpressions;
using GoldTimeApi.Models;
using GoldTimeApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GoldTimeApi.Controllers;

[ApiController]
[Route("api/orders")]
[Authorize]
public class OrdersController(IOrderStore orders, ILogger<OrdersController> logger) : ControllerBase
{
//gets the signed-in user's ID from the token claims
//Firebase tokens use "user_id", so fall back to the standard "sub" claim if it is missing
    private string? Uid => User.FindFirstValue("user_id") ?? User.FindFirstValue("sub");

//POST api/orders - creates a new checkout order for the signed-in user
    [HttpPost]
    [RequestSizeLimit(32768)]
    public async Task<IActionResult> Create(CreateOrderRequest request, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(Uid)) return Unauthorized();

        try
        {
            var order = await orders.CreateAsync(Uid, request, cancellationToken);
            return Ok(ToResponse(order));
        }
        catch (CheckoutException ex)
        {
            return Problem(statusCode: ex.StatusCode, detail: ex.Message);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Could not read order history.");

            return Problem(
                statusCode: 503,
                detail: $"Could not load your order history: {ex.Message}"
            );
        }
    }

    [HttpGet("history")]
    public async Task<IActionResult> History(CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(Uid)) return Unauthorized();

        try
        {
            var orderHistory = await orders.GetForUserAsync(
                Uid,
                cancellationToken
            );

            return Ok(orderHistory.Select(ToResponse));
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Could not read order history.");

            return Problem(
                statusCode: 503,
                detail: "Could not load your order history. Please try again."
            );
        }
    }
    //GET api/orders/{id} - returns a single order, only if it belongs to the signed-in user
    [HttpGet("{id}")]
    public async Task<IActionResult> Get(
        string id,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(Uid)) return Unauthorized();

        if (!Regex.IsMatch(id, "^[a-f0-9]{64}$"))
            return NotFound();

        try
        {
            var order = await orders.FindAsync(
                id,
                cancellationToken
            );

            if (order is null || order.UserId != Uid)
                return NotFound();

            return Ok(ToResponse(order));
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Could not read checkout order.");

            return Problem(
                statusCode: 503,
                detail: "Could not load your order. Please try again."
            );
        }
    }
//maps the CheckoutOrder entity to an anonymous response object
//this controls exactly which fields the app receives (e.g. UserId is intentionally left out)
    private static object ToResponse(CheckoutOrder order) => new
    {
        order.Id,
        order.Items,
        order.Delivery,
        order.PaymentMethod,
        order.Status,
        order.Currency,
        order.SubtotalCents,
        order.DeliveryFeeCents,
        order.TotalCents,
        order.CreatedAtUtc,
        order.PaymentEnvironment,
        order.PayFastPaymentId,
        order.PaidAtUtc
    };
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
