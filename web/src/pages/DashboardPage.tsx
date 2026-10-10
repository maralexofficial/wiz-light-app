import React from 'react';
import { Power, RefreshCw, Smartphone, Layers, CheckCircle, AlertTriangle } from 'lucide-react';
import { WizDevice, WizRoom } from '../types/wiz';
import { DeviceCard } from '../components/DeviceCard';
import { RoomCard } from '../components/RoomCard';

interface DashboardPageProps {
  devices: WizDevice[];
  rooms: WizRoom[];
  onBroadcastAll: (turnOn: boolean) => void;
  onRefreshDiscovery: () => void;
  onToggleDevice: (device: WizDevice) => void;
  onDeviceBrightnessChange: (device: WizDevice, brightness: number) => void;
  onDeviceClick: (device: WizDevice) => void;
  onToggleRoom: (room: WizRoom, turnOn: boolean) => void;
  onRoomClick: (room: WizRoom) => void;
  onNavigateToSettings: () => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  devices,
  rooms,
  onBroadcastAll,
  onRefreshDiscovery,
  onToggleDevice,
  onDeviceBrightnessChange,
  onDeviceClick,
  onToggleRoom,
  onRoomClick,
  onNavigateToSettings,
}) => {
  const totalCount = devices.length;
  const activeCount = devices.filter((d) => d.isOnline && d.state.state).length;
  const onlineCount = devices.filter((d) => d.isOnline).length;
  const offlineCount = devices.filter((d) => !d.isOnline).length;

  return (
    <div className="space-y-8 animate-fade-in pb-20 md:pb-8">
      {/* Welcome Banner */}
      <div>
        <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
          Mein Zuhause
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          {totalCount > 0
            ? `${activeCount} von ${totalCount} Geräten eingeschaltet`
            : 'Noch keine Geräte verbunden'}
        </p>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center gap-2 text-sky-500 mb-2">
            <Smartphone className="w-4 h-4" />
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Gesamt
            </span>
          </div>
          <span className="text-2xl font-black text-slate-900 dark:text-slate-100">
            {totalCount}
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center gap-2 text-amber-500 mb-2">
            <Power className="w-4 h-4" />
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Aktiv
            </span>
          </div>
          <span className="text-2xl font-black text-slate-900 dark:text-slate-100">
            {activeCount}
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center gap-2 text-emerald-500 mb-2">
            <CheckCircle className="w-4 h-4" />
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Online
            </span>
          </div>
          <span className="text-2xl font-black text-slate-900 dark:text-slate-100">
            {onlineCount}
          </span>
        </div>

        <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="flex items-center gap-2 text-red-500 mb-2">
            <AlertTriangle className="w-4 h-4" />
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Offline
            </span>
          </div>
          <span className="text-2xl font-black text-slate-900 dark:text-slate-100">
            {offlineCount}
          </span>
        </div>
      </div>

      {/* Broadcast Master Control */}
      <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
        <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100 mb-3">
          Zuhause-Schnellsteuerung
        </h3>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => onBroadcastAll(true)}
            className="flex-1 min-w-[130px] flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-medium text-sm transition shadow-sm shadow-amber-500/20"
          >
            <Power className="w-4 h-4" />
            Alles Ein
          </button>
          <button
            onClick={() => onBroadcastAll(false)}
            className="flex-1 min-w-[130px] flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-medium text-sm transition"
          >
            Alles Aus
          </button>
          <button
            onClick={onRefreshDiscovery}
            className="flex items-center justify-center gap-2 py-3 px-4 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium text-sm transition"
          >
            <RefreshCw className="w-4 h-4" />
            Suche
          </button>
        </div>
      </div>

      {/* Rooms Showcase */}
      {rooms.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
              Räume ({rooms.length})
            </h2>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {rooms.map((room) => (
              <RoomCard
                key={room.id}
                room={room}
                onToggleRoom={onToggleRoom}
                onClick={onRoomClick}
              />
            ))}
          </div>
        </div>
      )}

      {/* Devices Showcase */}
      {devices.length > 0 ? (
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100 mb-4">
            Geräte
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {devices.map((device) => (
              <DeviceCard
                key={device.mac || device.ip}
                device={device}
                onToggle={onToggleDevice}
                onBrightnessChange={onDeviceBrightnessChange}
                onClick={onDeviceClick}
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="p-8 rounded-3xl bg-slate-100/70 dark:bg-slate-800/40 border border-dashed border-slate-300 dark:border-slate-700 text-center">
          <Layers className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-slate-800 dark:text-slate-200">
            Noch keine WiZ-Geräte geladen
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto mt-1 mb-4">
            Konfiguriere deine API-URL in den Einstellungen oder starte die automatische
            Netzwerk-Erkennung.
          </p>
          <button
            onClick={onNavigateToSettings}
            className="py-2.5 px-5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-medium text-sm transition"
          >
            Einstellungen öffnen
          </button>
        </div>
      )}
    </div>
  );
};
