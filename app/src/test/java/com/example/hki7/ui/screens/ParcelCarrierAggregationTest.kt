package com.example.hki7.ui.screens

import com.example.hki7.data.HAEntity
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ParcelCarrierAggregationTest {
    @Test
    fun `accounts from the same known carrier merge into one carrier`() {
        val firstEntity = parcel("sensor.postnl_account_one_incoming", "ONE")
        val secondEntity = parcel("sensor.postnl_account_two_outgoing", "TWO")
        val carriers = listOf(
            carrier("postnl", "device-one", firstEntity, incoming = 1),
            carrier("postnl", "device-two", secondEntity, outgoing = 1)
        )

        val merged = aggregateParcelCarriers(carriers).single()

        assertEquals("PostNL", merged.name)
        assertEquals(1, merged.incoming)
        assertEquals(1, merged.outgoing)
        assertEquals(setOf("ONE", "TWO"), merged.parcels.mapNotNull { it["barcode"]?.toString()?.trim('"') }.toSet())
    }

    @Test
    fun `unknown carriers never merge merely because they share the fallback key`() {
        val carriers = listOf(
            carrier("parcel", "device-one", parcel("sensor.first_parcel", "ONE")),
            carrier("parcel", "device-two", parcel("sensor.second_parcel", "TWO"))
        )

        val result = aggregateParcelCarriers(carriers)

        assertEquals(2, result.size)
        assertNotEquals(result[0].deviceId, result[1].deviceId)
    }

    private fun carrier(
        key: String,
        deviceId: String,
        entity: HAEntity,
        incoming: Int = 0,
        outgoing: Int = 0
    ) = ParcelCarrier(
        key = key,
        name = deviceId,
        deviceId = deviceId,
        entities = listOf(entity),
        incoming = incoming,
        outgoing = outgoing,
        logoUrl = null,
        baseUrl = "https://example.test",
        accessToken = ""
    )

    private fun parcel(entityId: String, barcode: String) = HAEntity(
        entity_id = entityId,
        state = "on",
        attributes = buildJsonObject {
            put("barcode", barcode)
            put("friendly_name", entityId)
        }
    )
}
