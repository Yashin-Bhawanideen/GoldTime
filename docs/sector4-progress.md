# Sector 4 implementation record

## Delivery details

Requirement: WIL Task 2 sector plan, Sector 4 delivery-details form. Fields follow the supplied design: full name, phone, street address, city, province and postal code.

Rubric: UX and Feedback (inline errors), Responsiveness and Accessibility (scrolling, keyboard space, labelled fields, bounded tablet width), Programming Skills (separate validation), Security (client input validation). Server validation is still required; this is not evidence of completed backend security.

South African delivery is the initial assumption from the supplied design. Province and four-digit postal-code checks are format checks, not address verification. Phone validation permits international prefixes. Confirm delivery coverage with the client before release.

The screen preserves small form values with rememberSaveable. It is not yet connected to Cart or order review. The preview runs without Firebase or Azure and contains no authentication bypass or payment simulation.

Automated checks: DeliveryDetailsTest covers empty input, phone formatting, invalid characters, provinces, postal codes including leading zeroes, trimming and address limits.

Verification on 30 September 2026: all six JUnit tests passed when the actual validation and test sources were compiled with Kotlin 2.0.21 and run with JUnit 4.13.2. The user's Android Studio screenshot subsequently confirmed BUILD SUCCESSFUL and a rendered Delivery Details preview. The assistant's separate Gradle attempt was blocked by Android SDK filesystem access; the successful compilation evidence comes from Android Studio.

Manual checks passed: the user confirmed empty fields show errors and completed details display the preview validation confirmation, supported by a screenshot. This confirms form validation, not actual address verification, order creation or payment. Still pending: device rotation, keyboard/large-font layouts and full checkout integration.

## Order review

Requirement: Sector 4 Review Order and Task 1's multiple-item purchase workflow. Added item names, quantities, unit prices, line totals, delivery address, edit-delivery action, subtotal, delivery and total. Payment continuation is blocked for an empty cart or invalid delivery details.

Rubric: UX and Feedback (review/edit journey and empty state), Programming Skills and Data Flow (separate order model and exact decimal calculations). BigDecimal avoids binary floating-point arithmetic for money (Oracle, n.d.). These are display calculations; the API must later resolve authoritative prices and verify stock before payment.

CheckoutFlowPreview connects delivery and review using clearly labelled sample data only. Returning to edit restores the submitted delivery details. The sample R150 delivery fee follows the mock-up and still needs business confirmation. No real Cart connection, order persistence or payment processing is claimed.

All 13 JUnit tests passed (six delivery and seven order-review tests) using the Kotlin compiler and JUnit directly. Automated cases cover multiple products/quantities, a single delivery charge, fractional currency arithmetic, empty carts and invalid quantities/prices/fees. Android Studio compilation and interactive preview checks are pending for this change. Check the sample total of R139590.00, edit the address and confirm the change on review, then check that the empty-cart preview disables payment. Device back-button handling and full navigation will be verified when the screen is connected to the application's navigation graph.

Reference: Oracle. n.d. BigDecimal (Java SE 17). [Online]. Available at: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html [Accessed 30 September 2026].

Preview verification on 1 October 2026: after correcting text contrast and card colours, the user reported the flow working and supplied the "Preview: ready for payment" confirmation. This verifies that the interactive sample flow reaches the payment callback; no actual payment or order was created. The earlier empty-cart preview had a no-op edit callback; it now opens the delivery form and returns to review. The screenshot does not independently establish the exact displayed total, edited-city persistence, large-font layout or empty-cart disabled state; retain these for final regression testing. Automated calculation tests remain the evidence for totals. The UI has run in Android Studio's preview; this is not a claim of complete app/navigation integration.

## Payment method selection

Requirement: WIL Task 2 Sector 4 payment-method screen and Task 1 card/Instant EFT purchase options. Added Step 3 with the current order total, explicit single-method selection, validation feedback and return to Review Order. The preview now connects all three screens and retains the selected method and submitted address while moving backwards/forwards. PaymentMethodScreen passes valid selections to a callback; it does not send a network request.

Rubric: UX and Feedback (selection errors and back navigation), Responsiveness and Accessibility (scrolling and labelled radio-group semantics), Programming Skills (separate checkout validation). Comments and references follow the user's Kerberos style.

Card and Instant EFT are UI options from the requirements, not a claim that a merchant account has enabled either option. Preview actions are labelled "Check selection" and explicitly report that no payment/order is submitted. PayFast configuration, pending order storage, authoritative prices, notifications and verified payment confirmation remain outstanding.

Tests: all 18 checkout logic tests passed with Kotlin 2.0.21 and JUnit 4.13.2 on 1 October 2026. PaymentMethodTest adds five tests for missing selection, both supported selections, empty Cart, invalid delivery and zero total. Android Studio build/interactive testing is pending. Manually check no-selection error, switching methods, total consistency, back to review/delivery and return with state retained. The successful preview dialog must say "method selected", never "payment successful".

Reference: Android Developers. 2026. Radio button. [Online]. Available at: https://developer.android.com/develop/ui/compose/components/radio-button [Accessed 1 October 2026].

Manual verification on 1 October 2026: after receiving the build and interactive checklist, the user confirmed completion. The reported checks cover selection-required feedback, switching Card/Instant EFT, retaining selection after returning from Review Order, and the preview confirmation. This is user-reported preview verification, not a live PayFast or persisted-order test. All 18 checkout logic tests also passed independently. Device accessibility and full application navigation remain for later integration checks.

## Pending-order API — 1 October 2026

Added authenticated POST /api/orders and owner-only GET /api/orders/{id}. Server-side validation checks delivery, allowed payment selections, distinct product IDs and quantities. The server reads product prices and stock from Firestore and computes whole-cent totals; the request cannot choose its owner or paid status. Pending orders include item/address snapshots, currency and creation time. A Firestore transaction uses a user-scoped request ID and request fingerprint to prevent duplicate creation on identical retries and reject changed details under the same ID.

No priced catalogue existed in the API. The new required products schema and configured delivery fee are documented in checkout-api.md for coordination with Sector 2/3 and Yashin. Unknown/unavailable/unpriced products fail closed. Stock is checked but not reserved; payment remains disabled/unimplemented until reservation/expiry and verified PayFast handling are added. The Android preview still makes no order request.

Rubric: Back End Programming Skills, Database, APIs, Security, Data Flow & Logic. Added 27 automated tests covering calculations, malformed inputs, unavailable/insufficient-stock products, identity derivation, changed fingerprints, controller ownership and safe error responses. These tests are not a CI/CD deployment or a live database test.

Verification: the actual net8.0 API and test project compiled with the original Firestore 3.7.0/JWT 8.0.8 dependencies, with no compiler warnings after correction. All 27 xUnit tests passed. Standard NuGet downloading failed due this machine's TLS configuration; public packages were downloaded with Python's verified HTTPS client into a task-local package feed, then restored locally. No credentials or cloud writes were used. Firestore transaction/concurrency behaviour, HTTP JWT middleware and Azure deployment remain integration checks. Existing Android tests were not repeated because no Android source changed.

Comments and references were included in the transaction implementation and setup document. The README now accurately describes this increment and corrects the Android configuration filename and service-account handling instructions.

## Delivery references

Android Developers, Save UI state in Compose. Accessed 30 September 2026.
https://developer.android.com/develop/ui/compose/state-saving

Android Developers, Preview your UI with composable previews. Accessed 30 September 2026.
https://developer.android.com/develop/ui/compose/tooling/previews

These references informed state preservation and isolated preview testing. The application code was written for this project.

## Comment and commit style

Match the user's previous Kerberos work: short plain-language comments explaining logic, author/year citations where a source informed the implementation, and full reference entries in the relevant code file. Use n.d. when no publication year has been verified. Cite sources actually consulted, not unrelated references added to increase the count.

Use short plain-language commit messages, for example "add delivery details validation" or "add order review screen". Commit meaningful tested changes separately and retain the existing history. Documentation-only edits do not require repeating application tests.
