/**
 * Events — upcoming happenings attached to venues inside the zone.
 */
import { api } from '../api.js';
import { icon, escapeHtml, formatDateTime, money, skeletonCards, toast } from '../ui.js';

const SAVED_EVENTS_KEY = 'cbd.savedEvents';

export async function renderEvents(container) {
  container.innerHTML = `
    <div class="greet">
      <div>
        <p class="greet__hello">Plan your week</p>
        <h1 class="greet__title">What's on</h1>
      </div>
    </div>
    <div class="chips" id="event-chips" style="padding-bottom:10px">
      <button class="chip is-active" type="button" data-window="all">All</button>
      <button class="chip" type="button" data-window="week">This week</button>
      <button class="chip" type="button" data-window="month">This month</button>
      <button class="chip" type="button" data-window="free">Free entry</button>
      <button class="chip" type="button" data-window="saved">Saved</button>
    </div>
    <div id="event-list">${skeletonCards(2)}</div>`;

  const list = container.querySelector('#event-list');
  let events = [];

  container.querySelector('#event-chips').addEventListener('click', (event) => {
    const chip = event.target.closest('[data-window]');
    if (!chip) return;
    container.querySelectorAll('#event-chips .chip').forEach((node) => node.classList.toggle('is-active', node === chip));
    paint(list, filterEvents(events, chip.dataset.window), events, chip.dataset.window);
  });

  try {
    // /api/events returns a plain list (DiscoverController.events).
    const { data } = await api.events({ limit: 40 });
    const rows = Array.isArray(data) ? data : (data.items || []);
    events = rows.filter((event) => new Date(event.eventDateTime) > new Date());
    paint(list, filterEvents(events, 'all'), events, 'all');
  } catch (error) {
    list.innerHTML = `<div class="card empty">
      <span class="empty__icon">${icon('alert', 22)}</span>
      <strong>Could not load events</strong>
      <p class="muted small">${escapeHtml(error.message)}</p>
      <button class="btn btn--sm" type="button" id="retry-events">Try again</button>
    </div>`;
    list.querySelector('#retry-events').addEventListener('click', () => renderEvents(container));
  }
}

function filterEvents(events, window) {
  const now = Date.now();
  const week = now + 7 * 24 * 3600 * 1000;
  const month = now + 31 * 24 * 3600 * 1000;
  return events.filter((event) => {
    const time = new Date(event.eventDateTime).getTime();
    if (window === 'saved') return readSavedEvents().includes(String(event.id));
    if (window === 'week') return time <= week;
    if (window === 'month') return time <= month;
    if (window === 'free') return event.free;
    return true;
  });
}

function paint(list, events, allEvents = events, activeWindow = 'all') {
  list.innerHTML = events.length
    ? events.map((event) => {
      const saved = readSavedEvents().includes(String(event.id));
      return `
      <div class="card" style="margin-bottom:12px;display:grid;gap:10px">
        <div class="row row--between">
          <span class="badge badge--violet">${escapeHtml(event.categoryName || 'Event')}</span>
          <span class="badge ${event.free ? 'badge--green' : 'badge--gold'}">
            ${event.free ? 'Free' : money(event.ticketPrice)}</span>
        </div>
        <h3 style="font-size:1.05rem">${escapeHtml(event.title)}</h3>
        <p class="muted small">${escapeHtml(event.description || '')}</p>
        <div class="row" style="gap:14px;color:var(--muted);font-size:.8rem">
          <span>${icon('calendar', 13)} ${escapeHtml(formatDateTime(event.eventDateTime))}</span>
          <span>${icon('pin', 13)} ${escapeHtml(event.businessName)}</span>
        </div>
        <div class="row" style="gap:10px;margin-top:2px">
          <a class="btn btn--sm" href="#/business/${event.businessId}">See the venue</a>
          <button class="btn btn--ghost btn--sm" type="button" data-save="${event.id}" aria-pressed="${saved}">
            ${icon('heart', 14)} ${saved ? 'Saved' : 'Save'}
          </button>
        </div>
      </div>`;
    }).join('')
    : `<div class="card empty">
        <span class="empty__icon">${icon('ticket', 22)}</span>
        <strong>${activeWindow === 'saved' ? 'No saved events yet' : 'Nothing scheduled in this window'}</strong>
        <p class="muted small">${activeWindow === 'saved'
          ? 'Save an event and it will appear in this list on this device.'
          : 'Try “All” to see the pre-programmed upcoming events.'}</p>
      </div>`;

  list.querySelectorAll('[data-save]').forEach((button) => {
    button.addEventListener('click', () => {
      const eventId = String(button.dataset.save);
      const saved = readSavedEvents();
      const wasSaved = saved.includes(eventId);
      const next = wasSaved ? saved.filter((id) => id !== eventId) : [...saved, eventId];
      try {
        localStorage.setItem(SAVED_EVENTS_KEY, JSON.stringify(next));
      } catch {
        toast('This device could not save the event. Check your browser storage settings.', 'error');
        return;
      }
      paint(list, filterEvents(allEvents, activeWindow), allEvents, activeWindow);
      toast(wasSaved ? 'Removed from your saved events.' : 'Saved on this device.', 'success');
    });
  });
}

function readSavedEvents() {
  try {
    const value = JSON.parse(localStorage.getItem(SAVED_EVENTS_KEY) || '[]');
    return Array.isArray(value) ? value.map(String) : [];
  } catch {
    return [];
  }
}
