import { doc, getDoc } from 'firebase/firestore';
import type { Expedition, SessionReward } from '../domain/focus';
import { firebaseDb, firebaseEnabled, getFirebaseIdToken } from './firebase';

const apiBaseUrl = import.meta.env.VITE_FOCUS_RAID_API_BASE_URL?.replace(/\/$/, '') ?? '';

export const remoteBackendEnabled = firebaseEnabled && Boolean(apiBaseUrl);

export type FocusMode = 'expedition' | 'raid';

export type RemoteWorldSnapshot = {
  towerFloor?: number;
  towerProgress?: number;
  abyssDepth?: number;
  abyssProgress?: number;
  armoryReady?: number;
  focusNowEstimate?: number;
  currentRaidId?: string | null;
  updatedAt?: unknown;
};

export type StartFocusPayload = {
  plannedMinutes: number;
  destination: Expedition;
  mode: FocusMode;
  goalId?: string | null;
  raidId?: string | null;
};

export type StartFocusResponse = {
  sessionId: string;
  startedAt: string;
  plannedMinutes: number;
};

export type FinishFocusResponse = SessionReward & {
  sessionId: string;
  totalFocusMinutes: number;
  armoryReady?: number;
};

async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (!remoteBackendEnabled) throw new Error('Remote Focus Raid backend is not configured.');

  const token = await getFirebaseIdToken();
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      'content-type': 'application/json',
      authorization: `Bearer ${token}`,
      ...init.headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Focus Raid API ${response.status}: ${text || response.statusText}`);
  }

  return response.json() as Promise<T>;
}

export function startRemoteFocus(payload: StartFocusPayload): Promise<StartFocusResponse> {
  return apiFetch('/focus/start', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function finishRemoteFocus(sessionId: string): Promise<FinishFocusResponse> {
  return apiFetch('/focus/finish', {
    method: 'POST',
    body: JSON.stringify({ sessionId }),
  });
}

export async function loadRemoteWorld(): Promise<RemoteWorldSnapshot | null> {
  if (!firebaseDb) return null;
  const snapshot = await getDoc(doc(firebaseDb, 'world', 'current'));
  return snapshot.exists() ? snapshot.data() as RemoteWorldSnapshot : null;
}
