export interface WizDeviceState {
  state: boolean;
  sceneId?: number | null;
  temp?: number | null;
  dimming?: number | null;
  r?: number | null;
  g?: number | null;
  b?: number | null;
}

export interface WizBulbDto {
  ip: string;
  mac?: string | null;
  success?: boolean | null;
  roomId?: number | null;
  roomName?: string | null;
  deviceName?: string | null;
  deviceType?: string | null;
  moduleName?: string | null;
  firmwareVersion?: string | null;
  state?: WizDeviceState | null;
  rssi?: number | null;
}

export interface WizDevice {
  ip: string;
  mac: string;
  roomId: number | null;
  roomName: string;
  deviceName: string;
  deviceType: string;
  moduleName?: string | null;
  firmwareVersion?: string | null;
  state: WizDeviceState;
  rssi?: number | null;
  isOnline: boolean;
  lastSeenTimestamp: number;
}

export interface WizRoom {
  id: number;
  name: string;
  devices: WizDevice[];
  totalCount: number;
  activeCount: number;
  onlineCount: number;
  averageBrightness: number;
}

export interface ApiConfig {
  baseUrl: string;
  apiToken: string;
  autoRefresh: boolean;
  refreshIntervalSeconds: number;
  themeMode: 'system' | 'light' | 'dark';
  lastTestStatus?: boolean | null;
  lastTestMessage?: string | null;
  lastTestTimestamp?: number | null;
  lastSyncTimestamp?: number | null;
}

export interface ApiResponse<T = any> {
  success: boolean;
  timestamp?: number;
  data?: T | null;
  error?: {
    code?: string;
    message?: string;
  } | null;
}
