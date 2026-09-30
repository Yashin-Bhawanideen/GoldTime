# Sector 4 pending orders

This is the backend foundation for checkout, not a completed payment integration. The Android checkout currently runs with sample data in Compose previews. It does not call these endpoints yet. No Azure deployment or live Firestore write has been verified for this change.

## Required server setup

Use the API's existing Firebase ID-token authentication and Firestore server credentials. Do not copy credentials into Android or Git. Firestore client rules remain deny-all; this API uses server credentials and checks order ownership itself.

The catalogue contract below is new because the existing Home API supplies names and images but no authoritative price or stock. Align these product IDs with Sector 2 and Amir's Cart before integration. Do not populate production prices from the preview samples.

Each `products/{productId}` document requires these case-sensitive fields:

| Field | Firestore type | Meaning |
| --- | --- | --- |
| Name | string | Product display name |
| UnitPriceCents | integer | Confirmed selling price in ZAR cents, greater than zero |
| Available | boolean | Whether checkout is enabled for this product |
| StockQuantity | integer | Current quantity available |

Product IDs accept letters, numbers, hyphens and underscores, up to 100 characters. One request may contain up to 50 distinct products, each with quantity 1–99. A duplicate product must be combined into one line by Cart. The maximum unit price is R100,000,000 as a defensive technical bound, not an agreed commercial limit.

Set `Checkout__DeliveryFeeCents` in Azure (or `Checkout:DeliveryFeeCents` in local configuration) to the approved delivery fee. For example, `15000` means R150, but R150 is currently only the design assumption. Missing or invalid configuration blocks order creation; there is no silent free-delivery fallback. Confirm delivery coverage and fees with GoldTime.

## Create a pending order

`POST /api/orders`, with `Authorization: Bearer <Firebase ID token>` and JSON:

```json
{
  "requestId": "ef93cb52-2522-4900-82fd-466975a03cb7",
  "items": [{ "productId": "gold-bar-1oz", "quantity": 1 }],
  "delivery": {
    "fullName": "Test Customer",
    "phone": "+27 82 123 4567",
    "streetAddress": "10 Test Road",
    "city": "Johannesburg",
    "province": "Gauteng",
    "postalCode": "2000"
  },
  "paymentMethod": "CARD"
}
```

Allowed methods: `CARD` and `INSTANT_EFT`. Generate one request UUID for a checkout submission and retain it for network retries. A changed address, item quantity or payment method needs a new UUID. The transaction returns the same saved order for an identical retry. Reusing an ID for different details returns 409. Product line order does not change the request fingerprint.

The request has no trusted owner, price, total or paid-status fields. Those values are determined by the server. Successful creation or identical retry returns 200 with `id`, `items`, `delivery`, `paymentMethod`, `status`, `currency`, `subtotalCents`, `deliveryFeeCents`, `totalCents` and `createdAtUtc`. Each item includes product ID, name, quantity, unit price cents and line total cents. A newly created order is always `pending_payment`, in ZAR. Store this order ID for the later payment flow.

The Android integration must display and obtain confirmation of this server total before handing off to payment, especially if it differs from the earlier Cart total. Do not clear Cart when merely creating a pending order.

Errors: 400 invalid request; 401 missing/invalid authentication; 409 unavailable product, insufficient stock or changed retry details; 503 missing delivery configuration or unavailable storage. Error responses use ProblemDetails. API model binding can also return validation errors before the controller executes.

## Read an order

`GET /api/orders/{id}`, using the same authentication. Only its owner receives the saved order. Unknown IDs and another user's orders return 404. The endpoint cannot change payment status. Internal user IDs and request fingerprints are omitted from the response.

## Testing and limits

Run `dotnet test GoldTimeCo/GoldTimeApi.Tests/GoldTimeApi.Tests.csproj` from the repository root. The 27 tests cover calculation, delivery/payment validation, quantities, product availability/stock, stable user-scoped request IDs, changed-request fingerprints, controller ownership and safe error responses. Controller tests use a fake store: they do not prove JWT middleware, Firestore transaction retries or cloud connectivity.

Before deployment, use the Firestore emulator or an authorised test environment to verify actual creation/read, simultaneous duplicate submissions, changed-request rejection, price updates and insufficient stock. Test HTTP authentication with missing, expired and wrong-project tokens; test another user's order. Do not use real payment credentials for these checks.

Current stock is checked at pending-order creation but is **not reserved or deducted**. Pending orders are not a fulfilment promise. Before enabling payments, implement stock reservation/expiry or an agreed alternative, revalidate stale prices/orders, and verify PayFast notifications on the server. This increment has no payment endpoint, success claim, charge, cancellation/retry payment handling or order expiry. Live Firestore, payment, Android navigation and final accessibility checks remain outstanding.

## Rubric and references

Sector 4 requirement: persist checkout delivery details and reviewed items in preparation for payment. Rubric evidence: Back End Programming Skills, Database, APIs, Security, Data Flow & Logic; automated tests contribute to the testing requirement but do not establish a CI/CD pipeline or hosting marks.

Google. n.d. Transactions and batched writes. [Online]. Available at: https://firebase.google.com/docs/firestore/manage-data/transactions [Accessed 1 October 2026]. Used for atomic reads/writes and retry-safe transaction logic.

Google. n.d. Class Transaction. [Online]. Available at: https://cloud.google.com/dotnet/docs/reference/Google.Cloud.Firestore/latest/Google.Cloud.Firestore.Transaction [Accessed 1 October 2026]. Used for the .NET transaction methods; compilation checked against the project's existing Firestore 3.7.0 dependency.
