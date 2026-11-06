/**
 * Shared UI kit: inline SVG icon set, formatting helpers and reusable
 * components (business cards, event rows, toasts, modals, empty states).
 * Everything is string based so views stay declarative and dependency free.
 */

/* -------------------------------------------------------------------- icons */

const ICONS = {
  locate: '<path d="M12 21s7-6.1 7-11a7 7 0 1 0-14 0c0 4.9 7 11 7 11Z"/><circle cx="12" cy="10" r="2.6"/>',
  user: '<circle cx="12" cy="8" r="3.6"/><path d="M5 20c1.2-3.6 3.8-5.4 7-5.4s5.8 1.8 7 5.4"/>',
  search: '<circle cx="11" cy="11" r="6.4"/><path d="m16 16 4.2 4.2"/>',
  compass: '<circle cx="12" cy="12" r="8.6"/><path d="m15.2 8.8-2 4.4-4.4 2 2-4.4z"/>',
  ticket: '<path d="M4 9V6.5h16V9a3 3 0 0 0 0 6v2.5H4V15a3 3 0 0 0 0-6Z"/><path d="M10 6.5v11"/>',
  store: '<path d="M4 9.5 5.6 5h12.8L20 9.5"/><path d="M4 9.5h16V19H4z"/><path d="M9 19v-5h6v5"/>',
  star: '<path d="m12 4.4 2.3 4.7 5.2.8-3.8 3.6.9 5.1-4.6-2.4-4.6 2.4.9-5.1L4.5 9.9l5.2-.8z"/>',
  starHalf: '<path d="m12 4.4 2.3 4.7 5.2.8-3.8 3.6.9 5.1-4.6-2.4-4.6 2.4.9-5.1L4.5 9.9l5.2-.8z"/>',
  pin: '<path d="M12 21s6.5-5.7 6.5-10.4a6.5 6.5 0 1 0-13 0C5.5 15.3 12 21 12 21Z"/><circle cx="12" cy="10.3" r="2.4"/>',
  clock: '<circle cx="12" cy="12" r="8.4"/><path d="M12 7.6V12l3 1.9"/>',
  phone: '<path d="M7.4 4.5h2.9l1.4 3.6-2 1.3a10.4 10.4 0 0 0 4.9 4.9l1.3-2 3.6 1.4v2.9a2 2 0 0 1-2.2 2A15.4 15.4 0 0 1 5.4 6.7a2 2 0 0 1 2-2.2Z"/>',
  globe: '<circle cx="12" cy="12" r="8.4"/><path d="M3.6 12h16.8M12 3.6c2.4 2.3 3.6 5.2 3.6 8.4s-1.2 6.1-3.6 8.4c-2.4-2.3-3.6-5.2-3.6-8.4S9.6 5.9 12 3.6Z"/>',
  directions: '<path d="m20 12-8 8-8-8 8-8z"/><path d="M12 15v-3.2h3.4v2"/>',
  whatsapp: '<path d="M20 12a8 8 0 0 1-11.7 7.1L4 20.2l1.2-4.2A8 8 0 1 1 20 12Z"/><path d="M9 9.6c0 3 2.4 5.4 5.4 5.4.6 0 1.2-.5 1.2-1.1l-.2-1-1.7-.5-.8.9a5.4 5.4 0 0 1-2.2-2.2l.9-.8-.5-1.7-1-.2c-.6 0-1.1.6-1.1 1.2Z"/>',
  filter: '<path d="M4 6.5h16M7 12h10M10 17.5h4"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  check: '<path d="m5 12.6 4.4 4.4L19 7.4"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  edit: '<path d="M4 20h4l10-10-4-4L4 16z"/><path d="m13.6 5.4 4 4"/>',
  trash: '<path d="M5 7h14M9 7V5h6v2M7 7l1 12h8l1-12"/>',
  calendar: '<rect x="4" y="6" width="16" height="14" rx="2.5"/><path d="M4 10.5h16M9 4v4M15 4v4"/>',
  chevronDown: '<path d="m7 10 5 5 5-5"/>',
  chevronRight: '<path d="m10 7 5 5-5 5"/>',
  arrowLeft: '<path d="M19 12H5m6-6-6 6 6 6"/>',
  arrowRight: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
  sparkles: '<path d="m12 4 1.7 4.3L18 10l-4.3 1.7L12 16l-1.7-4.3L6 10l4.3-1.7z"/><path d="M18.5 15.5 19 17l1.5.5L19 18l-.5 1.5L18 18l-1.5-.5L18 17z"/>',
  info: '<circle cx="12" cy="12" r="8.4"/><path d="M12 11v5M12 8.2h.01"/>',
  alert: '<path d="M12 4.5 21 19H3z"/><path d="M12 10v4M12 16.6h.01"/>',
  logout: '<path d="M14 5H6v14h8"/><path d="M11 12h9m-3-3 3 3-3 3"/>',
  shield: '<path d="M12 3.6 19 6v6c0 4-3 7-7 8.4-4-1.4-7-4.4-7-8.4V6z"/><path d="m9 12 2 2 4-4"/>',
  wifi: '<path d="M4.5 9.5a11 11 0 0 1 15 0M7.5 12.8a7 7 0 0 1 9 0M10.4 16a3 3 0 0 1 3.2 0"/><circle cx="12" cy="19" r=".8" fill="currentColor"/>',
  power: '<path d="M13 3.5 6 13h5l-1 7.5L17 11h-5z"/>',
  plug: '<path d="M9 3.5v5M15 3.5v5"/><path d="M6.5 8.5h11v3a5.5 5.5 0 0 1-11 0z"/><path d="M12 17v3.5"/>',
  quiet: '<path d="M4 14V9.5h3.5L12 6v12L7.5 14z"/><path d="m16 9.5 4 5m0-5-4 5"/>',
  student: '<path d="m4 9 8-3.5L20 9l-8 3.5z"/><path d="M7 11v4.5c0 1.4 2.2 2.5 5 2.5s5-1.1 5-2.5V11"/>',
  card: '<rect x="3.5" y="6" width="17" height="12" rx="2.5"/><path d="M3.5 10h17M7 14.5h4"/>',
  cash: '<rect x="3.5" y="6.5" width="17" height="11" rx="2"/><circle cx="12" cy="12" r="2.6"/>',
  takeaway: '<path d="M6 8h12l-1.2 11H7.2z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
  outdoor: '<path d="M12 4v4M12 8a5 5 0 0 0-5 5h10a5 5 0 0 0-5-5Z"/><path d="M5 20h14M12 13v7"/>',
  aircon: '<rect x="3.5" y="5" width="17" height="7" rx="2"/><path d="M7 15.5c0 1.6 1.4 1.6 1.4 3.2M12 15.5c0 1.6 1.4 1.6 1.4 3.2M17 15.5c0 1.6 1.4 1.6 1.4 3.2"/>',
  accessible: '<circle cx="12" cy="5" r="2"/><path d="M12 8v5h4l1.5 6M12 10.5a4.5 4.5 0 1 0 2.4 8.3"/>',
  parking: '<rect x="4" y="4" width="16" height="16" rx="3"/><path d="M10 16.5V8h2.6a2.8 2.8 0 0 1 0 5.6H10"/>',
  portion: '<path d="M4 12h16a8 8 0 0 1-16 0Z"/><path d="M8 8.5c0-1.5 1.2-1.5 1.2-3M12 8.5c0-1.5 1.2-1.5 1.2-3M16 8.5c0-1.5 1.2-1.5 1.2-3"/>',
  late: '<path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z"/>',
  food: '<path d="M7 3.5v7a2.5 2.5 0 0 0 5 0v-7M9.5 3.5v17M18 3.5c-1.8 0-3 2.4-3 5.5s1.2 4.5 3 4.5V20"/>',
  restaurant: '<circle cx="12" cy="12" r="7.5"/><circle cx="12" cy="12" r="3.6"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/>',
  salon: '<circle cx="8" cy="7" r="2.6"/><circle cx="8" cy="17" r="2.6"/><path d="m10 8.5 9 6.5M10 15.5l9-6.5"/>',
  study: '<path d="M4 6.5C6.5 5 9.3 5 12 6.5v12C9.3 17 6.5 17 4 18.5z"/><path d="M20 6.5C17.5 5 14.7 5 12 6.5v12c2.7-1.5 5.5-1.5 8 0z"/>',
  event: '<path d="M4 8.5h16v3a2.5 2.5 0 0 0 0 5v3H4v-3a2.5 2.5 0 0 0 0-5z"/><path d="M13 8.5v11"/>',
  heart: '<path d="M12 19.5S4.5 15 4.5 9.8A3.8 3.8 0 0 1 12 8a3.8 3.8 0 0 1 7.5 1.8c0 5.2-7.5 9.7-7.5 9.7Z"/>',
  bag: '<path d="M5 8h14l-1 12H6z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
  grid: '<rect x="4" y="4" width="6.5" height="6.5" rx="1.6"/><rect x="13.5" y="4" width="6.5" height="6.5" rx="1.6"/><rect x="4" y="13.5" width="6.5" height="6.5" rx="1.6"/><rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.6"/>',
  users: '<circle cx="9" cy="9" r="3"/><path d="M3.5 19c.9-2.8 2.9-4.2 5.5-4.2s4.6 1.4 5.5 4.2"/><path d="M16 6.2a3 3 0 0 1 0 5.6M17.5 14.9c1.5.6 2.5 2 3 4.1"/>',
  chart: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  database: '<ellipse cx="12" cy="6" rx="7.5" ry="3"/><path d="M4.5 6v6c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3V6"/><path d="M4.5 12v6c0 1.7 3.4 3 7.5 3s7.5-1.3 7.5-3v-6"/>',
  download: '<path d="M12 4v10m0 0 3.8-3.8M12 14l-3.8-3.8"/><path d="M4.5 16.5V19a1.5 1.5 0 0 0 1.5 1.5h12A1.5 1.5 0 0 0 19.5 19v-2.5"/>',
  upload: '<path d="M12 20V10m0 0 3.8 3.8M12 10l-3.8 3.8"/><path d="M4.5 7.5V5A1.5 1.5 0 0 1 6 3.5h12A1.5 1.5 0 0 1 19.5 5v2.5"/>',
  printer: '<path d="M7 9V4h10v5"/><rect x="4" y="9" width="16" height="7" rx="1.6"/><path d="M7 16h10v4H7z"/>',

  /* --- dark-mode client additions ------------------------------------- */
  home: '<path d="M4 10.5 12 4l8 6.5"/><path d="M6.5 9.5V20h11V9.5"/>',
  map: '<path d="M9 4 3.8 6v14L9 18l6 2 5.2-2V4L15 6 9 4Z"/><path d="M9 4v14M15 6v14"/>',
  bell: '<path d="M18 15.5V11a6 6 0 1 0-12 0v4.5L4.5 18h15L18 15.5Z"/><path d="M10 21h4"/>',
  camera: '<path d="M4 8.5h3l1.4-2h7.2L17 8.5h3v11H4z"/><circle cx="12" cy="14" r="3.2"/>',
  eye: '<path d="M2.8 12S6 6.4 12 6.4 21.2 12 21.2 12 18 17.6 12 17.6 2.8 12 2.8 12Z"/><circle cx="12" cy="12" r="2.8"/>',
  eyeOff: '<path d="M4 4l16 16"/><path d="M9.9 5.1A9.8 9.8 0 0 1 12 4.9c6 0 9.2 5.6 9.2 5.6a17 17 0 0 1-2.7 3.3"/><path d="M6.4 6.9A16.7 16.7 0 0 0 2.8 10.5S6 16.1 12 16.1a9.4 9.4 0 0 0 3.4-.6"/><path d="M9.6 9.8a3 3 0 0 0 4.2 4.2"/>',
  arrowLeft: '<path d="M19 12H5"/><path d="m11 6-6 6 6 6"/>',
  chevronRight: '<path d="m9 5 7 7-7 7"/>',
  mail: '<rect x="3" y="5.5" width="18" height="13" rx="2.4"/><path d="m4 7 8 6 8-6"/>',
  lock: '<rect x="4.5" y="10" width="15" height="10" rx="2.4"/><path d="M8 10V7.5a4 4 0 0 1 8 0V10"/>',
  checkCircle: '<circle cx="12" cy="12" r="8.5"/><path d="m8.2 12.2 2.6 2.6 5-5.2"/>',
  shieldCheck: '<path d="M12 3.5 5 6.2v5.3c0 4.2 2.9 7.6 7 9 4.1-1.4 7-4.8 7-9V6.2L12 3.5Z"/><path d="m8.8 12 2.3 2.3 4.1-4.3"/>',
  wallet: '<rect x="3.5" y="6" width="17" height="12.5" rx="2.6"/><path d="M3.5 10.5h17"/><circle cx="17" cy="14.4" r="1.2"/>',
  chat: '<path d="M20 12a7.6 7.6 0 0 1-8 7.6 9 9 0 0 1-3-.5L4.5 20.5l1.2-3.6A7.3 7.3 0 0 1 4 12a7.6 7.6 0 0 1 8-7.6 7.6 7.6 0 0 1 8 7.6Z"/>',
  spark: '<path d="M12 3.5 13.8 9l5.7 1.8-5.7 1.8L12 18l-1.8-5.4L4.5 10.8 10.2 9 12 3.5Z"/>',
  google: '<path d="M21 12.2c0-.7-.06-1.4-.18-2H12v3.8h5.1a4.4 4.4 0 0 1-1.9 2.9v2.4h3.1c1.8-1.7 2.7-4.1 2.7-7.1Z"/><path d="M12 21c2.5 0 4.6-.8 6.3-2.2l-3.1-2.4c-.9.6-2 1-3.2 1a5.6 5.6 0 0 1-5.2-3.8H3.6v2.5A9 9 0 0 0 12 21Z"/><path d="M6.8 13.6a5.4 5.4 0 0 1 0-3.4V7.7H3.6a9 9 0 0 0 0 8.6l3.2-2.7Z"/><path d="M12 6.6c1.4 0 2.6.5 3.5 1.4l2.7-2.6A9 9 0 0 0 12 3a9 9 0 0 0-8.4 4.7l3.2 2.5A5.6 5.6 0 0 1 12 6.6Z"/>',
  apple: '<path d="M16.3 12.6c0-2.2 1.8-3.2 1.9-3.3-1-1.5-2.6-1.7-3.2-1.7-1.4-.1-2.6.8-3.3.8-.7 0-1.7-.8-2.9-.8-1.5 0-2.9.9-3.6 2.2-1.5 2.7-.4 6.6 1.1 8.8.7 1 1.6 2.2 2.8 2.1 1.1 0 1.5-.7 2.9-.7 1.3 0 1.7.7 2.9.7 1.2 0 2-1.1 2.7-2.2.9-1.3 1.2-2.6 1.3-2.6-.1 0-2.6-1-2.6-3.3Z"/><path d="M14.4 5.9c.6-.7 1-1.7.9-2.7-.9 0-2 .6-2.6 1.3-.6.6-1 1.6-.9 2.6 1 .1 2-.5 2.6-1.2Z"/>'
};

export function icon(name, size = 20, className = '') {
  const body = ICONS[name] || ICONS.info;
  return `<svg class="icon ${className}" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"
    aria-hidden="true" focusable="false">${body}</svg>`;
}

export function hydrateIcons(root = document) {
  root.querySelectorAll('[data-icon]').forEach((node) => {
    const name = node.getAttribute('data-icon');
    if (!name || node.dataset.hydrated === name) return;
    node.innerHTML = icon(name, Number(node.dataset.size || 20));
    node.dataset.hydrated = name;
  });
}

const AMENITY_ICON = {
  'Free Wi-Fi': 'wifi',
  'Backup Power': 'power',
  'Power Outlets': 'plug',
  'Quiet Atmosphere': 'quiet',
  'Student Specials': 'student',
  'Card Payments': 'card',
  'Cash Only': 'cash',
  Takeaway: 'takeaway',
  'Outdoor Seating': 'outdoor',
  'Air Conditioning': 'aircon',
  'Wheelchair Accessible': 'accessible',
  'Parking Nearby': 'parking',
  'Generous Portions': 'portion',
  'Late Night': 'late'
};

export function amenityIcon(name) {
  return AMENITY_ICON[name] || 'check';
}

/* --------------------------------------------------------------- formatting */

export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function distanceText(km, walking) {
  if (km === null || km === undefined) return 'Distance unavailable';
  const distance = km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(1)} km`;
  return walking ? `${distance} · ${walking} min walk` : distance;
}

export function priceSymbols(level) {
  return level || 'R';
}

export function stars(rating, size = 15) {
  const value = Number(rating) || 0;
  let markup = '';
  for (let i = 1; i <= 5; i += 1) {
    const filled = value >= i - 0.25;
    // Filled stars are solid amber; empty stars stay as a light outline.
    const style = filled
      ? 'color:var(--amber-600);display:inline-flex'
      : 'color:var(--line-strong);display:inline-flex;opacity:.75';
    markup += `<span class="stars__item${filled ? ' stars__item--on' : ''}" style="${style}">${icon('star', size)}</span>`;
  }
  return `<span class="stars" role="img" aria-label="${value.toFixed(1)} out of 5 stars" style="display:inline-flex;gap:1px">${markup}</span>`;
}

export function ratingBlock(business) {
  if (!business.reviewCount) {
    return `<span class="rating rating--empty">${icon('star', 15)} New listing</span>`;
  }
  return `<span class="rating">${icon('star', 15, 'star')} ${business.rating.toFixed(1)}
    <span class="rating__count">(${business.reviewCount})</span></span>`;
}

export function openBadge(openNow) {
  if (openNow === null || openNow === undefined) {
    return '<span class="badge badge--closed">Hours unknown</span>';
  }
  return openNow
    ? '<span class="badge badge--open">Open now</span>'
    : '<span class="badge badge--closed">Closed</span>';
}

export function statusBadge(status) {
  const map = {
    ACTIVE: ['badge--active', 'Live'],
    PENDING: ['badge--pending', 'Pending review'],
    REJECTED: ['badge--rejected', 'Not approved'],
    INACTIVE: ['badge--closed', 'Hidden']
  };
  const [cls, label] = map[status] || ['badge--closed', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

export function formatDate(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return `${date.getDate()} ${MONTHS[date.getMonth()]} ${date.getFullYear()}`;
}

export function formatDateTime(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${DAYS[date.getDay()]} ${date.getDate()} ${MONTHS[date.getMonth()]}, ${hours}:${minutes}`;
}

export function relativeTime(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const diff = Date.now() - date.getTime();
  const days = Math.floor(diff / 86400000);
  if (days <= 0) return 'Today';
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days} days ago`;
  if (days < 30) return `${Math.floor(days / 7)} week${days < 14 ? '' : 's'} ago`;
  if (days < 365) return `${Math.floor(days / 30)} month${days < 60 ? '' : 's'} ago`;
  return `${Math.floor(days / 365)} year${days < 730 ? '' : 's'} ago`;
}

export function money(value) {
  const number = Number(value) || 0;
  if (number === 0) return 'Free entry';
  return `R${number.toFixed(0)}`;
}

export function initials(name) {
  if (!name) return '?';
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
}

/* --------------------------------------------------------------- components */

export function bizCard(business) {
  const amenities = (business.amenities || []).slice(0, 3);
  return `
    <a class="biz-card" href="#/business/${business.id}" aria-label="${escapeHtml(business.name)}">
      <div class="biz-card__media">
        <img src="${escapeHtml(business.thumbnail)}" alt="${escapeHtml(business.name)}" loading="lazy" width="400" height="250">
        <div class="biz-card__badges">
          ${openBadge(business.openNow)}
          ${business.distanceKm != null
            ? `<span class="badge badge--glass">${business.distanceKm < 1
                ? Math.round(business.distanceKm * 1000) + ' m'
                : business.distanceKm.toFixed(1) + ' km'}</span>` : ''}
        </div>
        <span class="biz-card__price" title="${escapeHtml(business.priceLabel || '')}">${priceSymbols(business.priceLevel)}</span>
      </div>
      <div class="biz-card__body">
        <div class="biz-card__title">${escapeHtml(business.name)}</div>
        <div class="biz-card__cat">${icon('pin', 14)} ${escapeHtml(business.categoryName)}</div>
        ${business.todayHours
          ? `<div class="biz-card__hours">${icon('clock', 13)} ${escapeHtml(business.todayHours)}</div>`
          : ''}
        <div class="biz-card__amenities">
          ${amenities.map((a) => `<span class="tag">${escapeHtml(a)}</span>`).join('')}
        </div>
        <div class="biz-card__footer">
          ${ratingBlock(business)}
          <span>${business.walkingMinutes ? business.walkingMinutes + ' min' : ''}</span>
        </div>
      </div>
    </a>`;
}

export function eventCard(event) {
  const date = new Date(event.eventDateTime);
  return `
    <a class="event-card" href="#/business/${event.businessId}">
      <div class="event-date">
        <strong>${date.getDate()}</strong>
        <span>${MONTHS[date.getMonth()]}</span>
      </div>
      <div>
        <div class="event-card__title">${escapeHtml(event.title)}</div>
        <div class="event-card__meta">
          <span>${icon('clock', 14)} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}</span>
          <span>${icon('pin', 14)} ${escapeHtml(event.businessName)}</span>
          ${event.venue ? `<span>${escapeHtml(event.venue)}</span>` : ''}
        </div>
      </div>
      <div class="event-card__price">
        ${event.free ? 'Free' : money(event.ticketPrice)}
        <small>${event.distanceKm != null ? distanceText(event.distanceKm) : ''}</small>
      </div>
    </a>`;
}

export function skeletonCards(count = 6) {
  return Array.from({ length: count }, () => '<div class="skeleton skeleton--card"></div>').join('');
}

export function emptyState({ iconName = 'search', title, text, action = '' }) {
  return `
    <div class="empty">
      <div class="empty__icon">${icon(iconName, 26)}</div>
      <h3>${escapeHtml(title)}</h3>
      <p>${escapeHtml(text)}</p>
      ${action}
    </div>`;
}

export function toast(message, type = 'default', timeout = 4200) {
  const stack = document.getElementById('toast-stack');
  if (!stack) return;
  const node = document.createElement('div');
  node.className = `toast toast--${type}`;
  const glyph = type === 'error' ? 'alert' : type === 'success' ? 'check' : 'info';
  node.innerHTML = `${icon(glyph, 18)}<div>${escapeHtml(message)}</div>`;
  stack.appendChild(node);
  setTimeout(() => {
    node.style.transition = 'opacity 200ms ease, transform 200ms ease';
    node.style.opacity = '0';
    node.style.transform = 'translateY(6px)';
    setTimeout(() => node.remove(), 220);
  }, timeout);
}

/* ------------------------------------------------------------------- modals */

let activeModal = null;

export function closeModal() {
  const root = document.getElementById('modal-root');
  if (!root) return;
  root.innerHTML = '';
  document.body.classList.remove('is-locked');
  activeModal = null;
}

/**
 * Opens a modal. `render` returns HTML, `onMount` wires up behaviour.
 */
export function openModal({ title, body, footer = '', wide = false, onMount, labelledBy = 'modal-title' }) {
  const root = document.getElementById('modal-root');
  if (!root) return;
  root.innerHTML = `
    <div class="modal-backdrop" data-modal-backdrop>
      <div class="modal ${wide ? 'modal--wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="${labelledBy}">
        <div class="modal__head">
          <h2 id="${labelledBy}">${title}</h2>
          <button class="icon-button icon-button--plain" type="button" data-modal-close aria-label="Close dialog">
            ${icon('close', 20)}
          </button>
        </div>
        <div class="modal__body">${body}</div>
        ${footer ? `<div class="modal__foot">${footer}</div>` : ''}
      </div>
    </div>`;

  document.body.classList.add('is-locked');
  activeModal = root.querySelector('.modal');

  root.querySelector('[data-modal-backdrop]').addEventListener('mousedown', (event) => {
    if (event.target === event.currentTarget) closeModal();
  });
  root.querySelectorAll('[data-modal-close]').forEach((button) =>
    button.addEventListener('click', closeModal));

  const firstFocusable = activeModal.querySelector('input, select, textarea, button:not([data-modal-close])');
  if (firstFocusable) setTimeout(() => firstFocusable.focus({ preventScroll: true }), 60);

  if (typeof onMount === 'function') onMount(activeModal);
  return activeModal;
}

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && activeModal) closeModal();
});

export function confirmDialog({ title, message, confirmLabel = 'Confirm', danger = false }) {
  return new Promise((resolve) => {
    openModal({
      title,
      body: `<p>${escapeHtml(message)}</p>`,
      footer: `
        <button class="button button--ghost" type="button" data-cancel>Cancel</button>
        <button class="button ${danger ? 'button--danger' : ''}" type="button" data-confirm>${escapeHtml(confirmLabel)}</button>`,
      onMount(modal) {
        modal.querySelector('[data-cancel]').addEventListener('click', () => {
          closeModal();
          resolve(false);
        });
        modal.querySelector('[data-confirm]').addEventListener('click', () => {
          closeModal();
          resolve(true);
        });
      }
    });
  });
}

/* ------------------------------------------------------------------ widgets */

/** Inline SVG map of the pilot zone with the anchor and nearby listings. */
export function zoneMap({ zone, businesses = [], focus = null, height = 260 }) {
  if (!zone) return '';
  const size = 300;
  const cx = size / 2;
  const cy = size / 2;
  const ringRadius = size * 0.36;
  const radiusKm = zone.primaryRadiusKm || 1;
  const metresPerDegreeLat = 111320;
  const metresPerDegreeLng = 111320 * Math.cos((zone.anchorLatitude * Math.PI) / 180);

  // metres -> svg units, so the 1 km ring is drawn to scale
  const scale = ringRadius / (radiusKm * 1000);

  const project = (lat, lng) => ({
    x: cx + (lng - zone.anchorLongitude) * metresPerDegreeLng * scale,
    y: cy - (lat - zone.anchorLatitude) * metresPerDegreeLat * scale
  });

  const anchor = { x: cx, y: cy };

  const pins = businesses
    .filter((business) => business.latitude && business.longitude)
    .filter((business) => !focus || business.id !== focus.id)
    .map((business) => {
      const point = project(business.latitude, business.longitude);
      if (Math.hypot(point.x - anchor.x, point.y - anchor.y) > ringRadius) return '';
      return `<a href="#/business/${business.id}" aria-label="${escapeHtml(business.name)}">
        <circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="4"
          fill="var(--orange-2)" stroke="var(--bg)" stroke-width="1.6">
          <title>${escapeHtml(business.name)}</title>
        </circle></a>`;
    })
    .join('');

  let focusPin = '';
  if (focus) {
    const point = focus.latitude && focus.longitude
      ? project(focus.latitude, focus.longitude)
      : { x: cx, y: cy };
    focusPin = `<g>
      <circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="14" fill="var(--gold)" opacity="0.24"/>
      <circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="6.5" fill="var(--gold)" stroke="var(--bg)" stroke-width="2">
        <title>${escapeHtml(focus.name)}</title>
      </circle></g>`;
  }

  return `
  <div class="zone-map">
    <svg viewBox="0 0 ${size} ${size}" role="img" aria-label="Map of the pilot zone around Richfield campus"
         style="max-height:${height}px">
      <defs>
        <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
          <path d="M20 0H0v20" fill="none" stroke="#24262C" stroke-width="1"/>
        </pattern>
        <radialGradient id="zoneFill" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#CE6338" stop-opacity="0.20"/>
          <stop offset="100%" stop-color="#CE6338" stop-opacity="0.05"/>
        </radialGradient>
      </defs>
      <rect width="${size}" height="${size}" fill="#121317"/>
      <rect width="${size}" height="${size}" fill="url(#grid)"/>
      <path d="M0 150h300M0 214h300M84 0v300M210 0v300" stroke="#1C1E23" stroke-width="7"/>
      <circle cx="${cx}" cy="${cy}" r="${ringRadius}" fill="url(#zoneFill)" stroke="#CE6338"
              stroke-width="1.6" stroke-dasharray="5 5"/>
      <circle cx="${cx}" cy="${cy}" r="9" fill="#E0A33B" opacity="0.18"/>
      <circle cx="${cx}" cy="${cy}" r="4.5" fill="#E0A33B" stroke="#121317" stroke-width="1.6"/>
      <text x="${cx}" y="${size - 8}" text-anchor="middle" font-size="8" font-weight="700"
            letter-spacing="0.8" fill="#E6E1DA" font-family="system-ui, sans-serif">RICHFIELD CAMPUS</text>
      ${pins}
      ${focusPin}
      <text x="${cx}" y="${cy - ringRadius + 14}" text-anchor="middle" font-size="8"
            fill="#918D87" font-family="system-ui, sans-serif">${radiusKm} KM PILOT ZONE</text>
    </svg>
  </div>`;
}
