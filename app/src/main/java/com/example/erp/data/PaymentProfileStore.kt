package com.example.erp.data

import android.content.Context

/**
 * Local storage for the user's payment profiles.
 *
 * One JSON string in one SharedPreferences file: profiles are a short list that
 * is read once when the screen opens and written when it changes, so a schema
 * per field would add versioning problems without buying anything. Values are
 * never logged and never leave the device outside an explicit share.
 *
 * No Flow: the profiles screen lifts the list into Compose state and the screen
 * is the only writer, so a second observer would add a subscription without a
 * consumer.
 */
class PaymentProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<PaymentProfile> =
        PaymentProfileCodec.decode(prefs.getString(KEY_PROFILES, null))

    fun save(profiles: List<PaymentProfile>) {
        prefs.edit()
            .putString(KEY_PROFILES, PaymentProfileCodec.encode(profiles))
            .apply()
    }

    private companion object {
        const val PREFS = "payment_profiles"
        const val KEY_PROFILES = "profiles_json"
    }
}
