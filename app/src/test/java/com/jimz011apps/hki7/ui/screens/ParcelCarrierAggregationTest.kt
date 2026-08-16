package com.jimz011apps.hki7.ui.screens

import com.jimz011apps.hki7.data.HAEntity
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `postnl next delivery helper cannot shadow the complete incoming parcel`() {
        val nextDelivery = HAEntity(
            entity_id = "sensor.postnl_next_delivery",
            state = "2026-08-01T08:00:00+02:00",
            attributes = buildJsonObject {
                put("barcode", "3STEST")
                put("sender", "Example shop")
                put("planned_from", "2026-08-01T08:00:00+02:00")
                put("planned_to", "2026-08-01T10:00:00+02:00")
                put("url", "https://jouw.postnl.nl/track-and-trace/3STEST")
                put("friendly_name", "PostNL Next delivery")
            }
        )
        val incoming = HAEntity(
            entity_id = "sensor.postnl_incoming_parcels",
            state = "1",
            attributes = buildJsonObject {
                put("friendly_name", "PostNL Incoming parcels")
                putJsonArray("parcels") {
                    add(buildJsonObject {
                        put("barcode", "3STEST")
                        put("sender", "Example shop")
                        put("status", "in_transit")
                        put("planned_from", "2026-08-01T08:00:00+02:00")
                        putJsonArray("history") {
                            add(buildJsonObject { put("status", "registered") })
                        }
                    })
                }
            }
        )

        listOf(
            emptyMap(),
            mapOf(
                nextDelivery.entity_id to "next_delivery",
                incoming.entity_id to "incoming_parcels"
            )
        ).forEach { translationKeys ->
            val result = collectCarrierParcels(
                listOf(nextDelivery, incoming),
                translationKeys
            ).single()

            assertEquals("in_transit", result["status"]?.jsonPrimitive?.contentOrNull)
            assertEquals("2026-08-01T08:00:00+02:00", result["planned_from"]?.jsonPrimitive?.contentOrNull)
            assertTrue(result.containsKey("history"))
        }
    }

    @Test
    fun `carrier card selects earliest expected delivery from active parcels`() {
        val later = parcel("sensor.postnl_later", "LATER", "2026-08-04T14:00:00+02:00")
        val earlier = parcel("sensor.postnl_earlier", "EARLIER", "2026-08-03T09:30:00+02:00")
        val carrier = ParcelCarrier(
            key = "postnl",
            name = "PostNL",
            deviceId = "postnl-device",
            entities = listOf(later, earlier),
            incoming = 2,
            outgoing = 0,
            logoUrl = null,
            baseUrl = "https://example.test",
            accessToken = ""
        )

        assertEquals("2026-08-03T09:30:00+02:00", carrier.firstExpectedDeliveryValue())
    }

    @Test
    fun `carrier card can use next delivery helper without duplicating the parcel`() {
        val helper = HAEntity(
            entity_id = "sensor.postnl_next_delivery",
            state = "2026-08-03T11:00:00+02:00",
            attributes = buildJsonObject { put("friendly_name", "PostNL Next delivery") }
        )
        val carrier = carrier("postnl", "postnl-device", helper, incoming = 1)

        assertTrue(carrier.parcels.isEmpty())
        assertEquals("2026-08-03T11:00:00+02:00", carrier.firstExpectedDeliveryValue())
    }

    @Test
    fun `duplicate parcel sources merge into the most complete object`() {
        val individual = HAEntity(
            entity_id = "sensor.postnl_parcel_3stest",
            state = "in_transit",
            attributes = buildJsonObject {
                put("barcode", "3STEST")
                put("sender", "Example shop")
            }
        )
        val summary = HAEntity(
            entity_id = "sensor.postnl_incoming_parcels",
            state = "1",
            attributes = buildJsonObject {
                putJsonArray("parcels") {
                    add(buildJsonObject {
                        put("barcode", "3STEST")
                        put("status", "in_transit")
                        put("receiver", "Home")
                    })
                }
            }
        )

        val result = collectCarrierParcels(listOf(individual, summary)).single()

        assertEquals("Example shop", result["sender"]?.jsonPrimitive?.contentOrNull)
        assertEquals("Home", result["receiver"]?.jsonPrimitive?.contentOrNull)
        assertEquals("in_transit", result["status"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `canonical delivered flag marks a parcel as delivered`() {
        val delivered = HAEntity(
            entity_id = "sensor.postnl_delivered_parcels",
            state = "1",
            attributes = buildJsonObject {
                putJsonArray("parcels") {
                    add(buildJsonObject {
                        put("barcode", "3SDELIVERED")
                        put("status", "handed_over")
                        put("delivered", true)
                    })
                }
            }
        )
        val carrier = carrier("postnl", "device-one", delivered)

        assertEquals(1, carrier.deliveredCount)
        assertFalse(carrier.parcels.single()["status"]?.jsonPrimitive?.contentOrNull == "delivered")
    }

    @Test
    fun `translation key keeps a renamed vinted parcel outgoing`() {
        val outgoingSummary = HAEntity(
            entity_id = "sensor.my_custom_name",
            state = "1",
            attributes = buildJsonObject {
                putJsonArray("parcels") {
                    add(buildJsonObject {
                        put("barcode", "VINTED-ONE")
                        put("status", "in_transit")
                    })
                }
            }
        )
        val individual = HAEntity(
            entity_id = "sensor.another_custom_name",
            state = "in_transit",
            attributes = buildJsonObject {
                put("barcode", "VINTED-ONE")
                put("status", "in_transit")
                putJsonObject("raw") { put("contact_type", "sender") }
            }
        )
        val carrier = ParcelCarrier(
            key = "vinted_go",
            name = "Vinted Go",
            deviceId = "vinted-device",
            entities = listOf(outgoingSummary, individual),
            incoming = 0,
            outgoing = 1,
            logoUrl = null,
            baseUrl = "https://example.test",
            accessToken = "",
            domain = "vinted_go",
            translationKeys = mapOf(
                outgoingSummary.entity_id to "outgoing_parcels",
                individual.entity_id to "parcel"
            )
        )

        assertEquals(0, carrier.incomingCount)
        assertEquals(1, carrier.outgoingCount)
    }

    @Test
    fun `legacy postnl grouped arrays normalize into canonical parcels`() {
        val legacy = HAEntity(
            entity_id = "sensor.postnl_delivery",
            state = "2",
            attributes = buildJsonObject {
                putJsonArray("enroute") {
                    add(buildJsonObject {
                        put("key", "LEGACY-INCOMING")
                        put("name", "Example shop")
                        put("status_message", "Onderweg")
                        put("planned_from", "2026-08-02T09:00:00+02:00")
                    })
                }
                putJsonArray("delivered") {
                    add(buildJsonObject {
                        put("key", "LEGACY-DELIVERED")
                        put("name", "Other shop")
                        put("status_message", "Afgeleverd")
                    })
                }
            }
        )
        val carrier = carrier("postnl", "legacy-device", legacy)

        assertEquals(2, carrier.parcels.size)
        assertEquals("Example shop", carrier.parcels.first()["sender"]?.jsonPrimitive?.contentOrNull)
        assertEquals(1, carrier.incomingCount)
        assertEquals(1, carrier.deliveredCount)
    }

    @Test
    fun `postnl shared parcel prefers raw shop name over household contact`() {
        val incoming = HAEntity(
            entity_id = "sensor.postnl_incoming_parcels",
            state = "1",
            attributes = buildJsonObject {
                putJsonArray("parcels") {
                    add(buildJsonObject {
                        put("barcode", "SHARED")
                        put("sender", "Housemate")
                        put("status", "in_transit")
                        putJsonObject("raw") {
                            put("name", "Actual shop")
                            put("source_display_name", "Housemate")
                        }
                    })
                }
            }
        )

        val result = collectCarrierParcels(listOf(incoming), domain = "postnl").single()

        assertEquals("Actual shop", result["sender"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `manual tracking services match current integration contracts`() {
        assertEquals(
            setOf("gls", "dragonfly", "cainiao", "correos", "packeta", "hermes", "trunkrs"),
            TRACK_PARCEL_DOMAINS
        )
        assertEquals(setOf("gls", "trunkrs"), TRACK_PARCEL_POSTCODE_DOMAINS)
        assertFalse("vinted_go" in TRACK_PARCEL_DOMAINS)
    }

    @Test
    fun `every carrier the integration organisation publishes is recognised`() {
        // Domains follow the repository names in github.com/ha-parcel-integrations, minus the
        // `ha-` prefix and with dashes as underscores.
        val published = listOf(
            "ampere", "an_post", "budbee", "cainiao", "correos", "delhivery", "dhl_nl", "dpd",
            "dragonfly", "dynalogic", "gls", "helthjem", "hermes", "inpost", "nova_post",
            "oesterreichische_post", "packeta", "planzer", "postnl", "postnord", "quickpac",
            "sameday", "sunyou", "swiss_post", "trunkrs", "vinted_go", "parcel_aggregator",
        )
        val missing = published.filterNot { it in PARCEL_CARRIERS }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `carrier names resolve to the right domain`() {
        // A carrier must never be mistaken for a shorter name inside it: several of these contain
        // "post", and Nova Post / An Post / PostNord / Swiss Post all have to stay distinct.
        val expected = mapOf(
            "PostNL" to "postnl",
            "PostNord" to "postnord",
            "An Post" to "an_post",
            "Nova Poshta" to "nova_post",
            "Swiss Post" to "swiss_post",
            "Österreichische Post" to "oesterreichische_post",
            "InPost Paczkomat" to "inpost",
            "Sameday easybox" to "sameday",
            "SunYou" to "sunyou",
            "Budbee" to "budbee",
        )
        expected.forEach { (text, domain) ->
            assertEquals(text, domain, carrierKey(text))
        }
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

    private fun parcel(entityId: String, barcode: String, plannedFrom: String? = null) = HAEntity(
        entity_id = entityId,
        state = "on",
        attributes = buildJsonObject {
            put("barcode", barcode)
            put("status", "in_transit")
            put("friendly_name", entityId)
            plannedFrom?.let { put("planned_from", it) }
        }
    )
}
