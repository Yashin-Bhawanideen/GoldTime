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
    private string? Uid => User.FindFirstValue("user_id") ?? User.FindFirstValue("sub");

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
        catch (CheckoutException ex) { return Problem(statusCode: ex.StatusCode, detail: ex.Message); }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Could not save checkout order.");
            return Problem(statusCode: 503, detail: "Could not save your order. Retry with the same request ID.");
        }
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> Get(string id, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(Uid)) return Unauthorized();
        if (!Regex.IsMatch(id, "^[a-f0-9]{64}$")) return NotFound();
        try
        {
            var order = await orders.FindAsync(id, cancellationToken);
            //only the owner may read the order; the app cannot set the owner or payment status
            if (order is null || order.UserId != Uid) return NotFound();
            return Ok(ToResponse(order));
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "Could not read checkout order.");
            return Problem(statusCode: 503, detail: "Could not load your order. Please try again.");
        }
    }

    private static object ToResponse(CheckoutOrder order) => new
    {
        order.Id, order.Items, order.Delivery, order.PaymentMethod, order.Status, order.Currency,
        order.SubtotalCents, order.DeliveryFeeCents, order.TotalCents, order.CreatedAtUtc,
        order.PaymentEnvironment, order.PayFastPaymentId, order.PaidAtUtc
    };
}
