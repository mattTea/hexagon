package com.hexagonkt.serialization.csv

import com.hexagonkt.core.serialization.*
import com.hexagonkt.serialization.json.Json
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import kotlin.test.assertFailsWith

@TestInstance(PER_CLASS)
internal class CsvTest {
    data class Player (val name: String, val number: Int, val category: Int)

    enum class DeviceOs { ANDROID, IOS }

    data class Device(
        val id: String,
        val brand: String,
        val model: String,
        val os: DeviceOs,
        val osVersion: String,

        val alias: String = "$brand $model"
    )

    @BeforeAll fun initialize() {
        SerializationManager.formats = linkedSetOf(Json)
    }

    @Test fun `CSV is serialized properly` () {
        val player = Player("Michael", 23, 18)
        val serializedPlayer = player.serialize(Csv)
        val deserializedPlayer = serializedPlayer.parse<Player>(Csv)

        assert (player.name == deserializedPlayer.name)
        assert (player.number == deserializedPlayer.number)
        assert (player.category == deserializedPlayer.category)

        val players = listOf(player, Player("Magic", 32, 36))
        val serializedPlayers = players.serialize(Csv)
        val deserializedPlayers = serializedPlayers.parseObjects<Player>(Csv)

        assert (players[0].name == deserializedPlayers[0].name)
        assert (players[0].number == deserializedPlayers[0].number)
        assert (players[0].category == deserializedPlayers[0].category)

        assert (players[1].name == deserializedPlayers[1].name)
        assert (players[1].number == deserializedPlayers[1].number)
        assert (players[1].category == deserializedPlayers[1].category)
    }

    @Test fun `Parse invalid CSV type` () {
        assertFailsWith<ParseException> {
            "Michael,23,error".parse(Player::class, Csv)
        }
    }

    @Test fun `Parse exceptions contains failed field`() {
        try {
            "f,br,mo,ANDROI,v,al".parse(Device::class, Csv)

            assert(false) { "Exception expected" }
        }
        catch (e: ParseException) {
            assert(e.field == "com.hexagonkt.serialization.csv.CsvTest\$Device[\"os\"]")
        }
    }

    @Test fun `Invalid format exceptions field is 'null'`() {
        try {
            "f,br,mo,ANDROI,v,\"al".parse(Device::class)

            assert(false) { "Exception expected" }
        }
        catch (e: ParseException) {
            assert(e.field == "")
        }
    }

    @Test fun `Parse an invalid class list throws exception`() {
        assertFailsWith<ParseException> {
            "f,br,mo,ANDROI,v,al".parseObjects(Device::class, Csv)
        }
    }

    @Test fun `Serialize an empty list works as expected`() {
        assert(emptyList<Any>().serialize(Csv) == "")
    }
}
