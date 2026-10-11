package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.WizDevice
import com.example.data.model.WizDeviceState
import com.example.data.model.WizRoom
import com.example.data.remote.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WiZ Home", appName)
    }

    @Test
    fun `test ApiClient url normalization`() {
        val client = ApiClient()
        assertEquals("http://192.168.1.100:3000/", client.normalizeBaseUrl("http://192.168.1.100:3000"))
        assertEquals("http://192.168.1.100:3000/", client.normalizeBaseUrl("http://192.168.1.100:3000/"))
        assertEquals("http://192.168.1.100:3000/", client.normalizeBaseUrl("192.168.1.100:3000"))
        assertEquals("https://home.wiz.local/", client.normalizeBaseUrl("https://home.wiz.local/"))
    }

    @Test
    fun `test WizDevice capabilities and state helpers`() {
        val lightDevice = WizDevice(
            ip = "192.168.1.20",
            mac = "cc4085a3349e",
            roomId = 123L,
            roomName = "Wohnzimmer",
            deviceName = "Deckenlampe",
            deviceType = "light",
            moduleName = "ESP25_SHTW_01",
            firmwareVersion = "1.39.0",
            state = WizDeviceState(state = true, dimming = 75, r = 255, g = 100, b = 50),
            rssi = -65,
            isOnline = true
        )

        assertTrue(lightDevice.isLight)
        assertFalse(lightDevice.isPlug)
        assertTrue(lightDevice.supportsRgb)
        assertTrue(lightDevice.supportsDimming)
        assertEquals(75, lightDevice.brightnessPercent)
        assertTrue(lightDevice.isTurnedOn)
    }

    @Test
    fun `test WizRoom calculations`() {
        val dev1 = WizDevice(
            ip = "192.168.1.20",
            mac = "cc4085a3349e",
            roomId = 1L,
            roomName = "Küche",
            deviceName = "Spot 1",
            deviceType = "light",
            moduleName = null,
            firmwareVersion = null,
            state = WizDeviceState(state = true, dimming = 60),
            rssi = -70,
            isOnline = true
        )

        val dev2 = WizDevice(
            ip = "192.168.1.21",
            mac = "cc4085a3349f",
            roomId = 1L,
            roomName = "Küche",
            deviceName = "Spot 2",
            deviceType = "light",
            moduleName = null,
            firmwareVersion = null,
            state = WizDeviceState(state = false, dimming = 40),
            rssi = -75,
            isOnline = true
        )

        val room = WizRoom(id = 1L, name = "Küche", devices = listOf(dev1, dev2))
        assertEquals(2, room.totalCount)
        assertEquals(1, room.activeCount)
        assertEquals(2, room.onlineCount)
        assertTrue(room.hasActiveLights)
        assertEquals(60, room.averageBrightness)
    }

    @Test
    fun `test WizGroup properties and device type differentiation`() {
        val lightGroup = com.example.data.model.WizGroup(
            id = "grp_1",
            name = "Deckenstrahler",
            roomId = 1L,
            deviceType = "light",
            deviceMacs = listOf("cc4085a3349e", "cc4085a3349f"),
            isTurnedOn = true,
            brightnessPercent = 80,
            temperatureKelvin = 2700
        )

        assertTrue(lightGroup.isLight)
        assertFalse(lightGroup.isPlug)
        assertEquals(2, lightGroup.memberCount)
        assertEquals(80, lightGroup.brightnessPercent)
        assertEquals(2700, lightGroup.temperatureKelvin)

        val plugGroup = com.example.data.model.WizGroup(
            id = "grp_2",
            name = "Schreibtisch-Steckdosen",
            roomId = 2L,
            deviceType = "plug",
            deviceMacs = listOf("cc4085a334aa"),
            isTurnedOn = false
        )

        assertTrue(plugGroup.isPlug)
        assertFalse(plugGroup.isLight)
        assertEquals(1, plugGroup.memberCount)
    }

    @Test
    fun `test GroupActionResult multi-status evaluation`() {
        val partialResult = com.example.data.model.GroupActionResult(
            isFullSuccess = false,
            isPartialSuccess = true,
            successfulCount = 2,
            failedCount = 1,
            message = "Teilweiser Erfolg (HTTP 207)"
        )
        assertFalse(partialResult.isFullSuccess)
        assertTrue(partialResult.isPartialSuccess)
        assertEquals(2, partialResult.successfulCount)
        assertEquals(1, partialResult.failedCount)

        val fullSuccess = com.example.data.model.GroupActionResult(
            isFullSuccess = true,
            isPartialSuccess = false,
            successfulCount = 3,
            failedCount = 0,
            message = "Gruppe erfolgreich geschaltet"
        )
        assertTrue(fullSuccess.isFullSuccess)
        assertFalse(fullSuccess.isPartialSuccess)
    }
}
