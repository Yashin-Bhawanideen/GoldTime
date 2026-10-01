using System.Net;
using GoldTimeApi.Controllers;
using GoldTimeApi.Models;
using GoldTimeApi.Services;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace GoldTimeApi.Tests;

public class PayFastTests
{
    private static readonly PayFastSettings Settings = new("12345678", "test-key", "test-pass", "https://example.com");
    private static CheckoutOrder Order() => new()
    {
        Id = new string('a', 64), UserId = "owner", TotalCents = 1234,
        PaymentEnvironment = "sandbox", CreatedAtUtc = DateTime.UtcNow
    };

    private static IConfiguration Configuration(bool enabled = true) => new ConfigurationBuilder()
        .AddInMemoryCollection(new Dictionary<string, string?>
        {
            ["PayFast:SandboxEnabled"] = enabled ? "true" : "false", ["PayFast:MerchantId"] = Settings.MerchantId,
            ["PayFast:MerchantKey"] = Settings.MerchantKey, ["PayFast:Passphrase"] = Settings.Passphrase,
            ["PayFast:PublicBaseUrl"] = Settings.PublicBaseUrl
        }).Build();

    private static List<KeyValuePair<string, string>> Notification(string amount = "12.34", string status = "COMPLETE",
        string merchant = "12345678", string paymentId = "12345")
    {
        var fields = new List<KeyValuePair<string, string>>
        {
            new("m_payment_id", new string('a', 64)), new("pf_payment_id", paymentId),
            new("payment_status", status), new("item_name", "Gold & Silver!"), new("item_description", ""),
            new("amount_gross", amount), new("merchant_id", merchant)
        };
        fields.Add(new("signature", PayFastProtocol.Signature(PayFastProtocol.Parameters(fields), Settings.Passphrase)));
        return fields;
    }

    [Fact]
    public void FormUsesSavedAmountAndSandboxWithoutExposingPassphrase()
    {
        var form = PayFastProtocol.CreateForm(Order(), Settings);
        Assert.Equal("https://sandbox.payfast.co.za/eng/process", form.ActionUrl);
        Assert.Equal("sandbox", form.Environment);
        var values = form.Fields.ToDictionary(x => x.Key, x => x.Value);
        Assert.Equal("12.34", values["amount"]);
        Assert.Equal(Order().Id, values["m_payment_id"]);
        Assert.Equal("https://example.com/api/payments/payfast/notify", values["notify_url"]);
        Assert.False(values.ContainsKey("passphrase"));
        Assert.DoesNotContain(Settings.Passphrase, values.Values);
        Assert.False(values.ContainsKey("payment_method"));
        Assert.Equal("merchant_id", form.Fields[0].Key);
        Assert.Equal("signature", form.Fields[^1].Key);
    }

    [Fact]
    public void EncodingMatchesFormRulesAndRetainsEmptyNotificationFields()
    {
        Assert.Equal("Gold+%26+Silver%21+%7E+%2F+%2B", PayFastProtocol.Encode("Gold & Silver! ~ / +"));
        Assert.Contains("item_description=&amount_gross=", PayFastProtocol.Parameters(Notification()));
        Assert.DoesNotContain("signature=", PayFastProtocol.Parameters(Notification()));
        Assert.Equal("9c150ad1ed05f093883c7d2e92efb5b4", PayFastProtocol.Signature(
            "item_name=Gold+%26+Silver%21&amount=12.34", "test-pass"));
    }

    [Fact]
    public void MissingConfigurationDoesNotEnablePayments()
    {
        Assert.Equal(503, Assert.Throws<CheckoutException>(() => PayFastSettings.Read(new ConfigurationBuilder().Build())).StatusCode);
        Assert.Equal(503, Assert.Throws<CheckoutException>(() => PayFastSettings.Read(Configuration(false))).StatusCode);
    }

    [Theory]
    [InlineData("http://example.com")] [InlineData("https://localhost")]
    [InlineData("https://example.com?host=other")] [InlineData("https://user:password@example.com")]
    public void RejectsInvalidCallbackConfiguration(string url)
    {
        var configuration = Configuration(); configuration["PayFast:PublicBaseUrl"] = url;
        Assert.Throws<CheckoutException>(() => PayFastSettings.Read(configuration));
    }

    [Fact]
    public void RejectsTamperedSignatureAndDuplicateFields()
    {
        var fields = Notification(); fields[5] = new("amount_gross", "99.99");
        Assert.Throws<CheckoutException>(() => PayFastProtocol.Validate(fields, Settings));
        fields = Notification(); fields.Add(new("amount_gross", "12.34"));
        Assert.Throws<CheckoutException>(() => PayFastProtocol.Validate(fields, Settings));
    }

    [Theory]
    [InlineData("12.345", "COMPLETE", "12345678")]
    [InlineData("12,34", "COMPLETE", "12345678")]
    [InlineData("-12.34", "COMPLETE", "12345678")]
    [InlineData("0.00", "COMPLETE", "12345678")]
    [InlineData("12.34", "CANCELLED", "12345678")]
    [InlineData("12.34", "COMPLETE", "87654321")]
    public void RejectsInvalidNotificationDetailsEvenWithValidSignature(string amount, string status, string merchant)
    {
        Assert.Throws<CheckoutException>(() => PayFastProtocol.Validate(Notification(amount, status, merchant), Settings));
    }

    [Fact]
    public void RequiresExactOrderAmountAndSandboxEnvironment()
    {
        var notification = PayFastProtocol.Validate(Notification(), Settings);
        var order = Order();
        SandboxPaymentStore.ValidateConfirmation(order, notification);
        order.TotalCents++;
        Assert.Throws<CheckoutException>(() => SandboxPaymentStore.ValidateConfirmation(order, notification));
        order = Order(); order.PaymentEnvironment = "live";
        Assert.Throws<CheckoutException>(() => SandboxPaymentStore.ValidateConfirmation(order, notification));
    }

    [Fact]
    public void AllowsSamePaymentReplayButRejectsAnotherPaymentForTheOrder()
    {
        var order = Order(); order.Status = "sandbox_paid"; order.PayFastPaymentId = "12345";
        SandboxPaymentStore.ValidateConfirmation(order, PayFastProtocol.Validate(Notification(), Settings));
        Assert.Equal(409, Assert.Throws<CheckoutException>(() => SandboxPaymentStore.ValidateConfirmation(order,
            PayFastProtocol.Validate(Notification(paymentId: "99999"), Settings))).StatusCode);
    }

    [Theory]
    [InlineData("197.97.145.144", true)] [InlineData("197.97.145.159", true)]
    [InlineData("197.97.145.160", false)] [InlineData("41.74.179.192", true)]
    [InlineData("41.74.179.223", true)] [InlineData("41.74.179.224", false)]
    [InlineData("102.216.36.15", true)] [InlineData("102.216.36.16", false)]
    [InlineData("102.216.36.128", true)] [InlineData("102.216.36.143", true)]
    [InlineData("102.216.36.144", false)] [InlineData("144.126.193.139", true)]
    [InlineData("127.0.0.1", false)] [InlineData("::ffff:144.126.193.139", true)]
    [InlineData("2001::906e:c18b", false)]
    public void ChecksDocumentedNotificationAddresses(string address, bool allowed)
    {
        Assert.Equal(allowed, PayFastSource.IsPayFast(IPAddress.Parse(address)));
    }

    [Fact]
    public void IgnoresSpoofedForwardingHeadersUnlessPeerIsTrusted()
    {
        var attacker = IPAddress.Parse("192.0.2.50");
        Assert.Equal(attacker, PayFastSource.ClientIp(attacker, "144.126.193.139", []));
        var proxy = IPAddress.Parse("10.0.0.1");
        Assert.Equal(attacker, PayFastSource.ClientIp(proxy, "144.126.193.139, 192.0.2.50", ["10.0.0.1"]));
        Assert.Equal(IPAddress.Parse("144.126.193.139"), PayFastSource.ClientIp(proxy, "144.126.193.139", ["10.0.0.1"]));
        Assert.Null(PayFastSource.ClientIp(proxy, "invalid", ["10.0.0.1"]));
    }

    [Theory]
    [InlineData("VALID", 200, true)] [InlineData("INVALID", 200, false)]
    [InlineData("VALID", 503, false)] [InlineData("<html>error</html>", 200, false)]
    public async Task OnlyConfirmedNotificationsReachTheOrderStore(string body, int status, bool accepted)
    {
        var handler = new ValidationHandler(body, status);
        using var client = new HttpClient(handler);
        var store = new FakeStore();
        var service = new PayFastService(client, store, Configuration());
        if (accepted) await service.NotifyAsync(Notification(), IPAddress.Parse("144.126.193.139"), default);
        else await Assert.ThrowsAsync<CheckoutException>(() => service.NotifyAsync(Notification(), IPAddress.Parse("144.126.193.139"), default));
        Assert.Equal(accepted, store.Confirmed);
        Assert.Equal(PayFastSettings.ValidateUrl, handler.Url);
        Assert.DoesNotContain("signature=", handler.Body);
        Assert.DoesNotContain("passphrase", handler.Body);
    }

    [Fact]
    public async Task InvalidSourceDoesNotContactPayfastOrSaveAnOrder()
    {
        var handler = new ValidationHandler("VALID", 200);
        using var client = new HttpClient(handler);
        var store = new FakeStore();
        var service = new PayFastService(client, store, Configuration());
        await Assert.ThrowsAsync<CheckoutException>(() => service.NotifyAsync(Notification(), IPAddress.Loopback, default));
        Assert.Null(handler.Url); Assert.False(store.Confirmed);
    }

    [Fact]
    public void BrowserReturnAndCancelDoNotConfirmPayment()
    {
        using var client = new HttpClient(new ValidationHandler("VALID", 200));
        var store = new FakeStore();
        var controller = new PayFastController(new PayFastService(client, store, Configuration()), NullLogger<PayFastController>.Instance)
        { ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() } };
        Assert.Contains("check the verified payment status", controller.Return().Content);
        Assert.Contains("cart has not been cleared", controller.Cancel().Content);
        Assert.False(store.Confirmed);
    }

    [Fact]
    public void OnlyOwnerCanStartPaymentForARecentUnpaidOrder()
    {
        var order = Order();
        SandboxPaymentStore.ValidateStart(order, "owner", DateTime.UtcNow);
        Assert.Equal(404, Assert.Throws<CheckoutException>(() => SandboxPaymentStore.ValidateStart(order, "other-user", DateTime.UtcNow)).StatusCode);
        Assert.Equal(409, Assert.Throws<CheckoutException>(() => SandboxPaymentStore.ValidateStart(order, "owner", DateTime.UtcNow.AddMinutes(31))).StatusCode);
        order.Status = "sandbox_paid";
        Assert.Equal(409, Assert.Throws<CheckoutException>(() => SandboxPaymentStore.ValidateStart(order, "owner", DateTime.UtcNow)).StatusCode);
    }

    [Fact]
    public async Task NotificationControllerPreservesSignedFormValues()
    {
        using var client = new HttpClient(new ValidationHandler("VALID", 200));
        var store = new FakeStore();
        var controller = new PayFastController(new PayFastService(client, store, Configuration()), NullLogger<PayFastController>.Instance)
        { ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() } };
        var fields = Notification();
        var body = string.Join("&", fields.Select(x => x.Key + "=" + PayFastProtocol.Encode(x.Value)));
        controller.Request.Body = new MemoryStream(System.Text.Encoding.UTF8.GetBytes(body));
        controller.HttpContext.Items[PayFastSource.SourceItem] = IPAddress.Parse("144.126.193.139");
        Assert.IsType<OkResult>(await controller.Notify(default));
        Assert.True(store.Confirmed);
    }

    private class ValidationHandler(string body, int status) : HttpMessageHandler
    {
        public string? Url { get; private set; }
        public string Body { get; private set; } = "";
        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            Url = request.RequestUri!.ToString();
            Body = await request.Content!.ReadAsStringAsync(cancellationToken);
            return new HttpResponseMessage((HttpStatusCode)status) { Content = new StringContent(body) };
        }
    }

    private class FakeStore : ISandboxPaymentStore
    {
        public bool Confirmed { get; private set; }
        public Task<CheckoutOrder> PrepareAsync(string orderId, string userId, CancellationToken token) => Task.FromResult(Order());
        public Task ConfirmAsync(PayFastNotification notification, CancellationToken token)
        {
            SandboxPaymentStore.ValidateConfirmation(Order(), notification);
            Confirmed = true;
            return Task.CompletedTask;
        }
    }
}
