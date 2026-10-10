import React from 'react';
import { Layers, RefreshCw } from 'lucide-react';
import { WizRoom } from '../types/wiz';
import { RoomCard } from '../components/RoomCard';

interface RoomsPageProps {
  rooms: WizRoom[];
  onToggleRoom: (room: WizRoom, turnOn: boolean) => void;
  onRoomClick: (room: WizRoom) => void;
  onRefreshDiscovery: () => void;
}

export const RoomsPage: React.FC<RoomsPageProps> = ({
  rooms,
  onToggleRoom,
  onRoomClick,
  onRefreshDiscovery,
}) => {
  const totalDevs = rooms.reduce((acc, r) => acc + r.totalCount, 0);
  const activeDevs = rooms.reduce((acc, r) => acc + r.activeCount, 0);

  return (
    <div className="space-y-6 animate-fade-in pb-20 md:pb-8">
      <div>
        <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
          Räume
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          {rooms.length} Räume • {totalDevs} Geräte ({activeDevs} aktiv)
        </p>
      </div>

      {rooms.length > 0 ? (
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
      ) : (
        <div className="p-8 rounded-3xl bg-slate-100/70 dark:bg-slate-800/40 border border-dashed border-slate-300 dark:border-slate-700 text-center">
          <Layers className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-slate-800 dark:text-slate-200">
            Keine Räume gefunden
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto mt-1 mb-4">
            Räume werden automatisch aus den Geräte-Metadaten erstellt. Starte einen
            Suchlauf, um Geräte hinzuzufügen.
          </p>
          <button
            onClick={onRefreshDiscovery}
            className="flex items-center gap-2 mx-auto py-2.5 px-5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-medium text-sm transition"
          >
            <RefreshCw className="w-4 h-4" />
            Geräte suchen
          </button>
        </div>
      )}
    </div>
  );
};
