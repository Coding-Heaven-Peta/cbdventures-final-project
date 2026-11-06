/**
 * Discover — the home feed.
 * Reference layout: greeting, rounded search, category chips, a horizontal rail
 * of large cards ("Featured"), then a two-up grid ("Popular right now").
 */
import { api, session } from '../api.js';
import { icon, distanceText, escapeHtml, priceSymbols, skeletonCards } from '../ui.js';
import { setState, state } from '../state.js';

const CATEGORY_CHIPS = [
  { slug: '', label: 'All' },
  { slug: 'food-drinks', label: 'Food & Drinks' },
  { slug: 'restaurants', label: 'Restaurants' },
  { slug: 'salons', label: 'Salons' },
  { slug: 'study-spaces', label: 'Study & Social' },
  { slug: 'events', label: 'Events' }
];

export function mediaCard(business) {
  return `
    <a class="media-card" href="#/business/${business.id}">
      <div class="media-card__img">
        <img src="${escapeHtml(business.thumbnail)}" alt="${escapeHtml(business.name)}" loading="lazy">
        <span class="media-card__badge">${escapeHtml(business.categoryName)}</span>
        <span class="media-card__rating">
          ${icon('star', 13)} ${business.reviewCount ? business.rating.toFixed(1) : 'New'}
        </span>
      </div>
      <div class="media-card__body">
        <span class="media-card__title">${escapeHtml(business.name)}</span>
        <span class="media-card__meta">
          <span>${icon('pin', 13)} ${distanceText(business.distanceKm, business.walkingMinutes)}</span>
          <span>${icon('clock', 13)} ${escapeHtml(business.todayHours || 'Hours unknown')}</span>
        </span>
      </div>
    </a>`;
}

export function miniCard(business) {
  return `
    <a class="mini-card" href="#/business/${business.id}">
      <div class="mini-card__img">
        <img src="${escapeHtml(business.thumbnail)}" alt="${escapeHtml(business.name)}" loading="lazy">
      </div>
      <div class="mini-card__body">
        <span class="mini-card__title">${escapeHtml(business.name)}</span>
        <span class="mini-card__row">
          <span>${priceSymbols(business.priceLevel)} · ${escapeHtml((business.categoryName || '').split(' ')[0])}</span>
          <span class="rating-inline">${icon('star', 12)} ${business.reviewCount ? business.rating.toFixed(1) : '—'}</span>
        </span>
        <span class="mini-card__row">
          <span>${icon('pin', 11)} ${distanceText(business.distanceKm, business.walkingMinutes)}</span>
          <span>${business.openNow === false ? '<span style="color:var(--red)">Closed</span>' : ''}</span>
        </span>
      </div>
    </a>`;
}

export async function renderDiscover(container, params = {}) {
  const user = session.user;
  const firstName = user?.fullName?.split(' ')[0];

  container.innerHTML = `
    <div class="greet">
      <div>
        <p class="greet__hello">${firstName ? `Hello, ${escapeHtml(firstName)}!` : 'Hello, Explorer!'}</p>
        <h1 class="greet__title">Discover Pretoria Central</h1>
      </div>
    </div>

    <form class="search-pill" id="discover-search" style="margin-top:16px">
      ${icon('search', 18)}
      <input class="input" type="search" name="q" placeholder="Search monuments, eats, hotels…"
             aria-label="Search listings" value="${escapeHtml(params.q || '')}">
    </form>

    <div class="chips" id="category-chips">
      ${CATEGORY_CHIPS.map((chip) => `
        <button class="chip ${chip.slug === (params.category || '') ? 'is-active' : ''}"
                type="button" data-category="${chip.slug}">${chip.label}</button>`).join('')}
    </div>

    <div id="feed">${skeletonCards(2)}</div>`;

  container.querySelector('#category-chips').addEventListener('click', (event) => {
    const chip = event.target.closest('[data-category]');
    if (!chip) return;
    container.querySelectorAll('.chip').forEach((node) => node.classList.toggle('is-active', node === chip));
    loadFeed(container.querySelector('#feed'), chip.dataset.category);
  });

  container.querySelector('#discover-search').addEventListener('submit', (event) => {
    event.preventDefault();
    const value = event.target.q.value.trim();
    location.hash = value ? `#/explore?q=${encodeURIComponent(value)}` : '#/explore';
  });

  await loadFeed(container.querySelector('#feed'), params.category || '');
}

async function loadFeed(feed, categorySlug) {
  feed.innerHTML = skeletonCards(2);

  let home;
  try {
    const { data } = await api.home(state.usingMyLocation ? state.coords : null);
    home = data;
  } catch (error) {
    feed.innerHTML = `<div class="card center" style="display:grid;gap:10px;justify-items:center">
      <span class="empty__icon">${icon('alert', 22)}</span>
      <strong>We could not load the feed</strong>
      <p class="muted small">${escapeHtml(error.message)}</p>
      <button class="btn btn--sm" type="button" id="retry-feed">Try again</button>
    </div>`;
    feed.querySelector('#retry-feed').addEventListener('click', () => loadFeed(feed, categorySlug));
    return;
  }

  setState({ home });

  const matches = (list) => (categorySlug
    ? list.filter((business) => business.categorySlug === categorySlug)
    : list);

  const featured = matches(home.featured || []);
  const popular = matches(home.recommended || []);
  const events = home.events || [];
  const zone = home.zone || state.zone;

  feed.innerHTML = `
    ${featured.length ? `
      <div class="section-head">
        <h2>${categorySlug ? 'Near the campus' : 'Featured near campus'}</h2>
        <a href="#/explore${categorySlug ? `?category=${categorySlug}` : ''}">See All</a>
      </div>
      <div class="rail">${featured.slice(0, 6).map(mediaCard).join('')}</div>` : ''}

    ${popular.length ? `
      <div class="section-head">
        <h2>${home.personalisation?.personalised ? 'Picked for you' : 'Popular right now'}</h2>
        <a href="#/explore${categorySlug ? `?category=${categorySlug}` : ''}">See All</a>
      </div>
      <div class="card-grid">${popular.slice(0, 6).map(miniCard).join('')}</div>`
      : `<div class="empty card"><span class="empty__icon">${icon('search', 22)}</span>
           <strong>Nothing in this category yet</strong>
           <p class="muted small">Try another category — the pilot covers five.</p></div>`}

    ${events.length ? `
      <div class="section-head">
        <h2>Happening soon</h2>
        <a href="#/events">See All</a>
      </div>
      <div class="stack">${events.slice(0, 3).map(eventRow).join('')}</div>` : ''}

    <div class="card" style="margin-top:20px;display:grid;gap:8px">
      <div class="row row--between">
        <strong class="small">${icon('pin', 14)} ${escapeHtml(zone?.name || 'Richfield campus')}</strong>
        <span class="badge badge--violet">${home.stats?.businesses || 0} live listings</span>
      </div>
      <p class="field__hint">
        ${home.personalisation?.basis || 'What is popular near Richfield right now'} ·
        ${home.stats?.reviews || 0} reviews · average ${home.stats?.averageRating || '—'} ★
      </p>
    </div>`;
}

function eventRow(event) {
  return `
    <a class="list-card" href="#/business/${event.businessId}">
      <div class="list-card__img">
        <img src="${escapeHtml(event.thumbnail || '/images/hero-welcome.svg')}" alt="" loading="lazy">
      </div>
      <div>
        <div class="list-card__title">${escapeHtml(event.title)}</div>
        <div class="list-card__meta">
          <span>${icon('calendar', 12)} ${new Date(event.eventDateTime).toLocaleDateString('en-ZA',
            { weekday: 'short', day: 'numeric', month: 'short' })}</span>
          <span>${icon('pin', 12)} ${escapeHtml(event.businessName)}</span>
        </div>
      </div>
      <div class="list-card__tail">
        <span class="badge ${event.free ? 'badge--green' : 'badge--gold'}">${event.free ? 'Free' : `R${event.ticketPrice}`}</span>
      </div>
    </a>`;
}
