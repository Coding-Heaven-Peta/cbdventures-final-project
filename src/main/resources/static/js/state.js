/**
 * Small shared application state. Kept deliberately simple: the router renders
 * a view from this object and views call `setState` to update the shell.
 */
import { session } from './api.js';

export const state = {
  user: session.user,
  coords: null,
  zone: null,
  filters: null,
  home: null,
  usingMyLocation: false,
  offline: !navigator.onLine
};

const listeners = new Set();

export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function setState(patch) {
  Object.assign(state, patch);
  listeners.forEach((listener) => listener(state));
}

export function setUser(user) {
  setState({ user });
  session.updateUser(user);
}

export function signOut() {
  session.clear();
  setState({ user: null });
}

/** Reference point used for distance calculations. */
export function referencePoint() {
  return state.coords && state.usingMyLocation ? state.coords : null;
}

/** Current hash route split into path segments and query params. */
export function parseRoute(hash = window.location.hash) {
  const raw = hash.replace(/^#/, '') || '/';
  const [pathPart, queryPart] = raw.split('?');
  const segments = pathPart.split('/').filter(Boolean);
  const params = Object.fromEntries(new URLSearchParams(queryPart || ''));
  return { segments, params, path: '/' + segments.join('/') };
}
