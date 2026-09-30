# Sector 4 implementation record

## Delivery details

Requirement: WIL Task 2 sector plan, Sector 4 delivery-details form. Fields follow the supplied design: full name, phone, street address, city, province and postal code.

Rubric: UX and Feedback (inline errors), Responsiveness and Accessibility (scrolling, keyboard space, labelled fields, bounded tablet width), Programming Skills (separate validation), Security (client input validation). Server validation is still required; this is not evidence of completed backend security.

South African delivery is the initial assumption from the supplied design. Province and four-digit postal-code checks are format checks, not address verification. Phone validation permits international prefixes. Confirm delivery coverage with the client before release.

The screen preserves small form values with rememberSaveable. It is not yet connected to Cart or order review. The preview runs without Firebase or Azure and contains no authentication bypass or payment simulation.

Automated checks: DeliveryDetailsTest covers empty input, phone formatting, invalid characters, provinces, postal codes including leading zeroes, trimming and address limits.

Verification on 30 September 2026: all six JUnit tests passed when the actual validation and test sources were compiled with Kotlin 2.0.21 and run with JUnit 4.13.2. The user's Android Studio screenshot subsequently confirmed BUILD SUCCESSFUL and a rendered Delivery Details preview. The assistant's separate Gradle attempt was blocked by Android SDK filesystem access; the successful compilation evidence comes from Android Studio.

Manual checks passed: the user confirmed empty fields show errors and completed details display the preview validation confirmation, supported by a screenshot. This confirms form validation, not actual address verification, order creation or payment. Still pending: device rotation, keyboard/large-font layouts and full checkout integration.

## References

Android Developers, Save UI state in Compose. Accessed 30 September 2026.
https://developer.android.com/develop/ui/compose/state-saving

Android Developers, Preview your UI with composable previews. Accessed 30 September 2026.
https://developer.android.com/develop/ui/compose/tooling/previews

These references informed state preservation and isolated preview testing. The application code was written for this project.
