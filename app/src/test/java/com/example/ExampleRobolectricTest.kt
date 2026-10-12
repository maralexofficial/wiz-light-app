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

    @Test
    fun `test WizPreset model and target types`() {
        val groupPreset = com.example.data.model.WizPreset(
            id = "preset_1",
            name = "Gemütlicher Abend",
            icon = "night",
            targetType = "group",
            targetId = "grp_100",
            targetName = "Wohnzimmer Lampen",
            state = true,
            dimming = 40,
            temperature = 2200
        )

        assertTrue(groupPreset.isGroupTarget)
        assertFalse(groupPreset.isDeviceTarget)
        assertEquals("Gemütlicher Abend", groupPreset.name)
        assertEquals(40, groupPreset.dimming)
        assertEquals(2200, groupPreset.temperature)

        val devicePreset = com.example.data.model.WizPreset(
            id = "preset_2",
            name = "Leselampe An",
            icon = "lightbulb",
            targetType = "device",
            targetId = "cc4085a3349e",
            targetName = "Stehlampe",
            state = true,
            dimming = 90
        )

        assertTrue(devicePreset.isDeviceTarget)
        assertFalse(devicePreset.isGroupTarget)
    }

    @Test
    fun `test PreferencesManager hidden devices and preset caching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsManager = com.example.data.local.PreferencesManager(context)

        // Test Hidden Devices
        assertFalse(prefsManager.isDeviceHidden("dev_123"))
        prefsManager.setDeviceHidden("dev_123", true)
        assertTrue(prefsManager.isDeviceHidden("dev_123"))
        assertTrue(prefsManager.getHiddenDeviceIds().contains("dev_123"))

        prefsManager.setDeviceHidden("dev_123", false)
        assertFalse(prefsManager.isDeviceHidden("dev_123"))

        // Test Preset DTO Caching
        val presetDto = com.example.data.model.WizPresetDto(
            id = "p_1",
            name = "Nachtmodus",
            icon = "night",
            targetType = "group",
            targetId = "grp_1",
            state = true,
            dimming = 20
        )
        prefsManager.saveCachedPresets(listOf(presetDto))
        val cached = prefsManager.getCachedPresets()
        assertEquals(1, cached.size)
        assertEquals("Nachtmodus", cached[0].name)
        assertEquals("p_1", cached[0].id)
    }

    @Test
    fun `test ApiConfig syncMode default and serialization`() {
        val cfg = com.example.data.model.ApiConfig(syncMode = "MANUAL")
        assertEquals("MANUAL", cfg.syncMode)

        val autoCfg = com.example.data.model.ApiConfig()
        assertEquals("AUTO", autoCfg.syncMode)
    }
}
