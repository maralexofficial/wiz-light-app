import React from 'react';
import { DoorOpen } from 'lucide-react';
import { WizRoom } from '../types/wiz';

interface RoomCardProps {
  room: WizRoom;
  onToggleRoom: (room: WizRoom, turnOn: boolean) => void;
  onClick: (room: WizRoom) => void;
}

export const RoomCard: React.FC<RoomCardProps> = ({ room, onToggleRoom, onClick }) => {
  const hasActive = room.activeCount > 0;

  return (
    <div
      onClick={() => onClick(room)}
      className={`rounded-2xl p-5 border transition-all duration-200 cursor-pointer ${
        hasActive
          ? 'bg-amber-50/70 dark:bg-amber-950/20 border-amber-300/80 dark:border-amber-700/60 shadow-sm'
          : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 shadow-sm'
      }`}
    >
      <div className="flex items-start justify-between">
        <div
          className={`w-11 h-11 rounded-xl flex items-center justify-center transition-colors ${
            hasActive
              ? 'bg-amber-500/20 text-amber-500'
              : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
          }`}
        >
          <DoorOpen className="w-5 h-5" />
        </div>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onToggleRoom(room, !hasActive);
          }}
          disabled={room.onlineCount === 0}
          className={`w-12 h-7 rounded-full transition-colors relative flex items-center p-1 ${
            room.onlineCount === 0
              ? 'opacity-40 cursor-not-allowed bg-slate-300 dark:bg-slate-700'
              : hasActive
              ? 'bg-amber-500'
              : 'bg-slate-300 dark:bg-slate-700'
          }`}
        >
          <div
            className={`w-5 h-5 rounded-full bg-white transition-transform ${
              hasActive ? 'translate-x-5' : 'translate-x-0'
            }`}
          />
        </button>
      </div>

      <div className="mt-4">
        <h3 className="font-bold text-slate-900 dark:text-slate-100 text-lg truncate">
          {room.name}
        </h3>
        <p className="text-xs font-medium text-slate-500 dark:text-slate-400 mt-1">
          {room.totalCount === 0
            ? 'Keine Geräte'
            : hasActive
            ? `${room.totalCount} Geräte • ${room.activeCount} aktiv`
            : `${room.totalCount} Geräte • alle aus`}
        </p>
      </div>
    </div>
  );
};
