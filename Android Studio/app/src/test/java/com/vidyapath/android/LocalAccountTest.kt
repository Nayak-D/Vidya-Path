package com.vidyapath.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAccountTest {
    @Test
    fun passwordHashesAreSaltedAndOnlyMatchTheOriginalPassword() {
        val first = PasswordHasher.create("correct horse battery")
        val second = PasswordHasher.create("correct horse battery")

        assertFalse(first.salt.contentEquals(second.salt))
        assertTrue(PasswordHasher.matches("correct horse battery", first.salt, first.hash))
        assertFalse(PasswordHasher.matches("incorrect password", first.salt, first.hash))
    }

    @Test
    fun accountEmailIsTrimmedAndNormalized() {
        assertEquals("student@example.com", normalizeAccountEmail("  Student@Example.com "))
    }
}
