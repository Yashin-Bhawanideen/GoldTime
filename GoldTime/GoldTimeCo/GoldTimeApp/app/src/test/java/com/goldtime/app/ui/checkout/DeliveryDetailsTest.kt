package com.goldtime.app.ui.checkout

import org.junit.Assert.*
import org.junit.Test

class DeliveryDetailsTest {
    private val valid = DeliveryDetails(
        "Test Customer", "+27 82 000 0000", "12 Example Street",
        "Pretoria", "Gauteng", "0002"
    )

    @Test fun acceptsFormattedPhoneAndPreservesLeadingPostalZeroes() {
        assertTrue(validateDelivery(valid).isEmpty())
        assertEquals("0002", valid.trimmed().postalCode)
    }

    @Test fun rejectsEmptyAndWhitespaceFields() {
        assertEquals(6, validateDelivery(DeliveryDetails()).size)
        assertTrue(validateDelivery(valid.copy(fullName = "   ")).containsKey("fullName"))
    }

    @Test fun rejectsPhoneLettersAndMisplacedPlus() {
        for (phone in listOf("call0820000000", "082+0000000", "123", "+1234567890123456")) {
            assertTrue(phone, validateDelivery(valid.copy(phone = phone)).containsKey("phone"))
        }
    }

    @Test fun acceptsAllProvincesIgnoringCaseAndWhitespace() {
        for (province in southAfricanProvinces) {
            assertTrue(validateDelivery(valid.copy(province = " ${province.lowercase()} ")).isEmpty())
        }
    }

    @Test fun rejectsUnknownProvinceAndMalformedPostalCode() {
        assertTrue(validateDelivery(valid.copy(province = "Unknown")).containsKey("province"))
        for (code in listOf("123", "12345", "AB12")) {
            assertTrue(validateDelivery(valid.copy(postalCode = code)).containsKey("postalCode"))
        }
    }

    @Test fun trimsInputAndRejectsOversizedAddress() {
        assertEquals("Test Customer", valid.copy(fullName = " Test Customer ").trimmed().fullName)
        assertTrue(validateDelivery(valid.copy(streetAddress = "a".repeat(201))).containsKey("streetAddress"))
    }
}
