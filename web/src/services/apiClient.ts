import { ApiConfig, ApiResponse, WizBulbDto, WizDeviceState } from '../types/wiz';

export class ApiClientError extends Error {
  code?: string;
  httpStatus?: number;

  constructor(message: string, code?: string, httpStatus?: number) {
    super(message);
    this.name = 'ApiClientError';
    this.code = code;
    this.httpStatus = httpStatus;
  }
}

export class ApiClient {
  private config: ApiConfig;

  constructor(config: ApiConfig) {
    this.config = config;
  }

  public updateConfig(config: ApiConfig) {
    this.config = config;
  }

  public normalizeBaseUrl(raw: string): string {
    let trimmed = raw.trim();
    if (!trimmed) {
      trimmed = 'http://192.168.1.100:3000';
    }
    if (!/^https?:\/\//i.test(trimmed)) {
      trimmed = `http://${trimmed}`;
    }
    // Remove trailing slashes
    return trimmed.replace(/\/+$/, '');
  }

  private async request<T = any>(
    path: string,
    options: {
      method?: 'GET' | 'POST';
      body?: any;
      timeoutMs?: number;
    } = {}
  ): Promise<ApiResponse<T>> {
    const { method = 'GET', body, timeoutMs = 10000 } = options;
    const baseUrl = this.normalizeBaseUrl(this.config.baseUrl);
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    const url = `${baseUrl}${cleanPath}`;

    const headers: Record<string, string> = {
      Accept: 'application/json',
    };

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }

    if (this.config.apiToken && this.config.apiToken.trim().length > 0) {
      headers['Authorization'] = `Bearer ${this.config.apiToken.trim()}`;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(url, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      let json: any = null;
      try {
        json = await response.json();
      } catch {
        // Not valid JSON
      }

      if (!response.ok) {
        let errorMsg = this.translateHttpStatus(response.status);
        if (json && json.error && json.error.message) {
          errorMsg = `${errorMsg} (${json.error.message})`;
        }
        throw new ApiClientError(errorMsg, json?.error?.code, response.status);
      }

      if (json && typeof json === 'object') {
        if (json.success === false && json.error) {
          throw new ApiClientError(
            json.error.message || 'API-Anfrage fehlgeschlagen',
            json.error.code,
            response.status
          );
        }
        return json as ApiResponse<T>;
      }

      return { success: true, data: json } as ApiResponse<T>;
    } catch (err: any) {
      clearTimeout(timeoutId);
      if (err instanceof ApiClientError) {
        throw err;
      }
      if (err.name === 'AbortError') {
        throw new ApiClientError(
          'Zeitüberschreitung der Netzwerkanfrage (Timeout). Der WiZ-Server antwortet nicht rechtzeitig.',
          'TIMEOUT'
        );
      }
      throw new ApiClientError(
        `Verbindungsfehler: Keine Verbindung zu ${baseUrl}. Bitte Netzwerk und IP-Adresse prüfen.`,
        'NETWORK_ERROR'
      );
    }
  }

  private translateHttpStatus(status: number): string {
    switch (status) {
      case 401:
        return 'Authentifizierung fehlgeschlagen: Ungültiges oder abgelaufenes API-Token (HTTP 401).';
      case 403:
        return 'Zugriff verweigert: Fehlende Berechtigung für diesen Endpunkt (HTTP 403).';
      case 404:
        return 'API-Endpunkt nicht gefunden: Bitte URL und Server prüfen (HTTP 404).';
      case 502:
        return 'Geräte-Timeout: Das WiZ-Gerät hat nicht rechtzeitig geantwortet (HTTP 502 Bad Gateway).';
      case 504:
        return 'Gateway-Timeout: Zeitüberschreitung bei der Kommunikation mit dem WiZ-Gerät (HTTP 504).';
      default:
        if (status >= 500) {
          return `Interner Serverfehler der REST-API (HTTP ${status}).`;
        }
        return `HTTP-Anfrage fehlgeschlagen mit Status ${status}.`;
    }
  }

  public async checkHealth(): Promise<string> {
    try {
      const res = await this.request<any>('/api/health');
      if (res.data && res.data.status) {
        return `Server erreichbar (${res.data.status})`;
      }
      return 'Server erreichbar (HTTP 200 OK)';
    } catch {
      // Fallback check root
      const rootRes = await this.request<any>('/');
      return 'Server erreichbar (Root-Endpunkt online)';
    }
  }

  public async discover(): Promise<WizBulbDto[]> {
    const res = await this.request<{ count?: number; bulbs?: WizBulbDto[] }>('/api/discover');
    if (res.data && Array.isArray(res.data.bulbs)) {
      return res.data.bulbs;
    }
    if (Array.isArray(res.data)) {
      return res.data;
    }
    return [];
  }

  public async getBulbs(): Promise<WizBulbDto[]> {
    const res = await this.request<any>('/api/bulbs');
    if (res.data && Array.isArray(res.data.bulbs)) {
      return res.data.bulbs;
    }
    if (Array.isArray(res.data)) {
      return res.data;
    }
    return [];
  }

  public async getState(ip: string): Promise<WizDeviceState | null> {
    const res = await this.request<any>(`/api/state/${encodeURIComponent(ip)}`);
    const data = res.data;
    if (data?.state && typeof data.state === 'object') {
      return data.state;
    }
    return data || null;
  }

  public async turnOn(ip: string): Promise<void> {
    await this.request('/api/on', { method: 'POST', body: { ip } });
  }

  public async turnOff(ip: string): Promise<void> {
    await this.request('/api/off', { method: 'POST', body: { ip } });
  }

  public async toggle(ip: string): Promise<void> {
    await this.request('/api/toggle', { method: 'POST', body: { ip } });
  }

  public async setBrightness(ip: string, brightness: number): Promise<void> {
    const clamped = Math.max(0, Math.min(100, Math.round(brightness)));
    await this.request('/api/brightness', {
      method: 'POST',
      body: { ip, brightness: clamped },
    });
  }

  public async setColor(ip: string, r: number, g: number, b: number): Promise<void> {
    await this.request('/api/color', {
      method: 'POST',
      body: {
        ip,
        r: Math.max(0, Math.min(255, Math.round(r))),
        g: Math.max(0, Math.min(255, Math.round(g))),
        b: Math.max(0, Math.min(255, Math.round(b))),
      },
    });
  }

  public async broadcastOn(): Promise<void> {
    await this.request('/api/broadcast/on', { method: 'POST' });
  }

  public async broadcastOff(): Promise<void> {
    await this.request('/api/broadcast/off', { method: 'POST' });
  }
}
