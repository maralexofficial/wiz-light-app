import React from 'react';
import { Lightbulb, Zap, WifiOff } from 'lucide-react';
import { WizDevice } from '../types/wiz';

interface DeviceCardProps {
  device: WizDevice;
  onToggle: (device: WizDevice) => void;
  onBrightnessChange: (device: WizDevice, brightness: number) => void;
  onClick: (device: WizDevice) => void;
}

export const DeviceCard: React.FC<DeviceCardProps> = ({
  device,
  onToggle,
  onBrightnessChange,
  onClick,
}) => {
  const isLight = device.deviceType.toLowerCase() === 'light' || !device.deviceType;
  const isOn = device.isOnline && device.state.state;
  const brightness = device.state.dimming ?? 100;

  return (
    <div
      onClick={() => onClick(device)}
      className={`group relative rounded-2xl p-4 transition-all duration-200 cursor-pointer border ${
        !device.isOnline
          ? 'bg-slate-100/60 dark:bg-slate-800/40 border-slate-200 dark:border-slate-800 opacity-60'
          : isOn
          ? 'bg-amber-50/70 dark:bg-amber-950/20 border-amber-300/80 dark:border-amber-700/60 shadow-sm'
          : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 shadow-sm'
      }`}
    >
      <div className="flex items-start justify-between">
        {/* Icon */}
        <div
          className={`w-11 h-11 rounded-xl flex items-center justify-center transition-colors ${
            !device.isOnline
              ? 'bg-slate-200 dark:bg-slate-700 text-slate-400'
              : isOn
              ? 'bg-amber-500/20 text-amber-500'
              : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
          }`}
        >
          {!device.isOnline ? (
            <WifiOff className="w-5 h-5" />
          ) : isLight ? (
            <Lightbulb className="w-5 h-5" />
          ) : (
            <Zap className="w-5 h-5" />
          )}
        </div>

        {/* Toggle Switch */}
        {device.isOnline ? (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onToggle(device);
            }}
            className={`w-12 h-7 rounded-full transition-colors relative flex items-center p-1 ${
              isOn ? 'bg-amber-500' : 'bg-slate-300 dark:bg-slate-700'
            }`}
          >
            <div
              className={`w-5 h-5 rounded-full bg-white transition-transform ${
                isOn ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        ) : (
          <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-red-100 dark:bg-red-950/40 text-red-600 dark:text-red-400">
            Offline
          </span>
        )}
      </div>

      {/* Info */}
      <div className="mt-4">
        <h3 className="font-semibold text-slate-900 dark:text-slate-100 text-base truncate">
          {device.deviceName}
        </h3>
        <p className="text-xs text-slate-500 dark:text-slate-400 truncate mt-0.5">
          {device.roomName}
        </p>
      </div>

      {/* Brightness Slider for Lights */}
      {device.isOnline && isLight && (
        <div className="mt-3" onClick={(e) => e.stopPropagation()}>
          <div className="flex justify-between items-center text-xs mb-1">
            <span className="text-slate-500 dark:text-slate-400">Helligkeit</span>
            <span className={`font-semibold ${isOn ? 'text-amber-500' : 'text-slate-400'}`}>
              {isOn ? `${brightness}%` : 'Aus'}
            </span>
          </div>
          <input
            type="range"
            min="1"
            max="100"
            value={brightness}
            onChange={(e) => onBrightnessChange(device, parseInt(e.target.value, 10))}
            className="w-full h-1.5 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
          />
        </div>
      )}
    </div>
  );
};
