package com.jimz011apps.hki7.data

/**
 * The wire contract between the phone app and the watch app.
 *
 * Lives in `:core` so neither side can drift: a renamed key here is a compile error on both, where
 * a typo in a string literal on one side would instead be a silent "the watch never gets its
 * favourites" bug that only shows up on real hardware.
 */
object WearHandover {

    /** DataItem path the phone writes the watch's whole configuration to. */
    const val CONFIG_PATH = "/hki7/config"

    /** Message path the watch sends when it wants the phone to re-send [CONFIG_PATH]. */
    const val REQUEST_CONFIG_PATH = "/hki7/request-config"

    const val KEY_SERVER_URL = "server_url"
    /**
     * The *refresh* token, not an access token.
     *
     * An access token expires in about half an hour and the watch would have no way to renew one,
     * so it would work briefly and then quietly stop. With a refresh token the watch keeps its own
     * session alive indefinitely, exactly as it does when it signs itself in through the phone's
     * browser instead.
     */
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_QUICK_ACTIONS = "quick_actions"
    const val KEY_ROOMS = "rooms"

    /**
      * Entity ids for the watch thermostat tile, chosen on the phone, comma separated.
      *
      * A list rather than one id because the tile cycles through them on a tap: a watch cannot
      * swipe within a tile (ProtoLayout has no gesture beyond Clickable), and adding one tile per
      * thermostat would need per-instance configuration on a screen too small to do it on.
      *
      * Empty means no thermostat tile content.
      */
    const val KEY_THERMOSTAT_ENTITY_IDS = "thermostat_entity_ids"

    /**
     * Bumped whenever the phone's state changes in a way the watch should notice.
     *
     * The Data Layer deduplicates identical DataItems and will not deliver a write whose bytes
     * match what is already there. Without a changing field, re-sending an unchanged config after
     * the watch asked for it would be silently dropped, and a watch that had missed the original
     * would stay empty forever.
     */
    const val KEY_UPDATED_AT = "updated_at"
}
