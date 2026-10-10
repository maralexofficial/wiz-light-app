import React, { useState } from 'react';
import {
  Server,
  Key,
  CheckCircle,
  AlertCircle,
  Eye,
  EyeOff,
  RefreshCw,
  Trash2,
  ShieldCheck,
  AlertTriangle,
} from 'lucide-react';
import { ApiConfig } from '../types/wiz';

interface SettingsPageProps {
  apiConfig: ApiConfig;
  isTesting: boolean;
  isDiscovering: boolean;
  onTestConnection: () => void;
  onSaveConfig: (config: ApiConfig) => void;
  onRefreshDiscovery: () => void;
  onResetData: () => void;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({
  apiConfig,
  isTesting,
  isDiscovering,
  onTestConnection,
  onSaveConfig,
  onRefreshDiscovery,
  onResetData,
}) => {
  const [baseUrl, setBaseUrl] = useState(apiConfig.baseUrl);
  const [apiToken, setApiToken] = useState(apiConfig.apiToken);
  const [showToken, setShowToken] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(apiConfig.autoRefresh);
  const [refreshInterval, setRefreshInterval] = useState(apiConfig.refreshIntervalSeconds);
  const [themeMode, setThemeMode] = useState(apiConfig.themeMode);
  const [showConfirmReset, setShowConfirmReset] = useState(false);

  const isHttpUnencrypted = baseUrl.startsWith('http://');
  const isLocalIp =
    baseUrl.includes('192.168.') ||
    baseUrl.includes('10.') ||
    baseUrl.includes('localhost') ||
    baseUrl.includes('127.0.0.1');

  const handleSave = () => {
    onSaveConfig({
      ...apiConfig,
      baseUrl: baseUrl.trim(),
      apiToken: apiToken.trim(),
      autoRefresh,
      refreshIntervalSeconds: refreshInterval,
      themeMode,
    });
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in pb-24 md:pb-8">
      <div>
        <h1 className="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
          Einstellungen
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          Konfiguration der REST-API und App-Präferenzen
        </p>
      </div>

      {/* Connection Status Card */}
      <div className="bg-white dark:bg-slate-900 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
        <div className="flex items-center gap-2">
          {apiConfig.lastTestStatus === true ? (
            <CheckCircle className="w-5 h-5 text-emerald-500" />
          ) : apiConfig.lastTestStatus === false ? (
            <AlertCircle className="w-5 h-5 text-red-500" />
          ) : (
            <Server className="w-5 h-5 text-slate-400" />
          )}
          <h3 className="font-semibold text-slate-900 dark:text-slate-100 text-sm">
            {apiConfig.lastTestStatus === true
              ? 'Verbindung hergestellt'
              : apiConfig.lastTestStatus === false
              ? 'Verbindung fehlgeschlagen'
              : 'Verbindung ungetestet'}
          </h3>
        </div>

        {apiConfig.lastTestMessage && (
          <p className="text-xs text-slate-600 dark:text-slate-400 pl-7">
            {apiConfig.lastTestMessage}
          </p>
        )}

        <div className="text-xs text-slate-400 dark:text-slate-500 pl-7 space-y-0.5">
          {apiConfig.lastTestTimestamp && (
            <p>
              Letzter Test:{' '}
              {new Date(apiConfig.lastTestTimestamp).toLocaleString('de-DE')}
            </p>
          )}
          {apiConfig.lastSyncTimestamp && (
            <p>
              Letzter Abgleich:{' '}
              {new Date(apiConfig.lastSyncTimestamp).toLocaleString('de-DE')}
            </p>
          )}
        </div>
      </div>

      {/* API Configuration Card */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-5">
        <div>
          <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">
            REST-API Server
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Adresse und Zugangsdaten deines Node.js WiZ Backends
          </p>
        </div>

        {/* Base URL */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
            API-Basis-URL
          </label>
          <div className="relative">
            <Server className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="http://192.168.1.100:3000"
              className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
          </div>
          {isHttpUnencrypted && !isLocalIp && (
            <div className="flex items-center gap-1.5 text-xs text-amber-600 dark:text-amber-400 mt-1">
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>Unverschlüsseltes HTTP außerhalb des Heimnetzwerks (LAN) ist unsicher.</span>
            </div>
          )}
        </div>

        {/* API Token */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
            API-Token (Optional)
          </label>
          <div className="relative">
            <Key className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type={showToken ? 'text' : 'password'}
              value={apiToken}
              onChange={(e) => setApiToken(e.target.value)}
              placeholder="Bearer Token falls aktiviert"
              className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
            <button
              type="button"
              onClick={() => setShowToken(!showToken)}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              {showToken ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={onTestConnection}
            disabled={isTesting}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-sm font-medium transition disabled:opacity-50"
          >
            {isTesting && <RefreshCw className="w-4 h-4 animate-spin" />}
            {isTesting ? 'Prüfe...' : 'Verbindung testen'}
          </button>
          <button
            type="button"
            onClick={handleSave}
            className="flex-1 py-2.5 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-sm font-semibold transition shadow-sm"
          >
            Speichern
          </button>
        </div>
      </div>

      {/* Auto Refresh Card */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-slate-100">
              Automatische Aktualisierung
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Zustände regelmäßig im Hintergrund synchronisieren
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              const val = !autoRefresh;
              setAutoRefresh(val);
              onSaveConfig({ ...apiConfig, autoRefresh: val });
            }}
            className={`w-12 h-7 rounded-full transition-colors relative flex items-center p-1 ${
              autoRefresh ? 'bg-amber-500' : 'bg-slate-300 dark:bg-slate-700'
            }`}
          >
            <div
              className={`w-5 h-5 rounded-full bg-white transition-transform ${
                autoRefresh ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {autoRefresh && (
          <div className="pt-2">
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block mb-2">
              Intervall
            </label>
            <div className="flex gap-2">
              {[
                { sec: 15, label: '15s' },
                { sec: 30, label: '30s' },
                { sec: 60, label: '60s' },
                { sec: 120, label: '2 Min' },
              ].map((opt) => (
                <button
                  key={opt.sec}
                  type="button"
                  onClick={() => {
                    setRefreshInterval(opt.sec);
                    onSaveConfig({ ...apiConfig, refreshIntervalSeconds: opt.sec });
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                    refreshInterval === opt.sec
                      ? 'bg-amber-500 text-white shadow-sm'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Theme Card */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-3">
        <h2 className="text-base font-bold text-slate-900 dark:text-slate-100">
          Erscheinungsbild
        </h2>
        <div className="flex gap-2">
          {[
            { id: 'system', label: 'System' },
            { id: 'light', label: 'Hell' },
            { id: 'dark', label: 'Dunkel' },
          ].map((mode) => (
            <button
              key={mode.id}
              type="button"
              onClick={() => {
                const m = mode.id as any;
                setThemeMode(m);
                onSaveConfig({ ...apiConfig, themeMode: m });
              }}
              className={`px-3.5 py-2 rounded-xl text-xs font-semibold transition ${
                themeMode === mode.id
                  ? 'bg-amber-500 text-white shadow-sm'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
              }`}
            >
              {mode.label}
            </button>
          ))}
        </div>
      </div>

      {/* Maintenance & Reset */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-3">
        <h2 className="text-base font-bold text-slate-900 dark:text-slate-100">
          Geräte & Wartung
        </h2>

        <div className="flex flex-col sm:flex-row gap-3 pt-1">
          <button
            type="button"
            onClick={onRefreshDiscovery}
            disabled={isDiscovering}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-sm font-semibold transition"
          >
            <RefreshCw className={`w-4 h-4 ${isDiscovering ? 'animate-spin' : ''}`} />
            {isDiscovering ? 'Suche läuft...' : 'Geräte neu erkennen'}
          </button>
          <button
            type="button"
            onClick={() => setShowConfirmReset(true)}
            className="flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl border border-red-300 dark:border-red-900 text-red-600 dark:text-red-400 text-sm font-medium hover:bg-red-50 dark:hover:bg-red-950/20 transition"
          >
            <Trash2 className="w-4 h-4" />
            Zurücksetzen
          </button>
        </div>
      </div>

      {/* Security Note */}
      <div className="p-4 rounded-2xl bg-slate-100/60 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
        <p className="text-xs text-slate-500 dark:text-slate-400">
          <strong>Datensicherheit:</strong> Deine API-Zugangsdaten werden ausschließlich im
          lokalen Speicher (Browser LocalStorage) abgelegt und niemals an externe Server gesendet.
        </p>
      </div>

      {/* Reset Confirmation Dialog */}
      {showConfirmReset && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 dark:border-slate-800">
            <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
              Konfiguration löschen?
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
              Alle gespeicherten Serverdaten, Tokens und zwischengespeicherten Geräte werden
              unwiderruflich gelöscht.
            </p>
            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowConfirmReset(false)}
                className="flex-1 py-2 px-4 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-sm font-medium"
              >
                Abbrechen
              </button>
              <button
                onClick={() => {
                  setShowConfirmReset(false);
                  onResetData();
                }}
                className="flex-1 py-2 px-4 rounded-xl bg-red-600 text-white text-sm font-semibold"
              >
                Löschen
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
