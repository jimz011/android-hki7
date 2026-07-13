package com.example.hki7.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.example.hki7.ui.ActionOutcome
import com.example.hki7.ui.navRouteForTarget

/** Resolves a view-model [ActionOutcome] into UI effects: opening more-info (delegated to
 *  [openMoreInfo]), navigating within the app, or opening an external URL. Side-effecting outcomes
 *  ([ActionOutcome.Handled]/[ActionOutcome.None]) are no-ops here. [navController] may be null for
 *  surfaces without navigation (navigate outcomes are then ignored). */
fun handleActionOutcome(
    outcome: ActionOutcome,
    context: Context,
    navController: NavController?,
    openMoreInfo: (String) -> Unit
) {
    when (outcome) {
        is ActionOutcome.OpenMoreInfo -> openMoreInfo(outcome.entityId)
        is ActionOutcome.Navigate -> navController?.let { nav ->
            navRouteForTarget(outcome.target)?.let { route -> runCatching { nav.navigate(route) } }
        }
        is ActionOutcome.OpenUrl -> runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(outcome.url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        ActionOutcome.Handled, ActionOutcome.None -> {}
    }
}
