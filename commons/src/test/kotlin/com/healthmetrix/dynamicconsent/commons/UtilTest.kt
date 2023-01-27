package com.healthmetrix.dynamicconsent.commons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UtilTest {
    @Nested
    inner class StringJoinPathsTest {
        @Test
        fun `it joins two paths together with slash`() {
            assertThat("test".joinPaths("potato")).isEqualTo("test/potato")
        }

        @Test
        fun `it does not duplicate slashes from starting path`() {
            assertThat("test/".joinPaths("potato")).isEqualTo("test/potato")
        }

        @Test
        fun `it does not duplicate slashes from joined paths`() {
            assertThat("test".joinPaths("/potato")).isEqualTo("test/potato")
        }

        @Test
        fun `it joins multiple paths`() {
            assertThat("test".joinPaths("potato", "tomato")).isEqualTo("test/potato/tomato")
        }
    }

    @Nested
    inner class StringConsentTest {
        @Test
        fun `it drops the first word in a hyphenated consent`() {
            assertThat("test-consent-coconut".consent()).isEqualTo("consent-coconut")
        }
    }

    @Nested
    inner class StringConsentSourceTest {
        @Test
        fun `it grabs the first word from a hyphenated consent`() {
            assertThat("test-consent-coconut".consentSource()).isEqualTo("test")
        }
    }

    @Nested
    inner class StringAsLocalStaticResourcePathTest {
        @Test
        fun `it prepends folder name to path`() {
            assertThat("test".asLocalStaticResourcePath()).isEqualTo("/consent-assets/test")
        }
    }
}
