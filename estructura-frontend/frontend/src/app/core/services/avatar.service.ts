import { Injectable, signal } from '@angular/core';

export interface AvatarConfig {
  figura: string;
  accesorio?: string;
}

@Injectable({ providedIn: 'root' })
export class AvatarService {
  private readonly storageKey = 'lukaapp.avatar.config';

  readonly avatarConfig = signal<AvatarConfig>({
    figura: 'GATO ANDINO',
    accesorio: 'LENTES',
  });

  setAvatar(config: AvatarConfig): void {
    this.avatarConfig.set(config);
    localStorage.setItem(this.storageKey, JSON.stringify(config));
  }

  getAvatar(): AvatarConfig {
    return this.avatarConfig();
  }

  loadAvatar(): void {
    const rawConfig = localStorage.getItem(this.storageKey);
    if (!rawConfig) {
      return;
    }

    try {
      const parsed = JSON.parse(rawConfig) as AvatarConfig;
      if (parsed?.figura) {
        this.avatarConfig.set(parsed);
      }
    } catch {
      localStorage.removeItem(this.storageKey);
    }
  }
}

