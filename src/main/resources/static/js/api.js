/**
 * CBDVentures API client.
 *
 * Thin wrapper around fetch that:
 *  - attaches the JWT bearer token when the visitor is signed in;
 *  - converts backend error payloads into friendly Error objects;
 *  - keeps a read-through cache in localStorage so previously viewed listings
 *    stay visible when the connection briefly drops (NFR: Reliability / FR-02).
 */

const TOKEN_KEY = 'cbd.token';
const USER_KEY = 'cbd.user';
const CACHE_PREFIX = 'cbd.cache.';
const CACHE_TTL_MS = 1000 * 60 * 60 * 12;

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

/* ------------------------------------------------------------------ session */

export const session = {
  get token() {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  },
  get user() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
  },
  save(auth) {
    localStorage.setItem(TOKEN_KEY, auth.token);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
    return auth.user;
  },
  updateUser(user) {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  get isSignedIn() {
    return Boolean(this.token && this.user);
  },
  get isOwner() {
    return this.user?.role === 'OWNER' || this.user?.role === 'ADMIN';
  },
  get isAdmin() {
    return this.user?.role === 'ADMIN';
  }
};

/* -------------------------------------------------------------------- cache */

function cacheKey(path) {
  return CACHE_PREFIX + path;
}

function readCache(path) {
  try {
    const raw = localStorage.getItem(cacheKey(path));
    if (!raw) return null;
    const entry = JSON.parse(raw);
    if (Date.now() - entry.at > CACHE_TTL_MS) return null;
    return entry.data;
  } catch {
    return null;
  }
}

function writeCache(path, data) {
  try {
    localStorage.setItem(cacheKey(path), JSON.stringify({ at: Date.now(), data }));
  } catch {
    /* storage full or disabled - caching is best effort */
  }
}

export function clearCache() {
  try {
    Object.keys(localStorage)
      .filter((key) => key.startsWith(CACHE_PREFIX))
      .forEach((key) => localStorage.removeItem(key));
  } catch { /* ignore */ }
}

/* -------------------------------------------------------------------- fetch */

async function request(path, { method = 'GET', body, signal, raw = false, cache = method === 'GET' } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = session.token;
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      // `raw` sends the body verbatim (used when re-uploading a backup file).
      body: body === undefined ? undefined : (raw ? body : JSON.stringify(body)),
      signal
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    const cached = cache ? readCache(path) : null;
    if (cached) {
      return { data: cached, fromCache: true };
    }
    throw new ApiError('We could not reach the CBDVentures server. Check your connection and try again.', 0, null);
  }

  if (response.status === 204) return { data: null, fromCache: false };

  let payload = null;
  const text = await response.text();
  if (text) {
    try { payload = JSON.parse(text); } catch { payload = null; }
  }

  if (!response.ok) {
    if (response.status === 401) session.clear();
    const message = payload?.message || `Request failed (${response.status}).`;
    throw new ApiError(message, response.status, payload);
  }

  if (cache) writeCache(path, payload);
  return { data: payload, fromCache: false };
}

/**
 * Fetch a non-JSON endpoint (CSV export) with the bearer token attached and
 * return the body as text plus the suggested file name.
 */
async function requestText(path) {
  const headers = { Accept: 'text/csv' };
  const token = session.token;
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`/api${path}`, { headers });
  if (!response.ok) {
    let message = `Request failed (${response.status}).`;
    try {
      const payload = await response.json();
      if (payload?.message) message = payload.message;
    } catch { /* not JSON */ }
    if (response.status === 401) session.clear();
    throw new ApiError(message, response.status, null);
  }
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = /filename="?([^"]+)"?/.exec(disposition);
  return { text: await response.text(), fileName: match ? match[1] : 'report.csv' };
}

/* ------------------------------------------------------------------ helpers */

function query(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '' ) return;
    if (Array.isArray(value)) {
      value.filter((v) => v !== undefined && v !== null && v !== '').forEach((v) => search.append(key, v));
    } else {
      search.set(key, value);
    }
  });
  const string = search.toString();
  return string ? `?${string}` : '';
}

export const api = {
  /* auth */
  register: (payload) => request('/auth/register', { method: 'POST', body: payload }),
  verify: (payload) => request('/auth/verify', { method: 'POST', body: payload, cache: false }),
  resend: (payload) => request('/auth/resend', { method: 'POST', body: payload, cache: false }),
  login: (payload) => request('/auth/login', { method: 'POST', body: payload }),
  oauthProviders: () => request('/auth/providers', { cache: false }),
  exchangeOAuth: () => request('/auth/oauth/exchange', { method: 'POST', cache: false }),
  me: () => request('/auth/me', { cache: false }),
  changePassword: (payload) => request('/auth/password', { method: 'POST', body: payload }),

  /* discovery */
  home: (coords) => request(`/discover/home${query({ lat: coords?.lat, lng: coords?.lng })}`),
  businesses: (params) => request(`/businesses${query(params)}`),
  business: (id) => request(`/businesses/${id}`, { cache: false }),
  mapPins: () => request('/businesses/map'),
  reviews: (id) => request(`/businesses/${id}/reviews`),
  addReview: (id, payload) => request(`/businesses/${id}/reviews`, { method: 'POST', body: payload }),
  deleteReview: (reviewId) => request(`/businesses/reviews/${reviewId}`, { method: 'DELETE' }),

  /* reference data */
  categories: () => request('/categories'),
  amenities: () => request('/amenities'),
  filters: () => request('/meta/filters'),
  zone: () => request('/meta/zone'),

  /* events */
  events: (params = {}) => request(`/events${query(params)}`),

  /* personalisation */
  preferences: () => request('/preferences', { cache: false }),
  savePreferences: (payload) => request('/preferences', { method: 'PUT', body: payload }),
  recommendations: (coords, limit) =>
    request(`/recommendations${query({ lat: coords?.lat, lng: coords?.lng, limit })}`),

  /* owner */
  myListings: () => request('/owner/businesses', { cache: false }),
  createListing: (payload) => request('/owner/businesses', { method: 'POST', body: payload }),
  updateListing: (id, payload) => request(`/owner/businesses/${id}`, { method: 'PUT', body: payload }),
  deleteListing: (id) => request(`/owner/businesses/${id}`, { method: 'DELETE' }),
  listingEvents: (id) => request(`/owner/businesses/${id}/events`, { cache: false }),
  createEvent: (id, payload) => request(`/owner/businesses/${id}/events`, { method: 'POST', body: payload }),
  deleteEvent: (eventId) => request(`/owner/events/${eventId}`, { method: 'DELETE' }),

  /* admin */
  adminListings: (status) => request(`/admin/businesses${query({ status })}`, { cache: false }),
  approve: (id, note) => request(`/admin/businesses/${id}/approve`, { method: 'POST', body: { note } }),
  reject: (id, note) => request(`/admin/businesses/${id}/reject`, { method: 'POST', body: { note } }),
  adminStats: () => request('/admin/stats', { cache: false }),

  /* reports (administrator) */
  reports: () => request('/admin/reports', { cache: false }),
  report: (key) => request(`/admin/reports/${key}`, { cache: false }),
  reportCsv: (key) => requestText(`/admin/reports/${key}/csv`),

  /* student assistant (public) */
  assistantConfig: () => request('/assistant/config'),
  assistantPlaces: (params = {}) => request(`/assistant/places${query(params)}`),
  assistantPlace: (id) => request(`/assistant/places/${encodeURIComponent(id)}`),
  assistantChat: (messages) =>
    request('/assistant/chat', { method: 'POST', body: { messages }, cache: false }),

  /* system backup (administrator) */
  backupSummary: () => request('/admin/backup/summary', { cache: false }),
  downloadBackup: async () => {
    const headers = { Accept: 'application/json' };
    const token = session.token;
    if (token) headers.Authorization = `Bearer ${token}`;
    const response = await fetch('/api/admin/backup', { headers });
    if (!response.ok) {
      let message = `Backup failed (${response.status}).`;
      try {
        const payload = await response.json();
        if (payload?.message) message = payload.message;
      } catch { /* not JSON */ }
      throw new ApiError(message, response.status, null);
    }
    const disposition = response.headers.get('Content-Disposition') || '';
    const match = /filename="?([^"]+)"?/.exec(disposition);
    return {
      data: await response.json(),
      fileName: match ? match[1] : 'cbdventures-backup.json'
    };
  },
  restoreBackup: (json) => request('/admin/backup/restore', { method: 'POST', body: json, raw: true, cache: false })
};

/* -------------------------------------------------------------- geolocation */

export function currentPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Your browser does not support location services.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => resolve({ lat: position.coords.latitude, lng: position.coords.longitude }),
      () => reject(new Error('Location permission was declined. Showing distances from the Richfield campus.')),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
    );
  });
}
