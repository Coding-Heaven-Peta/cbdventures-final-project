/**
 * CBDVentures — Dark edition router.
 *
 * Hash routing keeps the whole app servable from Spring Boot on one port with
 * no build step. The shell has three modes:
 *   • chromeless — welcome and the auth screens (no top bar, no tab bar);
 *   • tabbed      — the five consumer screens;
 *   • portal      — owner, admin, reports and backup, reached from Profile.
 */
import { api, session } from './api.js';
import { hydrateIcons, icon, toast, closeModal } from './ui.js';
import { parseRoute, setState, state, signOut } from './state.js';
import { renderWelcome } from './views/welcome.js';
import { renderSignIn, renderSignUp, renderVerify } from './views/auth.js';
import { renderDiscover } from './views/discover.js';
import { renderExplore } from './views/explore.js';
import { renderBusiness } from './views/business.js';
import { renderMap } from './views/map.js';
import { renderAssistant } from './views/assistant.js';
import { renderEvents } from './views/events.js';
import { renderProfile } from './views/profile.js';
import { renderOwner } from './views/owner.js';
import { renderAdmin } from './views/admin.js';
import { renderReports } from './views/reports.js';
import { renderBackup } from './views/backup.js';

const main = document.getElementById('main');
const topbarAvatar = document.getElementById('topbar-avatar');
const SEEN_KEY = 'cbd.welcomeSeen';

let renderToken = 0;

/* ------------------------------------------------------------------- routing */

const CHROMELESS = new Set(['welcome', 'signin', 'signup', 'verify', 'oauth-complete']);
const TAB_OF = {
  '': 'discover', explore: 'explore', business: 'explore',
  map: 'map', assistant: 'assistant', events: 'events', profile: 'profile', owner: 'profile',
  admin: 'profile', reports: 'profile', backup: 'profile'
};

async function router() {
  const token = ++renderToken;
  const { segments, params } = parseRoute();
  const [first, second] = segments;
  const route = first || '';

  // First visit: introduce the app before dropping the visitor into the feed.
  if (!first && !session.isSignedIn && !localStorage.getItem(SEEN_KEY)
      && !window.location.hash.replace('#', '').startsWith('/sign')) {
    window.location.replace('#/welcome');
    return;
  }

  // Leaving a screen must dismiss anything it opened over the app.
  closeModal();
  window.scrollTo({ top: 0, behavior: 'instant' in window ? 'instant' : 'auto' });
  applyChrome(route);
  setActiveTab(TAB_OF[route] || 'discover');
  paintAvatar();

  try {
    switch (route) {
      case 'welcome': await renderWelcome(main); break;
      case 'signin': await renderSignIn(main, params); break;
      case 'signup': await renderSignUp(main, params); break;
      case 'verify': await renderVerify(main, params); break;
      case 'oauth-complete': await completeOAuthLogin(main); break;

      case '':
      case 'discover': await renderDiscover(main, params); break;
      case 'explore': await renderExplore(main, params); break;
      case 'business': await renderBusiness(main, second); break;
      case 'map': await renderMap(main, params); break;
      case 'assistant': await renderAssistant(main); break;
      case 'events': await renderEvents(main, params); break;

      case 'profile':
        if (!requireSignIn()) return;
        await renderProfile(main); break;

      case 'owner':
        if (!requireSignIn()) return;
        if (!session.isOwner) return deny();
        await renderOwner(main); break;

      case 'admin':
        if (!requireSignIn()) return;
        if (!session.isAdmin) return deny();
        await renderAdmin(main); break;

      case 'reports':
        if (!requireSignIn()) return;
        if (!session.isAdmin) return deny();
        await renderReports(main); break;

      case 'backup':
        if (!requireSignIn()) return;
        if (!session.isAdmin) return deny();
        await renderBackup(main); break;

      default: renderNotFound(main);
    }
  } catch (error) {
    if (token !== renderToken) return;
    if (error.status === 401) {
      session.clear();
      toast('Your session ended. Please sign in again.', 'error');
      window.location.hash = '#/signin';
      return;
    }
    main.innerHTML = `<div class="card" style="margin-top:26px;display:grid;gap:12px;justify-items:center;text-align:center">
      <span class="empty__icon">${icon('alert', 24)}</span>
      <h2>Something went wrong</h2>
      <p class="muted small">${error.message}</p>
      <a class="btn btn--sm" href="#/">Back to discover</a>
    </div>`;
  }

  hydrateIcons(main);
}

/* --------------------------------------------------------------------- chrome */

function applyChrome(route) {
  document.body.classList.toggle('is-chromeless', CHROMELESS.has(route));
}

function setActiveTab(tab) {
  document.querySelectorAll('[data-tab]').forEach((link) => {
    link.classList.toggle('is-active', link.dataset.tab === tab);
  });
}

function paintAvatar() {
  const user = session.user;
  if (!topbarAvatar) return;
  topbarAvatar.classList.add('avatar', 'avatar--sm');
  topbarAvatar.innerHTML = user
    ? `<span>${(user.initials || user.fullName || 'U').slice(0, 2).toUpperCase()}</span>`
    : icon('user', 18);
  topbarAvatar.setAttribute('aria-label', user ? `Signed in as ${user.fullName}` : 'Sign in');
}

/* ---------------------------------------------------------------------- guards */

function requireSignIn() {
  if (session.isSignedIn) return true;
  window.location.hash = '#/signin?next=1';
  toast('Sign in to continue.', 'error');
  return false;
}

async function completeOAuthLogin(container) {
  container.innerHTML = `<div class="auth"><div class="auth__head">
    <h1 class="auth__title">Finishing sign in</h1>
    <p class="auth__sub">Just a moment while we open your CBDVentures account.</p>
  </div></div>`;
  try {
    const { data } = await api.exchangeOAuth();
    const user = session.save(data);
    setState({ user });
    const next = sessionStorage.getItem('cbd.oauth.next') || '#/';
    sessionStorage.removeItem('cbd.oauth.next');
    window.dispatchEvent(new Event('cbd:session'));
    toast(`Welcome, ${user.fullName.split(' ')[0]}.`, 'success');
    location.hash = next;
  } catch {
    sessionStorage.removeItem('cbd.oauth.next');
    location.hash = '#/signin?oauth_error=provider';
  }
}

function deny() {
  main.innerHTML = `<div class="card" style="margin-top:26px;display:grid;gap:12px;justify-items:center;text-align:center">
    <span class="empty__icon">${icon('shield', 24)}</span>
    <h2>That area is for administrators</h2>
    <p class="muted small">Your account is signed in, but it does not have access to this module.</p>
    <a class="btn btn--sm" href="#/">Back to discover</a>
  </div>`;
  hydrateIcons(main);
}

function renderNotFound(container) {
  container.innerHTML = `<div class="card" style="margin-top:26px;display:grid;gap:12px;justify-items:center;text-align:center">
    <span class="empty__icon">${icon('compass', 24)}</span>
    <h1 style="font-size:1.4rem">That page has moved</h1>
    <p class="muted small">We could not find what you were looking for inside the pilot zone.</p>
    <a class="btn btn--sm" href="#/">Back to discover</a>
  </div>`;
}

/* ------------------------------------------------------------------- bootstrap */

window.addEventListener('hashchange', router);

document.getElementById('notify-button')?.addEventListener('click', () => {
  const unread = state.home?.events?.length || 0;
  toast(unread
    ? `${unread} new happenings in the zone this week — see Events.`
    : 'You are all caught up.');
});

function setOffline(isOffline) {
  const banner = document.getElementById('offline-banner');
  if (!banner) return;
  banner.hidden = !isOffline;
  document.body.classList.toggle('is-offline', isOffline);
}
setOffline(navigator.onLine === false);
window.addEventListener('online', () => { setOffline(false); toast('Back online.', 'success'); });
window.addEventListener('offline', () => setOffline(true));

/* Keep the profile chip in sync when a view signs the visitor out. */
window.addEventListener('cbd:session', () => { paintAvatar(); });

async function warmUp() {
  // Prime the shared zone + category data so the first screen renders instantly.
  try {
    const [zone, categories] = await Promise.all([api.zone(), api.categories()]);
    setState({ zone: zone.data, categories: categories.data });
  } catch { /* offline: views handle their own fallbacks */ }

  if (session.isSignedIn) {
    try {
      const me = await api.me();
      if (me.data) session.updateUser({ ...session.user, ...me.data });
      else { signOut(); window.dispatchEvent(new Event('cbd:session')); }
    } catch { /* keep the cached identity */ }
  }
}

warmUp().finally(() => {
  hydrateIcons(document);
  router();
});
