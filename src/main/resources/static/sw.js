/**
 * CBDVentures service worker.
 * Strategy:
 *  - app shell (HTML, CSS, JS, artwork): cache-first so the interface opens offline;
 *  - API GETs: network-first with a cache fallback, so listings you have already
 *    seen stay readable when the connection drops (FR-02 / NFR reliability).
 */
const SHELL_CACHE = 'cbdventures-shell-v5';
const DATA_CACHE = 'cbdventures-data-v5';

const SHELL_ASSETS = [
  '/',
  '/index.html',
  '/css/app.css',
  '/js/app.js',
  '/js/api.js',
  '/js/ui.js',
  '/js/map-widget.js',
  '/js/state.js',
  '/js/views/discover.js',
  '/js/views/explore.js',
  '/js/views/business.js',
  '/js/views/map.js',
  '/js/views/assistant.js',
  '/js/views/events.js',
  '/js/views/profile.js',
  '/js/views/owner.js',
  '/js/views/admin.js',
  '/js/views/reports.js',
  '/js/views/backup.js',
  '/js/views/auth.js',
  '/js/views/welcome.js',
  '/images/logo-dark.svg',
  '/images/hero.svg',
  '/images/hero-welcome.svg',
  '/images/CBD.jpg',
  '/images/places/asian-delight-shawarma.jpg',
  '/images/places/es-kia-mphahlele-library.jpg',
  '/images/places/four-four-two.jpg',
  '/images/places/honchos.jpg',
  '/images/places/magwinya-zone.jpg',
  '/images/places/phola-shisa-nyama.jpg',
  '/images/places/quick-bake.jpg',
  '/images/places/rebels-pizza.jpg',
  '/images/places/thuba-rush-kj.jpg',
  '/images/places/top-class-fast-foods.jpg',
  '/images/favicon.svg',
  '/images/icons/app-icon.svg',
  '/manifest.webmanifest'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(SHELL_CACHE)
      .then((cache) => cache.addAll(SHELL_ASSETS))
      .then(() => self.skipWaiting())
      .catch(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys
        .filter((key) => key !== SHELL_CACHE && key !== DATA_CACHE)
        .map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // Connection probes must always reach the network, never the cache.
  if (url.searchParams.has('__probe')) return;

  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(DATA_CACHE).then((cache) => cache.put(request, clone));
          }
          return response;
        })
        .catch(() => caches.match(request).then((cached) => cached
          || new Response(JSON.stringify({
            message: 'You are offline and this listing has not been saved yet.'
          }), { status: 503, headers: { 'Content-Type': 'application/json' } })))
    );
    return;
  }

  // Stale-while-revalidate: instant offline start, but a new deploy is picked
  // up on the next load instead of being pinned in the cache forever.
  event.respondWith(
    caches.match(request).then((cached) => {
      const network = fetch(request)
        .then((response) => {
          if (response.ok && request.destination !== '') {
            const clone = response.clone();
            caches.open(SHELL_CACHE).then((cache) => cache.put(request, clone));
          }
          return response;
        })
        .catch(() => cached || caches.match('/index.html'));
      return cached || network;
    })
  );
});
