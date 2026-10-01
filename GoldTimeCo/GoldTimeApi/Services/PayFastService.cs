using System.Net;
using System.Text;

namespace GoldTimeApi.Services;

public class PayFastService(HttpClient client, ISandboxPaymentStore orders, IConfiguration configuration)
{
    public async Task<PayFastForm> BeginAsync(string orderId, string userId, CancellationToken token)
    {
        var settings = PayFastSettings.Read(configuration);
        var order = await orders.PrepareAsync(orderId, userId, token);
        return PayFastProtocol.CreateForm(order, settings);
    }

    public async Task NotifyAsync(IReadOnlyList<KeyValuePair<string, string>> fields, IPAddress? source, CancellationToken token)
    {
        var settings = PayFastSettings.Read(configuration);
        var notification = PayFastProtocol.Validate(fields, settings);
        if (!PayFastSource.IsPayFast(source)) throw new CheckoutException(400, "Payment notification source could not be verified.");

        //asks Payfast to verify the notification before updating the order (Payfast, n.d.)
        using var content = new StringContent(PayFastProtocol.Parameters(fields), Encoding.UTF8, "application/x-www-form-urlencoded");
        using var response = await client.PostAsync(PayFastSettings.ValidateUrl, content, token);
        if (!response.IsSuccessStatusCode) throw new CheckoutException(503, "Payment validation is temporarily unavailable.");
        if ((await response.Content.ReadAsStringAsync(token)).Trim() != "VALID")
            throw new CheckoutException(400, "Payment notification was not confirmed by Payfast.");
        await orders.ConfirmAsync(notification, token);
    }
}

/* REFERENCE LIST
Payfast. n.d. Custom Payment Integration: Confirm payment. [Online]. Available at:
https://developers.payfast.co.za/docs#quickstart [Accessed 1 October 2026].
*/
