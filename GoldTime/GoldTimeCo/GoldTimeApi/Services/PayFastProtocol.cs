using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using GoldTimeApi.Models;

namespace GoldTimeApi.Services;

public record PayFastSettings(string MerchantId, string MerchantKey, string Passphrase, string PublicBaseUrl)
{
    //this integration only connects to the sandbox payment URLs
    public const string ProcessUrl = "https://sandbox.payfast.co.za/eng/process";
    public const string ValidateUrl = "https://sandbox.payfast.co.za/eng/query/validate";

    public static PayFastSettings Read(IConfiguration configuration)
    {
        var id = configuration["PayFast:MerchantId"] ?? "";
        var key = configuration["PayFast:MerchantKey"] ?? "";
        var passphrase = configuration["PayFast:Passphrase"] ?? "";
        var baseUrl = configuration["PayFast:PublicBaseUrl"] ?? "";
        if (configuration["PayFast:SandboxEnabled"] != "true" || !Regex.IsMatch(id, "^[0-9]{8}$")
            || string.IsNullOrWhiteSpace(key) || string.IsNullOrWhiteSpace(passphrase)
            || !Uri.TryCreate(baseUrl, UriKind.Absolute, out var uri) || uri.Scheme != "https"
            || uri.IsLoopback || !string.IsNullOrEmpty(uri.UserInfo) || uri.Query != "" || uri.Fragment != "")
            throw new CheckoutException(503, "Sandbox payments are not configured yet.");
        return new(id, key, passphrase, baseUrl.TrimEnd('/'));
    }
}

public record PayFastForm(string ActionUrl, List<KeyValuePair<string, string>> Fields, string Environment = "sandbox");
public record PayFastNotification(string OrderId, string PaymentId, long AmountCents, string Status);

public static class PayFastProtocol
{
    public static PayFastForm CreateForm(CheckoutOrder order, PayFastSettings settings)
    {
        if (order.Currency != "ZAR" || order.TotalCents < 500 || order.Status != "pending_payment")
            throw new CheckoutException(409, "This order is not available for payment.");

        //keeps the custom-integration field order required for the signature (Payfast, n.d.)
        var fields = new List<KeyValuePair<string, string>>
        {
            new("merchant_id", settings.MerchantId), new("merchant_key", settings.MerchantKey),
            new("return_url", settings.PublicBaseUrl + "/api/payments/payfast/return"),
            new("cancel_url", settings.PublicBaseUrl + "/api/payments/payfast/cancel"),
            new("notify_url", settings.PublicBaseUrl + "/api/payments/payfast/notify"),
            new("m_payment_id", order.Id),
            new("amount", (order.TotalCents / 100m).ToString("F2", CultureInfo.InvariantCulture)),
            new("item_name", "GoldTime order " + order.Id[..12])
        };
        //sandbox uses its test wallet; the requested card/EFT choice stays on the order
        fields.Add(new("signature", Signature(Parameters(fields), settings.Passphrase)));
        return new(PayFastSettings.ProcessUrl, fields);
    }

    //matches PHP urlencode: spaces become + and other escaped bytes use uppercase hex
    public static string Encode(string value) => Uri.EscapeDataString(value).Replace("%20", "+").Replace("~", "%7E");

    public static string Parameters(IEnumerable<KeyValuePair<string, string>> fields) =>
        string.Join("&", fields.Where(x => x.Key != "signature").Select(x => x.Key + "=" + Encode(x.Value)));

    public static string Signature(string parameters, string passphrase) =>
        Convert.ToHexString(MD5.HashData(Encoding.UTF8.GetBytes(parameters + "&passphrase=" + Encode(passphrase)))).ToLowerInvariant();

    public static PayFastNotification Validate(IReadOnlyList<KeyValuePair<string, string>> fields, PayFastSettings settings)
    {
        if (fields.Select(x => x.Key).Distinct(StringComparer.Ordinal).Count() != fields.Count
            || fields.Any(x => !Regex.IsMatch(x.Key, "^[a-zA-Z0-9_]+$")))
            throw new CheckoutException(400, "Invalid payment notification fields.");
        var values = fields.ToDictionary(x => x.Key, x => x.Value, StringComparer.Ordinal);
        string Value(string key) => values.GetValueOrDefault(key, "");
        var supplied = Value("signature");
        var expected = Signature(Parameters(fields), settings.Passphrase);
        if (!Regex.IsMatch(supplied, "^[a-f0-9]{32}$") || !CryptographicOperations.FixedTimeEquals(
            Encoding.ASCII.GetBytes(supplied), Encoding.ASCII.GetBytes(expected)))
            throw new CheckoutException(400, "Invalid payment notification signature.");

        if (Value("merchant_id") != settings.MerchantId || !Regex.IsMatch(Value("m_payment_id"), "^[a-f0-9]{64}$")
            || !Regex.IsMatch(Value("pf_payment_id"), "^[0-9]{1,30}$")
            || !Regex.IsMatch(Value("amount_gross"), "^[0-9]{1,14}\\.[0-9]{2}$")
            || !decimal.TryParse(Value("amount_gross"), NumberStyles.AllowDecimalPoint, CultureInfo.InvariantCulture, out var amount)
            || amount < 5 || Value("payment_status") != "COMPLETE")
            throw new CheckoutException(400, "Payment notification details could not be verified.");
        return new(Value("m_payment_id"), Value("pf_payment_id"), checked((long)(amount * 100)), Value("payment_status"));
    }
}

/* REFERENCE LIST
Payfast. n.d. Custom Payment Integration. [Online]. Available at:
https://developers.payfast.co.za/docs#quickstart [Accessed 1 October 2026].
Used for form field order, URL encoding, MD5 signatures, sandbox URLs and notification validation.
MD5 is required by this payment protocol; it is not used for passwords.
*/
