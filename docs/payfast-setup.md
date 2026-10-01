# PayFast sandbox setup

These settings are for the API maintainer. This integration supports the PayFast sandbox only. Test transactions do not transfer money or authorise fulfilment.

## Server settings

Create a sandbox account at https://sandbox.payfast.co.za and set a passphrase in its account settings. Add the following Azure application settings, or equivalent local .NET user secrets. Do not put the key or passphrase in Git or Android.

| Azure setting | Value |
| --- | --- |
| PayFast__SandboxEnabled | true |
| PayFast__MerchantId | Merchant ID from the sandbox account |
| PayFast__MerchantKey | Merchant key from the sandbox account |
| PayFast__Passphrase | The same passphrase set in the sandbox account |
| PayFast__PublicBaseUrl | Public HTTPS base URL of the deployed GoldTime API, without a trailing slash |
| PayFast__TrustedProxyAddresses | Comma-separated IP addresses of trusted reverse proxies, only when required by the hosting configuration |

Configure proxy addresses with the hosting administrator. Without them, the API checks the directly connected IP. Forwarded addresses are accepted only through the explicitly listed proxies. Do not add client IPs or broad public ranges to this setting. Verify the actual incoming address chain on Azure during deployment; the general forwarded-header configuration alone is not sufficient for this endpoint.

The API must be reachable over public HTTPS for PayFast to deliver notifications. Existing Firebase credentials, product documents and delivery-fee settings are also required; see [checkout-api.md](checkout-api.md). Missing payment settings return 503 and leave checkout unpaid.

## Endpoints

`POST /api/orders/{id}/payment` requires the owner's Firebase bearer token. The order must be pending, priced in ZAR, at least R5, and created within the preceding 30 minutes. It returns `actionUrl`, an ordered `fields` array of `{key, value}` pairs, and `environment: "sandbox"`. Submit those fields as an HTML-form POST to the returned sandbox URL. Do not sort or change the values. The passphrase is never included in the response.

The sandbox uses a test wallet, so card/EFT selection stays on the GoldTime order rather than restricting the sandbox form to a real payment method.

`POST /api/payments/payfast/notify` accepts PayFast's form-encoded notification without a Firebase token. It checks the signature, merchant, source IP, order amount and PayFast's server validation response. The receipt and `sandbox_paid` status are stored together in a Firestore transaction. Repeating the same receipt does not apply the payment again; a different payment reference for an already-paid order is rejected for investigation. Acknowledgement is sent only after the database write succeeds.

`GET /api/payments/payfast/return` and `/cancel` display messages only. They never change payment status. The app must retrieve the order through authenticated `GET /api/orders/{id}` and check `status` and `paymentEnvironment`. Display `sandbox_paid` as a sandbox confirmation, not a real payment. Retain Cart contents while payment is pending, cancelled or unverified.

## Verification

Run `dotnet test GoldTimeCo/GoldTimeApi.Tests/GoldTimeApi.Tests.csproj` from the repository root. Payment tests use a fake order store and HTTP handler, including signed form parsing. They do not establish that Azure, Firestore or PayFast is reachable.

In an authorised sandbox environment, verify a complete payment, a delayed notification, closing checkout, a duplicate notification and an unavailable API. Confirm that return/cancel pages cannot mark an order paid. Check the notification result in the sandbox dashboard; sandbox notifications are sent once, so investigate failures and replay the test as needed. Verify actual Firestore transaction concurrency before relying on duplicate handling.

No stock is reserved or deducted by sandbox payments. Live payment support requires separate implementation and testing, including stock reservation, expired-order handling, fulfilment and reconciliation. The live PayFast URL is not configurable in this integration.

## References

Payfast. n.d. Custom Payment Integration. [Online]. Available at: https://developers.payfast.co.za/docs#quickstart [Accessed 1 October 2026].

Payfast. n.d. Ports and IP addresses. [Online]. Available at: https://developers.payfast.co.za/docs/itn-instant-transaction-notification/ [Accessed 1 October 2026].
