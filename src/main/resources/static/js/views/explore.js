/**
 * Explore — search, filter and sort.
 * Same capabilities as the light edition (category, price, proximity, rating,
 * amenities, open-now, and distance/price/rating sorting) presented as a dark
 * results list with a bottom-sheet filter panel.
 */
import { api, currentPosition } from '../api.js';
import { icon, escapeHtml, distanceText, priceSymbols, skeletonCards, toast, openModal, closeModal } from '../ui.js';
import { setState, state } from '../state.js';

const SORTS = [
  { key: 'recommended', label: 'Best match' },
  { key: 'distance', label: 'Closest' },
  { key: 'price', label: 'Lowest price' },
  { key: 'rating', label: 'Highest rated' }
];

const DISTANCES = [
  { value: 0.25, label: '250 m' },
  { value: 0.5, label: '500 m' },
  { value: 1, label: '1 km' }
];

const PRICE_LABELS = { R: 'R · under R80', RR: 'RR · R80–R180', RRR: 'RRR · R180+' };

function blankFilters(params = {}) {
  return {
    q: params.q || '',
    category: params.category || '',
    price: [],
    minRating: null,
    maxDistanceKm: null,
    amenity: [],
    openNow: false,
    sort: params.sort || 'recommended'
  };
}

export async function renderExplore(container, params = {}) {
  const filters = blankFilters(params);
  let meta = { categories: [], amenities: [], priceLevels: [] };

  container.innerHTML = `
    <div class="greet">
      <div>
        <p class="greet__hello">Find it in the zone</p>
        <h1 class="greet__title">Explore</h1>
      </div>
      <button class="icon-button" type="button" id="filter-toggle" aria-label="Filters">
        ${icon('filter', 18)}
      </button>
    </div>

    <form id="explore-search" class="filter-bar">
      <span class="search-pill">
        ${icon('search', 18)}
        <input class="input" type="search" name="q" placeholder="Search listings…"
               aria-label="Search listings" value="${escapeHtml(filters.q)}">
      </span>
    </form>

    <div class="chips" id="sort-chips">
      ${SORTS.map((sort) => `
        <button class="chip ${sort.key === filters.sort ? 'is-active' : ''}" type="button"
                data-sort="${sort.key}">${sort.label}</button>`).join('')}
    </div>

    <div class="row row--between" style="margin:6px 0 12px">
      <span class="small muted" id="result-count">Searching…</span>
      <button class="btn--link small" type="button" id="clear-filters" hidden>Clear filters</button>
    </div>

    <div id="results">${skeletonCards(3)}</div>`;

  container.querySelector('#explore-search').addEventListener('submit', (event) => {
    event.preventDefault();
    filters.q = event.target.q.value.trim();
    run(container, filters);
  });
  container.querySelector('#explore-search').querySelector('input').addEventListener('input', (event) => {
    filters.q = event.target.value.trim();
    clearTimeout(run.timer);
    run.timer = setTimeout(() => run(container, filters), 320);
  });

  container.querySelector('#sort-chips').addEventListener('click', (event) => {
    const chip = event.target.closest('[data-sort]');
    if (!chip) return;
    filters.sort = chip.dataset.sort;
    container.querySelectorAll('[data-sort]').forEach((node) => node.classList.toggle('is-active', node === chip));
    run(container, filters);
  });

  container.querySelector('#filter-toggle').addEventListener('click', () => {
    openFilterSheet(meta, filters, () => run(container, filters));
  });

  container.querySelector('#clear-filters').addEventListener('click', () => {
    Object.assign(filters, blankFilters());
    container.querySelector('#explore-search input').value = '';
    container.querySelectorAll('[data-sort]').forEach((node) => node.classList.toggle('is-active', node.dataset.sort === 'recommended'));
    run(container, filters);
  });

  // Reference metadata for the filter sheet.
  try {
    const [categories, amenities, filterMeta] = await Promise.all([api.categories(), api.amenities(), api.filters()]);
    meta = {
      categories: categories.data || [],
      amenities: amenities.data || [],
      priceLevels: filterMeta.data?.priceLevels || []
    };
  } catch { /* the sheet still works with defaults */ }

  await run(container, filters);
}

async function run(container, filters) {
  const results = container.querySelector('#results');
  const count = container.querySelector('#result-count');
  const clear = container.querySelector('#clear-filters');
  if (!results) return;

  const active = Boolean(filters.category || filters.price.length || filters.minRating
    || filters.maxDistanceKm || filters.amenity.length || filters.openNow);
  clear.hidden = !active;

  results.innerHTML = skeletonCards(3);

  try {
    const { data } = await api.businesses({
      q: filters.q || undefined,
      category: filters.category || undefined,
      price: filters.price.length ? filters.price : undefined,
      minRating: filters.minRating || undefined,
      maxDistanceKm: filters.maxDistanceKm || undefined,
      amenity: filters.amenity.length ? filters.amenity : undefined,
      openNow: filters.openNow ? true : undefined,
      sort: filters.sort,
      size: 60,
      lat: state.usingMyLocation ? state.coords?.lat : undefined,
      lng: state.usingMyLocation ? state.coords?.lng : undefined
    });

    count.textContent = `${data.total} listing${data.total === 1 ? '' : 's'} · sorted by ${SORTS.find((s) => s.key === filters.sort)?.label.toLowerCase()}`;

    results.innerHTML = data.items.length
      ? data.items.map(resultCard).join('')
      : `<div class="card empty">
           <span class="empty__icon">${icon('search', 22)}</span>
           <strong>No matches inside the zone</strong>
           <p class="muted small">Loosen a filter, widen the distance, or search a different word.</p>
         </div>`;
  } catch (error) {
    results.innerHTML = `<div class="card empty">
      <span class="empty__icon">${icon('alert', 22)}</span>
      <strong>Search failed</strong>
      <p class="muted small">${escapeHtml(error.message)}</p>
    </div>`;
  }
}

function resultCard(business) {
  const open = business.openNow === null || business.openNow === undefined
    ? ''
    : business.openNow
      ? '<span class="badge badge--open">Open</span>'
      : '<span class="badge badge--closed">Closed</span>';

  return `
    <a class="list-card" href="#/business/${business.id}">
      <div class="list-card__img">
        <img src="${escapeHtml(business.thumbnail)}" alt="" loading="lazy">
      </div>
      <div>
        <div class="list-card__title">${escapeHtml(business.name)}</div>
        <div class="list-card__meta">
          <span>${icon('pin', 12)} ${distanceText(business.distanceKm, business.walkingMinutes)}</span>
          <span>${priceSymbols(business.priceLevel)}</span>
          <span>${icon('clock', 12)} ${escapeHtml(business.todayHours || 'Hours unknown')}</span>
        </div>
        <div class="list-card__meta" style="margin-top:6px">${open}</div>
      </div>
      <div class="list-card__tail">
        <span class="rating-inline">${icon('star', 12)} ${business.reviewCount ? business.rating.toFixed(1) : 'New'}</span>
        <span>${business.reviewCount} rev.</span>
      </div>
    </a>`;
}

/* --------------------------------------------------------------- filter sheet */

function openFilterSheet(meta, filters, onChange) {
  const draft = {
    category: filters.category,
    price: [...filters.price],
    minRating: filters.minRating,
    maxDistanceKm: filters.maxDistanceKm,
    amenity: [...filters.amenity],
    openNow: filters.openNow
  };

  // The API indexes price levels 1..3 (R, RR, RRR) — see BusinessQuery.priceLevelEnums().
  const priceOptions = [
    { value: 1, label: PRICE_LABELS.R },
    { value: 2, label: PRICE_LABELS.RR },
    { value: 3, label: PRICE_LABELS.RRR }
  ];

  const body = `
    <div class="filter-sheet">
      <div class="filter-group">
        <span class="filter-group__label">Category</span>
        <div class="filter-options" data-group="category">
          <button class="chip ${!draft.category ? 'is-active' : ''}" type="button" data-value="">All</button>
          ${(meta.categories || []).map((category) => `
            <button class="chip ${draft.category === category.slug ? 'is-active' : ''}" type="button"
                    data-value="${category.slug}">${escapeHtml(category.name)}</button>`).join('')}
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-group__label">Price range</span>
        <div class="filter-options" data-group="price">
          ${priceOptions.map((option) => `
            <button class="chip ${draft.price.includes(option.value) ? 'is-active' : ''}" type="button"
                    data-value="${option.value}">${escapeHtml(option.label)}</button>`).join('')}
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-group__label">Walking distance</span>
        <div class="filter-options" data-group="distance">
          ${DISTANCES.map((option) => `
            <button class="chip ${draft.maxDistanceKm === option.value ? 'is-active' : ''}" type="button"
                    data-value="${option.value}">${option.label}</button>`).join('')}
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-group__label">Minimum rating</span>
        <div class="filter-options" data-group="rating">
          ${[3, 4, 4.5].map((value) => `
            <button class="chip ${draft.minRating === value ? 'is-active' : ''}" type="button"
                    data-value="${value}">${value}★ and up</button>`).join('')}
        </div>
      </div>

      <div class="filter-group">
        <span class="filter-group__label">Amenities</span>
        <div class="filter-options" data-group="amenity">
          ${(meta.amenities || []).slice(0, 14).map((amenity) => `
            <button class="chip ${draft.amenity.includes(amenity.id) ? 'is-active' : ''}" type="button"
                    data-value="${amenity.id}">${escapeHtml(amenity.name)}</button>`).join('')}
        </div>
      </div>

      <label class="row" style="gap:10px;color:var(--ink-2)">
        <input type="checkbox" id="sheet-open-now" ${draft.openNow ? 'checked' : ''}
               style="width:18px;height:18px;accent-color:var(--orange)">
        <span class="small">Only show places that are open right now</span>
      </label>

      <button class="btn--link small" type="button" id="sheet-near-me">${icon('locate', 14)} Use my location for distances</button>
    </div>`;

  openModal({
    title: 'Filters',
    body,
    footer: `
      <button class="btn btn--ghost" type="button" id="sheet-reset">Reset</button>
      <button class="btn" type="button" id="sheet-apply">Show results</button>`,
    onMount(modal) {
      const read = () => {
        const group = (name) => [...modal.querySelectorAll(`[data-group="${name}"] .chip.is-active`)]
          .map((chip) => chip.dataset.value);

        draft.category = group('category')[0] || '';
        draft.price = group('price').map(Number);
        draft.maxDistanceKm = Number(group('distance')[0]) || null;
        draft.minRating = Number(group('rating')[0]) || null;
        draft.amenity = group('amenity').map(Number);
        draft.openNow = modal.querySelector('#sheet-open-now').checked;
      };

      modal.querySelectorAll('.filter-options').forEach((group) => {
        group.addEventListener('click', (event) => {
          const chip = event.target.closest('.chip');
          if (!chip) return;
          const single = group.dataset.group === 'category'
            || group.dataset.group === 'distance'
            || group.dataset.group === 'rating';
          if (single) {
            group.querySelectorAll('.chip').forEach((node) => node.classList.toggle('is-active', node === chip));
          } else {
            chip.classList.toggle('is-active');
          }
        });
      });

      modal.querySelector('#sheet-reset').addEventListener('click', () => {
        modal.querySelectorAll('.chip').forEach((chip) => chip.classList.remove('is-active'));
        modal.querySelector('[data-group="category"] .chip').classList.add('is-active');
        modal.querySelector('#sheet-open-now').checked = false;
      });

      modal.querySelector('#sheet-near-me').addEventListener('click', async () => {
        try {
          const coords = await currentPosition();
          setState({ coords, usingMyLocation: true });
          toast('Distances now measured from your location.', 'success');
        } catch (error) {
          toast(error.message, 'error');
        }
      });

      modal.querySelector('#sheet-apply').addEventListener('click', () => {
        read();
        Object.assign(filters, draft);
        closeModal();
        onChange();
      });
    }
  });
}
