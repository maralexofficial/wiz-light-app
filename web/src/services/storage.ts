import { ApiConfig, WizDevice } from '../types/wiz';

const CONFIG_KEY = 'wiz_home_api_config';
const DEVICES_KEY = 'wiz_home_cached_devices';

export const DEFAULT_CONFIG: ApiConfig = {
  baseUrl: 'http://192.168.1.100:3000',
  apiToken: '',
  autoRefresh: false,
  refreshIntervalSeconds: 30,
  themeMode: 'system',
  lastTestStatus: null,
  lastTestMessage: null,
  lastTestTimestamp: null,
  lastSyncTimestamp: null,
};

export function loadApiConfig(): ApiConfig {
  try {
    const raw = localStorage.getItem(CONFIG_KEY);
    if (!raw) return DEFAULT_CONFIG;
    return { ...DEFAULT_CONFIG, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_CONFIG;
  }
}

export function saveApiConfig(config: ApiConfig): void {
  try {
    localStorage.setItem(CONFIG_KEY, JSON.stringify(config));
  } catch (e) {
    console.error('Failed to save config to localStorage', e);
  }
}

export function loadCachedDevices(): WizDevice[] {
  try {
    const raw = localStorage.getItem(DEVICES_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

export function saveCachedDevices(devices: WizDevice[]): void {
  try {
    localStorage.setItem(DEVICES_KEY, JSON.stringify(devices));
  } catch (e) {
    console.error('Failed to save devices to localStorage', e);
  }
}

export function clearLocalStorage(): void {
  try {
    localStorage.removeItem(CONFIG_KEY);
    localStorage.removeItem(DEVICES_KEY);
  } catch (e) {
    console.error('Failed to clear storage', e);
  }
}
