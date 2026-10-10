import React, { useState, useMemo } from 'react';
import { Search, SlidersHorizontal, X } from 'lucide-react';
import { WizDevice } from '../types/wiz';
import { DeviceCard } from '../components/DeviceCard';

interface DevicesPageProps {
  devices: WizDevice[];
  onToggleDevice: (device: WizDevice) => void;
  onBrightnessChange: (device: WizDevice, brightness: number) => void;
  onDeviceClick: (device: WizDevice) => void;
}

export const DevicesPage: React.FC<DevicesPageProps> = ({
  devices,
  onToggleDevice,
  onBrightnessChange,
  onDeviceClick,
}) => {
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE' | 'LIGHTS' | 'PLUGS' | 'OFFLINE'>('ALL');

  const filteredDevices = useMemo(() => {
    return devices.filter((d) => {
      const matchSearch =
        !search ||
        d.deviceName.toLowerCase().includes(search.toLowerCase()) ||
        d.roomName.toLowerCase().includes(search.toLowerCase()) ||
        d.ip.includes(search);

      const isLight = d.deviceType.toLowerCase() === 'light' || !d.deviceType;
      const isPlug = d.deviceType.toLowerCase() === 'plug';

      const matchFilter = (() => {
        switch (filter) {
          case 'ACTIVE':
            return d.isOnline && d.state.state;
          case 'INACTIVE':
            return d.isOnline && !d.state.state;
          case 'LIGHTS':
            return isLight;
          case 'PLUGS':
            return isPlug;
          case 'OFFLINE':
            return !d.isOnline;
          default:
            return true;
        }
      })();

      return matchSearch && matchFilter;
    });
  }, [devices, search, filter]);

  return (
    <div className="space-y-6 animate-fade-in pb-20 md:pb-8">
      <div>
        <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
          Alle Geräte
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          {filteredDevices.length} von {devices.length} Geräten angezeigt
        </p>
      </div>

      {/* Search & Filter Bar */}
      <div className="space-y-3">
        <div className="relative">
          <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Gerätename, Raum oder IP-Adresse..."
            className="w-full pl-11 pr-10 py-3 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
          />
          {search && (
            <button
              onClick={() => setSearch('')}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Filter Chips */}
        <div className="flex gap-2 overflow-x-auto pb-1">
          {(
            [
              { id: 'ALL', label: 'Alle' },
              { id: 'ACTIVE', label: 'Eingeschaltet' },
              { id: 'INACTIVE', label: 'Aus' },
              { id: 'LIGHTS', label: 'Lampen' },
              { id: 'PLUGS', label: 'Steckdosen' },
              { id: 'OFFLINE', label: 'Offline' },
            ] as const
          ).map((chip) => (
            <button
              key={chip.id}
              onClick={() => setFilter(chip.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                filter === chip.id
                  ? 'bg-amber-500 text-white shadow-sm'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
              }`}
            >
              {chip.label}
            </button>
          ))}
        </div>
      </div>

      {/* Devices Grid */}
      {filteredDevices.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filteredDevices.map((device) => (
            <DeviceCard
              key={device.mac || device.ip}
              device={device}
              onToggle={onToggleDevice}
              onBrightnessChange={onBrightnessChange}
              onClick={onDeviceClick}
            />
          ))}
        </div>
      ) : (
        <div className="p-8 rounded-3xl bg-slate-100/70 dark:bg-slate-800/40 border border-dashed border-slate-300 dark:border-slate-700 text-center">
          <SlidersHorizontal className="w-10 h-10 text-slate-400 mx-auto mb-2" />
          <h3 className="text-base font-bold text-slate-800 dark:text-slate-200">
            Keine passenden Geräte
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Überprüfe deinen Suchbegriff oder ändere die Filterkriterien.
          </p>
        </div>
      )}
    </div>
  );
};
