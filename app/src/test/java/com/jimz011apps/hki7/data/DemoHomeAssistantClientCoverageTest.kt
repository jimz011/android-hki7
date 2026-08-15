package com.jimz011apps.hki7.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * [DemoHomeAssistantClient] subclasses the real [HomeAssistantClient], so any network method it
 * forgets to override is silently inherited and fires at [DEMO_SERVER_URL] — an unresolvable
 * `.invalid` host. That failure is quiet: the call just errors, gets logged, and (for the
 * subscription loops) retries forever, filling the connection log for the whole demo session.
 *
 * Every `open` member of the base class is a network call, so the rule is simply that the demo must
 * override all of them. This test is the enforcement — adding a new companion-component endpoint or
 * any other `open` client method fails here until the demo answers it offline.
 */
class DemoHomeAssistantClientCoverageTest {

    @Test
    fun `demo client overrides every network method of the real client`() {
        val demoMethods = DemoHomeAssistantClient::class.java.declaredMethods
            .map { it.signature() }
            .toSet()

        val missing = HomeAssistantClient::class.java.declaredMethods
            .filter { it.isOverridableNetworkCall() }
            .map { it.signature() }
            .distinct()
            .filterNot { it in demoMethods }
            .sorted()

        assertTrue(
            "DemoHomeAssistantClient does not override ${missing.size} network method(s), so they " +
                "run against $DEMO_SERVER_URL in demo mode: ${missing.joinToString(", ")}",
            missing.isEmpty()
        )
    }

    /** Kotlin's `open` compiles to a non-final JVM method. Synthetic/bridge methods (default-argument
     *  `$default` helpers, access thunks) are compiler output, not API, so they are skipped. */
    private fun Method.isOverridableNetworkCall(): Boolean =
        !Modifier.isFinal(modifiers) &&
            !Modifier.isStatic(modifiers) &&
            !Modifier.isPrivate(modifiers) &&
            !isSynthetic &&
            !isBridge &&
            name !in INHERITED_FROM_ANY

    private fun Method.signature(): String =
        "$name(${parameterTypes.joinToString(",") { it.name }})"

    private companion object {
        /** Object members are not client API and are not expected to be overridden. */
        val INHERITED_FROM_ANY = setOf("equals", "hashCode", "toString")
    }
}
