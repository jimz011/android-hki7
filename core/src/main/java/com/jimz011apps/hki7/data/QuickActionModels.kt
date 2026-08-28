package com.jimz011apps.hki7.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One entry on a surface that is not the dashboard.
 *
 * Android Auto can only draw templates and a watch screen has room for a handful of items, so
 * neither can render an HKI 7 dashboard — there is nothing to derive a car or watch screen from.
 * Instead the user curates one short, explicit list once and every off-dashboard surface reads it.
 *
 * Reuses [HKIAction] rather than inventing a second action model, so the service picker, target
 * handling and payload building in the dashboard editors all apply unchanged.
 *
 * This file lives in `:core` precisely because the phone and the watch both run these actions.
 * The semantics below — which domains have a one-tap meaning, what `default` expands to — must
 * give the same answer on both, or the same shortcut would behave differently depending on which
 * device the user reached for.
 */
@Serializable
data class HKIQuickAction(
    val id: String = UUID.randomUUID().toString(),
    /** The entity this acts on, and the fallback target for [action]. */
    val entityId: String = "",
    /** Overrides the entity's friendly name. Off-dashboard surfaces have very little room. */
    val name: String? = null,
    /** Pack-qualified icon slug, as everywhere else in the app. Null falls back to a domain icon. */
    val icon: String? = null,
    val action: HKIAction = HKIAction(type = "default"),
    /** Whether the Android Auto surface offers this entry. */
    val showInCar: Boolean = true,
    /** Whether the Wear OS surface offers this entry. */
    val showOnWatch: Boolean = true,
)

/** What a quick action actually does once resolved. */
enum class QuickActionKind {
    TOGGLE,
    CALL_SERVICE,

    /**
     * The action means nothing away from the dashboard — "open more info", "navigate",
     * "open a popup" and friends all need the app's own UI, and a `default` on a domain with no
     * sensible one-tap meaning (a lock, a vacuum, an alarm panel) lands here too.
     *
     * Surfaced in the editor as a warning rather than silently doing nothing on the other surface.
     */
    UNSUPPORTED,
}

/**
 * Domains where a plain tap has an unambiguous meaning and `<domain>.toggle` exists.
 *
 * Deliberately narrower than the dashboard's own default-action heuristic: on the dashboard a
 * `default` tap on a lock or an alarm panel opens a dialog, which no off-dashboard surface can do.
 * Rather than guess a direction for those, they resolve to [QuickActionKind.UNSUPPORTED] and the
 * user picks an explicit action — the safe choice for a surface used while driving.
 */
private val QUICK_TOGGLE_DOMAINS = setOf(
    "light", "switch", "fan", "input_boolean", "media_player",
    "humidifier", "siren", "climate", "remote", "cover", "valve", "automation",
)

/** Domains whose `default` means "run this", not "toggle this": service name by domain. */
private val QUICK_RUN_SERVICES = mapOf(
    "scene" to "turn_on",
    // script.toggle would *stop* a running script; from a car or a watch the intent is "run it".
    "script" to "turn_on",
    "button" to "press",
    "input_button" to "press",
)

/** The action this resolves to, with `default` expanded against the entity's domain. */
fun HKIQuickAction.resolvedAction(): HKIAction {
    if (action.type != "default") return action
    val domain = entityId.substringBefore('.', "")
    if (domain.isEmpty()) return HKIAction(type = "none")
    QUICK_RUN_SERVICES[domain]?.let { service ->
        return HKIAction(type = "call_service", service = "$domain.$service")
    }
    return if (domain in QUICK_TOGGLE_DOMAINS) HKIAction(type = "toggle") else HKIAction(type = "none")
}

/** How [resolvedAction] will be carried out, or [QuickActionKind.UNSUPPORTED]. */
fun HKIQuickAction.resolvedKind(): QuickActionKind {
    val resolved = resolvedAction()
    return when (resolved.type) {
        "toggle" -> if (targetEntityId() != null) QuickActionKind.TOGGLE else QuickActionKind.UNSUPPORTED
        "call_service" ->
            if (resolved.service?.contains('.') == true) QuickActionKind.CALL_SERVICE
            else QuickActionKind.UNSUPPORTED
        else -> QuickActionKind.UNSUPPORTED
    }
}

/** The entity a toggle acts on: the action's explicit target, else the entry's own entity. */
fun HKIQuickAction.targetEntityId(): String? =
    (resolvedAction().targetEntityId ?: entityId).takeIf { it.isNotBlank() }
