import React, { useState, useEffect, useRef, useMemo } from 'react';
import { ApiConfig, WizBulbDto, WizDevice, WizRoom } from './types/wiz';
import {
  loadApiConfig,
  saveApiConfig,
  loadCachedDevices,
  saveCachedDevices,
  clearLocalStorage,
} from './services/storage';
import { ApiClient } from './services/apiClient';
import { Navbar } from './components/Navbar';
import { DeviceDetailModal } from './components/DeviceDetailModal';
import { RoomDetailModal } from './components/RoomDetailModal';
import { DashboardPage } from './pages/DashboardPage';
import { RoomsPage } from './pages/RoomsPage';
import { DevicesPage } from './pages/DevicesPage';
import { SettingsPage } from './pages/SettingsPage';

export const App: React.FC = () => {
  const [apiConfig, setApiConfig] = useState<ApiConfig>(loadApiConfig);
  const [devices, setDevices] = useState<WizDevice[]>(loadCachedDevices);
  const [activeTab, setActiveTab] = useState<'dashboard' | 'rooms' | 'devices' | 'settings'>('dashboard');
  const [selectedDevice, setSelectedDevice] = useState<WizDevice | null>(null);
  const [selectedRoom, setSelectedRoom] = useState<WizRoom | null>(null);
  const [isDiscovering, setIsDiscovering] = useState<boolean>(false);
  const [isTesting, setIsTesting] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const apiClient = useMemo(() => new ApiClient(apiConfig), [apiConfig.baseUrl, apiConfig.apiToken]);
  const debounceTimers = useRef<Record<string, number>>({});

  // Theme Handling
  const currentTheme = useMemo<'light' | 'dark'>(() => {
    if (apiConfig.themeMode === 'system') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    return apiConfig.themeMode;
  }, [apiConfig.themeMode]);

  useEffect(() => {
    if (currentTheme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [currentTheme]);

  const toggleTheme = () => {
    const next: 'light' | 'dark' = currentTheme === 'dark' ? 'light' : 'dark';
    const updated: ApiConfig = { ...apiConfig, themeMode: next };
    setApiConfig(updated);
    saveApiConfig(updated);
  };

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((cur) => (cur === msg ? null : cur));
    }, 4000);
  };

  // Group Devices into Rooms
  const rooms: WizRoom[] = useMemo(() => {
    const map = new Map<number, { name: string; devices: WizDevice[] }>();

    for (const dev of devices) {
      const roomId = dev.roomId ?? -1;
      const roomName = dev.roomName || (roomId !== -1 ? `Raum ${roomId}` : 'Nicht zugeordnet');

      if (!map.has(roomId)) {
        map.set(roomId, { name: roomName, devices: [] });
      }
      map.get(roomId)!.devices.push(dev);
    }

    return Array.from(map.entries())
      .map(([id, group]) => {
        const devs = group.devices;
        const onlineCount = devs.filter((d) => d.isOnline).length;
        const activeCount = devs.filter((d) => d.isOnline && d.state.state).length;
        const lights = devs.filter((d) => d.isOnline && d.state.state && (d.deviceType === 'light' || !d.deviceType));
        const avgDim =
          lights.length > 0
            ? Math.round(lights.reduce((acc, d) => acc + (d.state.dimming ?? 100), 0) / lights.length)
            : 0;

        return {
          id,
          name: group.name,
          devices: devs,
          totalCount: devs.length,
          activeCount,
          onlineCount,
          averageBrightness: avgDim,
        };
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [devices]);

  // Initial Discovery if cache is empty
  useEffect(() => {
    if (devices.length === 0 && apiConfig.baseUrl) {
      handleDiscover(false);
    }
  }, []);

  // Periodic Auto Refresh
  useEffect(() => {
    if (!apiConfig.autoRefresh) return;
    const intervalMs = Math.max(10, apiConfig.refreshIntervalSeconds) * 1000;
    const timer = setInterval(() => {
      if (!isDiscovering) {
        handleDiscover(false);
      }
    }, intervalMs);
    return () => clearInterval(timer);
  }, [apiConfig.autoRefresh, apiConfig.refreshIntervalSeconds, isDiscovering]);

  const handleDiscover = async (showFeedback = true) => {
    if (isDiscovering) return;
    setIsDiscovering(true);

    try {
      const bulbs: WizBulbDto[] = await apiClient.discover();
      const now = Date.now();

      setDevices((prev) => {
        const deviceMap = new Map<string, WizDevice>();
        prev.forEach((d) => deviceMap.set(d.mac || d.ip, { ...d, isOnline: false }));

        for (const b of bulbs) {
          const key = (b.mac?.trim().toLowerCase() || '').length > 0 ? b.mac!.trim().toLowerCase() : b.ip.trim();
          if (!key) continue;

          const existing = deviceMap.get(key);
          const mapped: WizDevice = {
            ip: b.ip.trim(),
            mac: key,
            roomId: b.roomId ?? existing?.roomId ?? null,
            roomName: b.roomName?.trim() || existing?.roomName || (b.roomId ? `Raum ${b.roomId}` : 'Nicht zugeordnet'),
            deviceName: b.deviceName?.trim() || existing?.deviceName || `WiZ Lampe (${b.ip})`,
            deviceType: b.deviceType?.trim() || existing?.deviceType || 'light',
            moduleName: b.moduleName || existing?.moduleName,
            firmwareVersion: b.firmwareVersion || existing?.firmwareVersion,
            state: b.state || existing?.state || { state: false, dimming: 100 },
            rssi: b.rssi ?? existing?.rssi,
            isOnline: true,
            lastSeenTimestamp: now,
          };
          deviceMap.set(key, mapped);
        }

        const updated = Array.from(deviceMap.values()).sort((a, b) => a.deviceName.localeCompare(b.deviceName));
        saveCachedDevices(updated);
        return updated;
      });

      const updatedCfg = { ...apiConfig, lastSyncTimestamp: now };
      setApiConfig(updatedCfg);
      saveApiConfig(updatedCfg);

      if (showFeedback) {
        showToast(`${bulbs.length} WiZ-Gerät(e) erfolgreich erkannt.`);
      }
    } catch (err: any) {
      if (showFeedback) {
        showToast(err.message || 'Geräteerkennung fehlgeschlagen.');
      }
    } finally {
      setIsDiscovering(false);
    }
  };

  const handleToggleDevice = async (device: WizDevice) => {
    const nextState = !device.state.state;
    // Optimistic update
    setDevices((prev) =>
      prev.map((d) => (d.mac === device.mac ? { ...d, state: { ...d.state, state: nextState } } : d))
    );
    if (selectedDevice?.mac === device.mac) {
      setSelectedDevice((cur) => (cur ? { ...cur, state: { ...cur.state, state: nextState } } : null));
    }

    try {
      if (nextState) {
        await apiClient.turnOn(device.ip);
      } else {
        await apiClient.turnOff(device.ip);
      }
    } catch (err: any) {
      // Rollback
      setDevices((prev) =>
        prev.map((d) => (d.mac === device.mac ? { ...d, state: { ...d.state, state: !nextState } } : d))
      );
      showToast(`Fehler beim Schalten von ${device.deviceName}: ${err.message}`);
    }
  };

  const handleBrightnessChange = (device: WizDevice, brightness: number) => {
    const clamped = Math.max(1, Math.min(100, brightness));
    setDevices((prev) =>
      prev.map((d) =>
        d.mac === device.mac
          ? { ...d, state: { ...d.state, dimming: clamped, state: true } }
          : d
      )
    );
    if (selectedDevice?.mac === device.mac) {
      setSelectedDevice((cur) => (cur ? { ...cur, state: { ...cur.state, dimming: clamped, state: true } } : null));
    }

    // Debounce network request
    const key = device.mac || device.ip;
    if (debounceTimers.current[key]) {
      clearTimeout(debounceTimers.current[key]);
    }
    debounceTimers.current[key] = window.setTimeout(async () => {
      try {
        await apiClient.setBrightness(device.ip, clamped);
      } catch (err: any) {
        showToast(`Fehler beim Dimmen: ${err.message}`);
      }
    }, 280);
  };

  const handleColorChange = async (device: WizDevice, r: number, g: number, b: number) => {
    setDevices((prev) =>
      prev.map((d) =>
        d.mac === device.mac
          ? { ...d, state: { ...d.state, r, g, b, state: true } }
          : d
      )
    );
    if (selectedDevice?.mac === device.mac) {
      setSelectedDevice((cur) => (cur ? { ...cur, state: { ...cur.state, r, g, b, state: true } } : null));
    }

    try {
      await apiClient.setColor(device.ip, r, g, b);
    } catch (err: any) {
      showToast(`Fehler beim Setzen der Farbe: ${err.message}`);
    }
  };

  const handleToggleRoom = async (room: WizRoom, turnOn: boolean) => {
    setDevices((prev) =>
      prev.map((d) =>
        d.roomId === room.id && d.isOnline ? { ...d, state: { ...d.state, state: turnOn } } : d
      )
    );

    const onlineDevices = room.devices.filter((d) => d.isOnline);
    for (const d of onlineDevices) {
      try {
        if (turnOn) await apiClient.turnOn(d.ip);
        else await apiClient.turnOff(d.ip);
      } catch (err: any) {
        console.error(err);
      }
    }
  };

  const handleRoomBrightnessChange = (room: WizRoom, brightness: number) => {
    const clamped = Math.max(1, Math.min(100, brightness));
    setDevices((prev) =>
      prev.map((d) =>
        d.roomId === room.id && d.isOnline && (d.deviceType === 'light' || !d.deviceType)
          ? { ...d, state: { ...d.state, dimming: clamped, state: true } }
          : d
      )
    );

    const lights = room.devices.filter((d) => d.isOnline && (d.deviceType === 'light' || !d.deviceType));
    const timerKey = `room_${room.id}`;
    if (debounceTimers.current[timerKey]) {
      clearTimeout(debounceTimers.current[timerKey]);
    }
    debounceTimers.current[timerKey] = window.setTimeout(async () => {
      for (const d of lights) {
        try {
          await apiClient.setBrightness(d.ip, clamped);
        } catch (err: any) {
          console.error(err);
        }
      }
    }, 280);
  };

  const handleBroadcastAll = async (turnOn: boolean) => {
    setDevices((prev) =>
      prev.map((d) => (d.isOnline ? { ...d, state: { ...d.state, state: turnOn } } : d))
    );

    try {
      if (turnOn) await apiClient.broadcastOn();
      else await apiClient.broadcastOff();
      showToast(turnOn ? 'Alle Geräte eingeschaltet.' : 'Alle Geräte ausgeschaltet.');
    } catch (err: any) {
      showToast(`Broadcast fehlgeschlagen: ${err.message}`);
    }
  };

  const handleTestConnection = async () => {
    setIsTesting(true);
    const now = Date.now();
    try {
      const msg = await apiClient.checkHealth();
      const updated = {
        ...apiConfig,
        lastTestStatus: true,
        lastTestMessage: msg,
        lastTestTimestamp: now,
      };
      setApiConfig(updated);
      saveApiConfig(updated);
      showToast(`Verbindungstest erfolgreich: ${msg}`);
    } catch (err: any) {
      const updated = {
        ...apiConfig,
        lastTestStatus: false,
        lastTestMessage: err.message,
        lastTestTimestamp: now,
      };
      setApiConfig(updated);
      saveApiConfig(updated);
      showToast(`Verbindungstest fehlgeschlagen: ${err.message}`);
    } finally {
      setIsTesting(false);
    }
  };

  const handleSaveConfig = (newConfig: ApiConfig) => {
    setApiConfig(newConfig);
    saveApiConfig(newConfig);
    showToast('Einstellungen erfolgreich gespeichert.');
  };

  const handleResetData = () => {
    clearLocalStorage();
    setDevices([]);
    setApiConfig(loadApiConfig());
    showToast('Konfiguration und Gerätedaten wurden zurückgesetzt.');
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        apiConfig={apiConfig}
        isDiscovering={isDiscovering}
        onRefresh={() => handleDiscover(true)}
        theme={currentTheme}
        toggleTheme={toggleTheme}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-6 pb-12">
        {activeTab === 'dashboard' && (
          <DashboardPage
            devices={devices}
            rooms={rooms}
            onBroadcastAll={handleBroadcastAll}
            onRefreshDiscovery={() => handleDiscover(true)}
            onToggleDevice={handleToggleDevice}
            onDeviceBrightnessChange={handleBrightnessChange}
            onDeviceClick={setSelectedDevice}
            onToggleRoom={handleToggleRoom}
            onRoomClick={setSelectedRoom}
            onNavigateToSettings={() => setActiveTab('settings')}
          />
        )}

        {activeTab === 'rooms' && (
          <RoomsPage
            rooms={rooms}
            onToggleRoom={handleToggleRoom}
            onRoomClick={setSelectedRoom}
            onRefreshDiscovery={() => handleDiscover(true)}
          />
        )}

        {activeTab === 'devices' && (
          <DevicesPage
            devices={devices}
            onToggleDevice={handleToggleDevice}
            onBrightnessChange={handleBrightnessChange}
            onDeviceClick={setSelectedDevice}
          />
        )}

        {activeTab === 'settings' && (
          <SettingsPage
            apiConfig={apiConfig}
            isTesting={isTesting}
            isDiscovering={isDiscovering}
            onTestConnection={handleTestConnection}
            onSaveConfig={handleSaveConfig}
            onRefreshDiscovery={() => handleDiscover(true)}
            onResetData={handleResetData}
          />
        )}
      </main>

      {/* Modals */}
      <DeviceDetailModal
        device={selectedDevice}
        onClose={() => setSelectedDevice(null)}
        onToggle={handleToggleDevice}
        onBrightnessChange={handleBrightnessChange}
        onColorChange={handleColorChange}
      />

      <RoomDetailModal
        room={selectedRoom}
        onClose={() => setSelectedRoom(null)}
        onToggleDevice={handleToggleDevice}
        onDeviceBrightnessChange={handleBrightnessChange}
        onDeviceClick={setSelectedDevice}
        onToggleRoom={handleToggleRoom}
        onRoomBrightnessChange={handleRoomBrightnessChange}
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-20 md:bottom-6 right-6 z-50 max-w-md bg-slate-900/95 dark:bg-white/95 text-white dark:text-slate-900 px-4 py-3 rounded-2xl shadow-xl text-xs sm:text-sm font-medium animate-bounce border border-slate-700/50 dark:border-slate-200">
          {toastMessage}
        </div>
      )}
    </div>
  );
};
