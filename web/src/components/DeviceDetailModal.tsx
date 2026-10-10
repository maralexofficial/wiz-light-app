import React, { useState } from 'react';
import { X, Power, ChevronDown, ChevronUp, Wifi } from 'lucide-react';
import { WizDevice } from '../types/wiz';

interface DeviceDetailModalProps {
  device: WizDevice | null;
  onClose: () => void;
  onToggle: (device: WizDevice) => void;
  onBrightnessChange: (device: WizDevice, brightness: number) => void;
  onColorChange: (device: WizDevice, r: number, g: number, b: number) => void;
}

const PRESET_COLORS = [
  { name: 'Warmweiß', r: 255, g: 214, b: 170, hex: '#ffd6aa' },
  { name: 'Tageslicht', r: 255, g: 244, b: 229, hex: '#fff4e5' },
  { name: 'Bernstein', r: 255, g: 183, b: 3, hex: '#ffb703' },
  { name: 'Sonnenuntergang', r: 251, g: 133, b: 0, hex: '#fb8500' },
  { name: 'Rubinrot', r: 239, g: 68, b: 68, hex: '#ef4444' },
  { name: 'Koralle', r: 249, g: 115, b: 22, hex: '#f97316' },
  { name: 'Smaragd', r: 16, g: 185, b: 129, hex: '#10b981' },
  { name: 'Türkis', r: 6, g: 182, b: 212, hex: '#06b6d4' },
  { name: 'Ozeanblau', r: 59, g: 130, b: 246, hex: '#3b82f6' },
  { name: 'Purpur', r: 168, g: 85, b: 247, hex: '#a855f7' },
  { name: 'Magenta', r: 236, g: 72, b: 153, hex: '#ec4899' },
];

export const DeviceDetailModal: React.FC<DeviceDetailModalProps> = ({
  device,
  onClose,
  onToggle,
  onBrightnessChange,
  onColorChange,
}) => {
  const [showTechDetails, setShowTechDetails] = useState(false);

  if (!device) return null;

  const isLight = device.deviceType.toLowerCase() === 'light' || !device.deviceType;
  const isOn = device.isOnline && device.state.state;
  const brightness = device.state.dimming ?? 100;
  const supportsRgb =
    isLight &&
    (device.state.r !== undefined ||
      device.state.g !== undefined ||
      device.state.b !== undefined ||
      device.moduleName?.includes('RGB') === true);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 dark:border-slate-800 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
              {device.deviceName}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {device.roomName}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Large Power Button */}
        <div className="flex flex-col items-center justify-center my-6">
          <button
            type="button"
            onClick={() => onToggle(device)}
            disabled={!device.isOnline}
            className={`w-24 h-24 rounded-full flex items-center justify-center transition-all ${
              !device.isOnline
                ? 'bg-slate-100 dark:bg-slate-800 text-slate-400 border-2 border-dashed border-slate-300 dark:border-slate-700'
                : isOn
                ? 'bg-amber-500/20 text-amber-500 border-2 border-amber-500 shadow-lg shadow-amber-500/20'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-400 border-2 border-slate-300 dark:border-slate-700'
            }`}
          >
            <Power className="w-10 h-10" />
          </button>
          <span
            className={`mt-3 text-sm font-semibold ${
              !device.isOnline
                ? 'text-red-500'
                : isOn
                ? 'text-amber-500'
                : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            {!device.isOnline ? 'Offline' : isOn ? 'Eingeschaltet' : 'Ausgeschaltet'}
          </span>
        </div>

        {/* Brightness Slider */}
        {device.isOnline && isLight && (
          <div className="bg-slate-50 dark:bg-slate-800/50 p-4 rounded-2xl mb-4">
            <div className="flex justify-between items-center text-sm font-medium mb-2">
              <span className="text-slate-700 dark:text-slate-300">Helligkeit</span>
              <span className="font-bold text-amber-500">{brightness}%</span>
            </div>
            <input
              type="range"
              min="1"
              max="100"
              value={brightness}
              onChange={(e) => onBrightnessChange(device, parseInt(e.target.value, 10))}
              className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
            />
          </div>
        )}

        {/* RGB Color Palette (Only if supported!) */}
        {device.isOnline && supportsRgb && (
          <div className="bg-slate-50 dark:bg-slate-800/50 p-4 rounded-2xl mb-4">
            <h4 className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-3">
              Lichtfarben & Stimmungen
            </h4>
            <div className="flex flex-wrap gap-2">
              {PRESET_COLORS.map((preset) => (
                <button
                  key={preset.name}
                  onClick={() => onColorChange(device, preset.r, preset.g, preset.b)}
                  className="w-8 h-8 rounded-full border border-black/10 dark:border-white/10 shadow-sm hover:scale-110 transition-transform"
                  style={{ backgroundColor: preset.hex }}
                  title={preset.name}
                />
              ))}
            </div>
          </div>
        )}

        {/* Collapsible Tech Information */}
        <div className="border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden mt-4">
          <button
            onClick={() => setShowTechDetails(!showTechDetails)}
            className="w-full flex items-center justify-between p-4 text-sm font-semibold text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/40"
          >
            <span>Geräteinformationen</span>
            {showTechDetails ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>

          {showTechDetails && (
            <div className="p-4 space-y-2 text-xs text-slate-600 dark:text-slate-400 bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800">
              <div className="flex justify-between">
                <span>IP-Adresse:</span>
                <span className="font-mono text-slate-900 dark:text-slate-200">{device.ip}</span>
              </div>
              <div className="flex justify-between">
                <span>MAC-Adresse:</span>
                <span className="font-mono text-slate-900 dark:text-slate-200">{device.mac}</span>
              </div>
              <div className="flex justify-between">
                <span>Gerätetyp:</span>
                <span className="capitalize">{device.deviceType}</span>
              </div>
              {device.moduleName && (
                <div className="flex justify-between">
                  <span>Modul:</span>
                  <span>{device.moduleName}</span>
                </div>
              )}
              {device.firmwareVersion && (
                <div className="flex justify-between">
                  <span>Firmware:</span>
                  <span>{device.firmwareVersion}</span>
                </div>
              )}
              {device.rssi && (
                <div className="flex justify-between items-center">
                  <span>WLAN-Signal (RSSI):</span>
                  <span className="flex items-center gap-1 font-mono">
                    <Wifi className="w-3.5 h-3.5 text-emerald-500" />
                    {device.rssi} dBm
                  </span>
                </div>
              )}
              <div className="flex justify-between">
                <span>Zuletzt gesehen:</span>
                <span>{new Date(device.lastSeenTimestamp).toLocaleTimeString('de-DE')}</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
