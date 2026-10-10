import React from 'react';
import { Home, Layers, Sliders, Settings, RefreshCw, Sun, Moon } from 'lucide-react';
import { ApiConfig } from '../types/wiz';

interface NavbarProps {
  activeTab: 'dashboard' | 'rooms' | 'devices' | 'settings';
  setActiveTab: (tab: 'dashboard' | 'rooms' | 'devices' | 'settings') => void;
  apiConfig: ApiConfig;
  isDiscovering: boolean;
  onRefresh: () => void;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  apiConfig,
  isDiscovering,
  onRefresh,
  theme,
  toggleTheme,
}) => {
  const isHealthy = apiConfig.lastTestStatus === true;

  return (
    <header className="sticky top-0 z-40 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md border-b border-slate-200 dark:border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Status */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-amber-500/15 dark:bg-amber-500/20 text-amber-500 flex items-center justify-center font-bold text-lg shadow-sm">
              💡
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg text-slate-900 dark:text-slate-100">WiZ Home</span>
                <span
                  className={`w-2.5 h-2.5 rounded-full ${
                    isHealthy
                      ? 'bg-emerald-500'
                      : apiConfig.lastTestStatus === false
                      ? 'bg-red-500'
                      : 'bg-slate-400'
                  }`}
                  title={
                    isHealthy
                      ? 'Verbunden'
                      : apiConfig.lastTestStatus === false
                      ? 'Nicht erreichbar'
                      : 'Status unbekannt'
                  }
                />
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {apiConfig.lastSyncTimestamp
                  ? `Sync: ${new Date(apiConfig.lastSyncTimestamp).toLocaleTimeString('de-DE')}`
                  : 'Bereit'}
              </p>
            </div>
          </div>

          {/* Navigation Items (Desktop) */}
          <nav className="hidden md:flex items-center gap-1 bg-slate-100 dark:bg-slate-800/60 p-1 rounded-xl">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'dashboard'
                  ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Home className="w-4 h-4" />
              Übersicht
            </button>
            <button
              onClick={() => setActiveTab('rooms')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'rooms'
                  ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Layers className="w-4 h-4" />
              Räume
            </button>
            <button
              onClick={() => setActiveTab('devices')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'devices'
                  ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Sliders className="w-4 h-4" />
              Geräte
            </button>
            <button
              onClick={() => setActiveTab('settings')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                activeTab === 'settings'
                  ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <Settings className="w-4 h-4" />
              Einstellungen
            </button>
          </nav>

          {/* Action Buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={onRefresh}
              disabled={isDiscovering}
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-50 transition"
              title="Geräte aktualisieren"
            >
              <RefreshCw className={`w-5 h-5 ${isDiscovering ? 'animate-spin text-amber-500' : ''}`} />
            </button>

            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
              title="Farbschema umschalten"
            >
              {theme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Bottom Navigation Bar */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-lg border-t border-slate-200 dark:border-slate-800 py-2 px-4 flex justify-around">
        <button
          onClick={() => setActiveTab('dashboard')}
          className={`flex flex-col items-center gap-1 text-xs font-medium ${
            activeTab === 'dashboard' ? 'text-amber-500' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <Home className="w-5 h-5" />
          Übersicht
        </button>
        <button
          onClick={() => setActiveTab('rooms')}
          className={`flex flex-col items-center gap-1 text-xs font-medium ${
            activeTab === 'rooms' ? 'text-amber-500' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <Layers className="w-5 h-5" />
          Räume
        </button>
        <button
          onClick={() => setActiveTab('devices')}
          className={`flex flex-col items-center gap-1 text-xs font-medium ${
            activeTab === 'devices' ? 'text-amber-500' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <Sliders className="w-5 h-5" />
          Geräte
        </button>
        <button
          onClick={() => setActiveTab('settings')}
          className={`flex flex-col items-center gap-1 text-xs font-medium ${
            activeTab === 'settings' ? 'text-amber-500' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <Settings className="w-5 h-5" />
          Einstellungen
        </button>
      </div>
    </header>
  );
};
