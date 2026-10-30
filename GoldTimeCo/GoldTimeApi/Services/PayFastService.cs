using System.Net;
using System.Text;

namespace GoldTimeApi.Services;
//service that connects the PayFast controller to the order store and the PayFast sandbox
//primary constructor: the HTTP client, the sandbox payment store and the app configuration are injected through dependency injection

public class PayFastService(HttpClient client, ISandboxPaymentStore orders, IConfiguration configuration)
{
    //prepares a payment for an order and returns the signed form the app sends the user to PayFast with
    public async Task<PayFastForm> BeginAsync(string orderId, string userId, CancellationToken token)
    {
        var settings = PayFastSettings.Read(configuration);
        var order = await orders.PrepareAsync(orderId, userId, token);
        return PayFastProtocol.CreateForm(order, settings);
    }

    //handles a notification (ITN) from PayFast; the order is only updated if every check passes
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
