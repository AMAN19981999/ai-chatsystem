export type RuntimePlatform = 'web' | 'desktop';

// Desktop-ready boundary: Electron or Tauri adapters can replace this without touching UI code.
export const runtimePlatform: RuntimePlatform = window.navigator.userAgent.includes('Electron')
  ? 'desktop'
  : 'web';
