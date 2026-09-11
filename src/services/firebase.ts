import { getApp, getApps, initializeApp } from 'firebase/app';
import { getAuth, signInAnonymously, type Auth, type User } from 'firebase/auth';
import { getFirestore, type Firestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

export const firebaseEnabled = Boolean(
  firebaseConfig.apiKey
  && firebaseConfig.authDomain
  && firebaseConfig.projectId
  && firebaseConfig.appId,
);

const firebaseApp = firebaseEnabled
  ? (getApps().length ? getApp() : initializeApp(firebaseConfig))
  : null;

export const firebaseAuth: Auth | null = firebaseApp ? getAuth(firebaseApp) : null;
export const firebaseDb: Firestore | null = firebaseApp ? getFirestore(firebaseApp) : null;

let anonymousSignIn: Promise<User> | null = null;

export async function ensureAnonymousUser(): Promise<User> {
  if (!firebaseAuth) throw new Error('Firebase is not configured.');
  if (firebaseAuth.currentUser) return firebaseAuth.currentUser;

  anonymousSignIn ??= signInAnonymously(firebaseAuth)
    .then((credential) => credential.user)
    .finally(() => {
      anonymousSignIn = null;
    });

  return anonymousSignIn;
}

export async function getFirebaseIdToken(): Promise<string> {
  const user = await ensureAnonymousUser();
  return user.getIdToken();
}
