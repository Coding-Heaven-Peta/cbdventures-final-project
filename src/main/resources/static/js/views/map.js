/**
 * Map — every live listing plotted inside the 1 km pilot ring, with a scrollable
 * list underneath so the screen is usable without colour vision or a big screen.
 */
import { api } from '../api.js';
import { icon, escapeHtml, distanceText, priceSymbols, skeletonCards } from '../ui.js';
import { state } from '../state.js';
import { mountOpenStreetMap } from '../map-widget.js';

export async function renderMap(container) {
  container.innerHTML = `
    <div class="greet">
      <div>
        <p class="greet__hello">Everything within walking distance</p>
        <h1 class="greet__title">The 1 km zone</h1>
      </div>
    </div>
    <div id="map-body" style="margin-top:14px">${skeletonCards(1)}</div>`;

  const body = container.querySelector('#map-body');

  try {
    // /api/businesses/map returns id, name, coordinates, rating and distance for
    // every active listing inside the browsing radius.
    const { data: items } = await api.mapPins();
    const zone = state.zone;

    body.innerHTML = `
      <div class="map-frame">
        <div id="business-map" class="leaflet-map" role="application" aria-label="Interactive map of the Richfield pilot zone"></div>
      </div>
      <div class="map-legend">
        <span><i class="legend-dot"></i> Listing</span>
        <span><i class="legend-dot legend-dot--ring"></i> 1 km pilot zone</span>
        <span>${items.length} places</span>
      </div>

      <div class="section-head"><h2>Closest to the campus</h2>
        <a href="#/explore?sort=distance">See All</a></div>
      <div>
        ${items.slice(0, 14).map((business) => `
          <a class="list-card" href="#/business/${business.id}">
            <div class="list-card__img list-card__img--icon">
              <span class="list-card__glyph">${icon(business.iconKey || 'pin', 22)}</span>
            </div>
            <div>
              <div class="list-card__title">${escapeHtml(business.name)}</div>
              <div class="list-card__meta">
                <span>${icon('pin', 12)} ${distanceText(business.distanceKm, business.walkingMinutes)}</span>
                <span>${priceSymbols(business.priceLevel)}</span>
              </div>
            </div>
            <div class="list-card__tail">
              <span class="rating-inline">${icon('star', 12)} ${business.reviewCount ? business.rating.toFixed(1) : 'New'}</span>
            </div>
          </a>`).join('')}
      </div>`;
    mountOpenStreetMap(body.querySelector('#business-map'), { zone, businesses: items });
  } catch (error) {
    body.innerHTML = `<div class="card empty">
      <span class="empty__icon">${icon('alert', 22)}</span>
      <strong>The map needs a connection</strong>
      <p class="muted small">${escapeHtml(error.message)}</p>
    </div>`;
  }
}
