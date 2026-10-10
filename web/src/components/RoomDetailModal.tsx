import React from 'react';
import { X, Power } from 'lucide-react';
import { WizDevice, WizRoom } from '../types/wiz';
import { DeviceCard } from './DeviceCard';

interface RoomDetailModalProps {
  room: WizRoom | null;
  onClose: () => void;
  onToggleDevice: (device: WizDevice) => void;
  onDeviceBrightnessChange: (device: WizDevice, brightness: number) => void;
  onDeviceClick: (device: WizDevice) => void;
  onToggleRoom: (room: WizRoom, turnOn: boolean) => void;
  onRoomBrightnessChange: (room: WizRoom, brightness: number) => void;
}

export const RoomDetailModal: React.FC<RoomDetailModalProps> = ({
  room,
  onClose,
  onToggleDevice,
  onDeviceBrightnessChange,
  onDeviceClick,
  onToggleRoom,
  onRoomBrightnessChange,
}) => {
  if (!room) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
      <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 dark:border-slate-800 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
              {room.name}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              {room.totalCount} Geräte • {room.activeCount} aktiv
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Master Room Controls */}
        <div className="my-6 p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
          <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200 mb-3">
            Raumsteuerung
          </h4>
          <div className="flex gap-3 mb-4">
            <button
              onClick={() => onToggleRoom(room, true)}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-medium text-sm transition"
            >
              <Power className="w-4 h-4" />
              Alle Ein
            </button>
            <button
              onClick={() => onToggleRoom(room, false)}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-800 dark:text-slate-200 font-medium text-sm transition"
            >
              Alle Aus
            </button>
          </div>

          {room.devices.some((d) => d.deviceType.toLowerCase() === 'light') && (
            <div>
              <div className="flex justify-between items-center text-xs font-medium mb-1">
                <span className="text-slate-500 dark:text-slate-400">
                  Gemeinsame Helligkeit
                </span>
                <span className="font-bold text-amber-500">{room.averageBrightness}%</span>
              </div>
              <input
                type="range"
                min="1"
                max="100"
                value={room.averageBrightness}
                onChange={(e) => onRoomBrightnessChange(room, parseInt(e.target.value, 10))}
                className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
              />
            </div>
          )}
        </div>

        {/* Device Grid */}
        <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200 mb-3">
          Geräte in diesem Raum
        </h4>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {room.devices.map((device) => (
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
    </div>
  );
};
