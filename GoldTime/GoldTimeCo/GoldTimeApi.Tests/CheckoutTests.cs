using System.Security.Claims;
using GoldTimeApi.Controllers;
using GoldTimeApi.Models;
using GoldTimeApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace GoldTimeApi.Tests;

public class CheckoutTests
{
    private static CreateOrderRequest Request() => new()
    {
        RequestId = Guid.NewGuid(), Items = [new() { ProductId = "gold-bar", Quantity = 2 }],
        PaymentMethod = "CARD", Delivery = new()
        {
            FullName = "Test Customer", Phone = "+27 (82) 123-4567", StreetAddress = "10 Test Road",
            City = "Johannesburg", Province = "Gauteng", PostalCode = "0123"
        }
    };

    private static Dictionary<string, CheckoutProduct> Products() => new()
    {
        ["gold-bar"] = new() { Name = "Gold bar", UnitPriceCents = 10005, Available = true, StockQuantity = 10 }
    };

    [Fact]
    public void CalculatesExactCentsAndAddsDeliveryOnce()
    {
        var order = OrderPricing.Create("owner", Request(), Products(), 15000);
        Assert.Equal(20010, order.SubtotalCents);
        Assert.Equal(35010, order.TotalCents);
        Assert.Equal("pending_payment", order.Status);
        Assert.Equal("ZAR", order.Currency);
        Assert.Equal("owner", order.UserId);
    }

    [Theory]
    [InlineData(0)] [InlineData(-1)] [InlineData(100)]
    public void RejectsInvalidQuantities(int quantity)
    {
        var request = Request(); request.Items[0].Quantity = quantity;
        Assert.Equal(400, Assert.Throws<CheckoutException>(() => OrderPricing.Validate(request)).StatusCode);
    }

    [Theory]
    [InlineData("phone")] [InlineData("province")] [InlineData("postal")] [InlineData("name")]
    [InlineData("method")] [InlineData("id")] [InlineData("empty")] [InlineData("duplicate")]
    [InlineData("path")] [InlineData("nullItem")] [InlineData("nullDelivery")] [InlineData("nullItems")]
    public void RejectsInvalidCheckout(string field)
    {
        var request = Request();
        switch (field)
        {
            case "phone": request.Delivery.Phone = "12+3456789"; break;
            case "province": request.Delivery.Province = "Unknown"; break;
            case "postal": request.Delivery.PostalCode = "ABCD"; break;
            case "name": request.Delivery.FullName = "   "; break;
            case "method": request.PaymentMethod = "PAID"; break;
            case "id": request.RequestId = Guid.Empty; break;
            case "empty": request.Items.Clear(); break;
            case "duplicate": request.Items.Add(request.Items[0]); break;
            case "path": request.Items[0].ProductId = "../../orders"; break;
            case "nullItem": request.Items[0] = null!; break;
            case "nullDelivery": request.Delivery = null!; break;
            case "nullItems": request.Items = null!; break;
        }
        Assert.Equal(400, Assert.Throws<CheckoutException>(() => OrderPricing.Validate(request)).StatusCode);
    }

    [Theory]
    [InlineData("missing")] [InlineData("stock")] [InlineData("unavailable")] [InlineData("price")]
    public void RejectsProductsThatCannotBePurchased(string problem)
    {
        var products = Products();
        switch (problem)
        {
            case "missing": products.Clear(); break;
            case "stock": products["gold-bar"].StockQuantity = 1; break;
            case "unavailable": products["gold-bar"].Available = false; break;
            case "price": products["gold-bar"].UnitPriceCents = 0; break;
        }
        Assert.Equal(409, Assert.Throws<CheckoutException>(() => OrderPricing.Create("owner", Request(), products, 15000)).StatusCode);
    }

    [Fact]
    public void RejectsInvalidDeliveryConfiguration()
    {
        Assert.Equal(503, Assert.Throws<CheckoutException>(() => OrderPricing.Create("owner", Request(), Products(), -1)).StatusCode);
    }

    [Fact]
    public void RetryIdentityIsStableAndSeparateForDifferentUsers()
    {
        var request = Request();
        Assert.Equal(OrderPricing.OrderId("one", request.RequestId), OrderPricing.OrderId("one", request.RequestId));
        Assert.NotEqual(OrderPricing.OrderId("one", request.RequestId), OrderPricing.OrderId("two", request.RequestId));
        var fingerprint = OrderPricing.Fingerprint(request);
        request.Delivery.City = "Cape Town";
        Assert.NotEqual(fingerprint, OrderPricing.Fingerprint(request));
    }

    private static OrdersController Controller(FakeStore store, string? uid)
    {
        var controller = new OrdersController(store, NullLogger<OrdersController>.Instance);
        controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() };
        if (uid is not null) controller.HttpContext.User = new ClaimsPrincipal(new ClaimsIdentity([new Claim("sub", uid)], "test"));
        return controller;
    }

    [Fact]
    public async Task AnonymousCallsDoNotAccessTheStore()
    {
        var store = new FakeStore(); var controller = Controller(store, null);
        Assert.IsType<UnauthorizedResult>(await controller.Create(Request(), default));
        Assert.IsType<UnauthorizedResult>(await controller.Get(new string('a', 64), default));
        Assert.False(store.Called);
        Assert.NotEmpty(typeof(OrdersController).GetCustomAttributes(typeof(AuthorizeAttribute), true));
    }

    [Theory]
    [InlineData("owner", true)] [InlineData("another-user", false)]
    public async Task OnlyTheOwnerCanReadTheOrder(string uid, bool allowed)
    {
        var result = await Controller(new FakeStore(), uid).Get(new string('a', 64), default);
        if (allowed) Assert.IsType<OkObjectResult>(result);
        else Assert.IsType<NotFoundResult>(result);
    }

    [Fact]
    public async Task CreationUsesTokenIdentity()
    {
        var store = new FakeStore();
        Assert.IsType<OkObjectResult>(await Controller(store, "signed-in-user").Create(Request(), default));
        Assert.Equal("signed-in-user", store.UserId);
    }

    [Fact]
    public async Task StoreFailureReturnsHelpfulErrorWithoutExceptionDetails()
    {
        var result = Assert.IsType<ObjectResult>(await Controller(new FakeStore { Fail = true }, "owner").Create(Request(), default));
        Assert.Equal(503, result.StatusCode);
        var problem = Assert.IsType<ProblemDetails>(result.Value);
        Assert.DoesNotContain("private", problem.Detail!);
        Assert.Contains("same request ID", problem.Detail!);
    }

    private class FakeStore : IOrderStore
    {
        public bool Called { get; private set; }
        public bool Fail { get; init; }
        public string? UserId { get; private set; }
        public Task<CheckoutOrder> CreateAsync(string userId, CreateOrderRequest request, CancellationToken cancellationToken)
        {
            Called = true; UserId = userId;
            if (Fail) throw new Exception("private database details");
            return Task.FromResult(OrderPricing.Create(userId, request, Products(), 15000));
        }
        public Task<CheckoutOrder?> FindAsync(string orderId, CancellationToken cancellationToken)
        {
            Called = true;
            return Task.FromResult<CheckoutOrder?>(new CheckoutOrder { Id = orderId, UserId = "owner" });
        }
    }
}
