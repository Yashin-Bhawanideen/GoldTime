# Sector 4 progress

Updated 1 October 2026.

## Completed work

| Requirement | Implementation | Rubric areas |
| --- | --- | --- |
| Delivery details | Full name, phone, street, city, province and postal code, with validation and saved form state | UX and Feedback; Responsiveness and Accessibility; Programming Skills |
| Review order | Items, quantities, prices, delivery address, edit action and totals | UX and Feedback; Programming Skills; Data Flow and Logic |
| Payment method | Card and Instant EFT selection, validation and back navigation | UX and Feedback; Responsiveness and Accessibility |
| Pending orders | Authenticated API, server pricing, delivery validation, order storage and owner-only retrieval | Database; APIs; Security; Programming Skills; Data Flow and Logic |

The three Android screens are connected in CheckoutFlowPreview using sample items. Connection to the application's Cart and navigation is still outstanding. Preview confirmations do not create orders or process payments.

Delivery currently assumes South African addresses. Province and postal-code validation checks format only. Delivery coverage and the sample R150 fee need confirmation from GoldTime. Card and Instant EFT also need to be enabled and tested with the payment provider.

The API reads prices and stock from Firestore and saves orders with pending_payment status. It uses a request ID to handle repeated submissions. The required product fields, delivery configuration and endpoint details are in [checkout-api.md](checkout-api.md). Stock is checked but not reserved yet.

## Testing

| Check | Result |
| --- | --- |
| Delivery validation | 6 JUnit tests passed |
| Order calculations | 7 JUnit tests passed |
| Payment selection validation | 5 JUnit tests passed |
| Android compilation | Successful build in Android Studio |
| Interactive previews | Empty-field errors, valid delivery, review continuation and payment selection manually checked |
| API compilation | Successful .NET 8 build, no compiler warnings |
| Backend validation and controllers | 27 xUnit tests passed |

The 18 Android logic tests were run with Kotlin 2.0.21 and JUnit 4.13.2. The backend tests use a fake order store for controller checks. They cover totals, invalid input, unavailable stock, request identity, ownership and error responses; they do not test live Firestore or token validation over HTTP.

## Remaining checks and integration

- Connect the real Cart, checkout screens and order API.
- Confirm catalogue IDs, prices, stock and delivery fees.
- Verify Firestore saves, concurrent retries and order retrieval in a test environment.
- Test missing, expired and wrong-project authentication tokens over HTTP.
- Add stock reservation or an agreed alternative before enabling payment.
- Integrate PayFast and verify notifications before showing payment success.
- Test cancellation, failure and retry without losing Cart contents.
- Check device rotation, keyboard layouts, large text, accessibility and navigation on a device.
- Recheck edited-address persistence, displayed totals and empty-cart behaviour during the full flow.
- Deploy and verify the hosted API, then add CI/CD evidence and presentation material.

## References

Android Developers. n.d. Save UI state in Compose. [Online]. Available at: https://developer.android.com/develop/ui/compose/state-saving [Accessed 30 September 2026].

Android Developers. n.d. Preview your UI with composable previews. [Online]. Available at: https://developer.android.com/develop/ui/compose/tooling/previews [Accessed 30 September 2026].

Oracle. n.d. BigDecimal (Java SE 17). [Online]. Available at: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html [Accessed 30 September 2026].

Android Developers. 2026. Radio button. [Online]. Available at: https://developer.android.com/develop/ui/compose/components/radio-button [Accessed 1 October 2026].

Google. n.d. Transactions and batched writes. [Online]. Available at: https://firebase.google.com/docs/firestore/manage-data/transactions [Accessed 1 October 2026].

Google. n.d. Class Transaction. [Online]. Available at: https://cloud.google.com/dotnet/docs/reference/Google.Cloud.Firestore/latest/Google.Cloud.Firestore.Transaction [Accessed 1 October 2026].
