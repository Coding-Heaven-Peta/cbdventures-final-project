/**
 * Business profile — hero image, rating summary, opening hours for the whole
 * week, amenities, the zone map, reviews and the review form.
 */
import { api, session } from '../api.js';
import {
  icon, escapeHtml, distanceText, priceSymbols, stars, openModal, closeModal,
  toast, amenityIcon, relativeTime, initials, formatDateTime
} from '../ui.js';
import { state } from '../state.js';
import { mountOpenStreetMap } from '../map-widget.js';

export async function renderBusiness(container, id) {
  if (!id) { location.hash = '#/explore'; return; }

  container.innerHTML = `<div class="skeleton" style="height:300px;margin-top:8px"></div>`;

  let business;
  try {
    const { data } = await api.business(`${encodeURIComponent(id)}`);
    business = data;
  } catch (error) {
    container.innerHTML = `<div class="card empty" style="margin-top:26px">
      <span class="empty__icon">${icon('alert', 22)}</span>
      <strong>That listing is not available</strong>
      <p class="muted small">${escapeHtml(error.message)}</p>
      <a class="btn btn--sm" href="#/explore">Back to explore</a>
    </div>`;
    return;
  }

  const hero = business.thumbnail || business.images?.[0]?.url || '/images/hero-welcome.svg';
  const openState = business.openNow === null || business.openNow === undefined
    ? { label: 'Hours unknown', cls: '' }
    : business.openNow
      ? { label: 'Open now', cls: 'is-open' }
      : { label: 'Closed', cls: 'is-closed' };

  container.innerHTML = `
    <div class="biz-hero">
      <img src="${escapeHtml(hero)}" alt="${escapeHtml(business.name)}">
      <button class="biz-hero__back" type="button" data-back aria-label="Back">${icon('arrowLeft', 20)}</button>
      <span class="biz-hero__status ${openState.cls}">${openState.label}</span>
    </div>

    <div class="biz-head">
      <span class="badge badge--violet">${escapeHtml(business.categoryName)}</span>
      <h1>${escapeHtml(business.name)}</h1>
      <div class="biz-head__meta">
        <span class="rating-inline" style="font-size:.95rem">${icon('star', 15)}
          ${business.reviewCount ? business.rating.toFixed(1) : 'New'}
          <span class="muted" style="font-weight:500">(${business.reviewCount})</span>
        </span>
        <span>${icon('pin', 13)} ${distanceText(business.distanceKm, business.walkingMinutes)}</span>
        <span>${priceSymbols(business.priceLevel)} ${escapeHtml(business.priceLabel || '')}</span>
      </div>
    </div>

    <div class="biz-actions">
      <a class="btn" href="${escapeHtml(business.directionsUrl)}" target="_blank" rel="noopener">
        ${icon('directions', 17)} Get Directions
      </a>
      ${business.phone ? `<a class="btn btn--ghost" href="tel:${escapeHtml(business.phone.replace(/\s/g, ''))}"
           aria-label="Call ${escapeHtml(business.name)}">${icon('phone', 17)}</a>` : ''}
      <button class="btn btn--ghost" type="button" id="share-button" aria-label="Share">${icon('globe', 17)}</button>
    </div>

    ${business.description ? `<p class="muted" style="margin-top:16px;line-height:1.6">${escapeHtml(business.description)}</p>` : ''}

    <div class="card" style="margin-top:16px">
      <div class="row row--between" style="margin-bottom:6px">
        <strong>Opening hours</strong>
        <span class="small muted">${escapeHtml(business.todayHours || 'Not available')}</span>
      </div>
      ${hoursWeek(business)}
    </div>

    ${business.amenities?.length ? `
      <div class="card" style="margin-top:14px">
        <strong>Amenities</strong>
        <div class="amenity-chips">
          ${business.amenities.map((amenity) => `
            <span class="tag">${icon(amenityIcon(amenity.name), 13)} ${escapeHtml(amenity.name)}</span>`).join('')}
        </div>
      </div>` : ''}

    <div class="card" style="margin-top:14px">
      <strong>Location</strong>
      <div class="map-frame" style="margin-top:10px">
        <div id="business-location-map" class="leaflet-map leaflet-map--compact" role="application" aria-label="Map showing ${escapeHtml(business.name)}"></div>
      </div>
      <div class="fact-list" style="margin-top:6px">
        <div class="fact">
          <span class="fact__icon">${icon('pin', 16)}</span>
          <div><div class="fact__label">Address</div>
          <div class="fact__value">${escapeHtml(business.address)}</div></div>
        </div>
        ${business.phone ? `<div class="fact">
          <span class="fact__icon">${icon('phone', 16)}</span>
          <div><div class="fact__label">Contact</div>
          <div class="fact__value"><a href="tel:${escapeHtml(business.phone.replace(/\s/g, ''))}">${escapeHtml(business.phone)}</a></div></div>
        </div>` : ''}
        <div class="fact">
          <span class="fact__icon">${icon('user', 16)}</span>
          <div><div class="fact__label">Managed by</div>
          <div class="fact__value">${escapeHtml(business.ownerName)}</div></div>
        </div>
      </div>
    </div>

    ${business.events?.length ? `
      <div class="section-head"><h2>What's on here</h2></div>
      <div class="stack">
        ${business.events.map((event) => `
          <div class="list-card" style="grid-template-columns:1fr auto">
            <div>
              <div class="list-card__title">${escapeHtml(event.title)}</div>
              <div class="list-card__meta"><span>${icon('calendar', 12)}
                ${escapeHtml(formatDateTime(event.eventDateTime))}</span></div>
            </div>
            <span class="badge ${event.free ? 'badge--green' : 'badge--gold'}">
              ${event.free ? 'Free' : `R${event.ticketPrice}`}</span>
          </div>`).join('')}
      </div>` : ''}

    <div class="card" style="margin-top:16px" id="reviews">
      <div class="row row--between">
        <strong>Reviews</strong>
        ${business.myReview
          ? `<button class="btn btn--sm" type="button" id="review-button">
               ${icon('star', 14)} Edit your review</button>`
          : business.canReview
            ? `<button class="btn btn--sm" type="button" id="review-button">
                 ${icon('star', 14)} Leave a review</button>`
            : `<a class="badge" href="#/signin">${session.isSignedIn
                 ? 'Owners and admins cannot review' : 'Sign in to review'}</a>`}
      </div>

      ${business.reviewCount ? `
        <div class="rating-summary" style="margin-top:14px">
          <div>
            <div class="rating-summary__score">${business.rating.toFixed(1)}</div>
            <div class="stars" style="margin-top:6px">${stars(business.rating, 14)}</div>
            <div class="small muted" style="margin-top:4px">${business.reviewCount} review${business.reviewCount === 1 ? '' : 's'}</div>
          </div>
          <div class="rating-bars">
            ${['5', '4', '3', '2', '1'].map((score) => {
              const count = business.ratingDistribution?.[score] || 0;
              const width = business.reviewCount ? (count / business.reviewCount) * 100 : 0;
              return `<div class="rating-bar">
                <span>${score}★</span>
                <span class="rating-bar__track"><span class="rating-bar__fill" style="width:${width}%"></span></span>
                <span>${count}</span>
              </div>`;
            }).join('')}
          </div>
        </div>` : `<p class="muted small" style="margin-top:10px">No reviews yet — be the first.</p>`}

      <div style="margin-top:8px">
        ${(business.reviews || []).map(reviewRow).join('')}
      </div>
    </div>`;

  try {
    mountOpenStreetMap(container.querySelector('#business-location-map'), {
      zone: state.zone,
      businesses: [cardFor(business)],
      focus: business
    });
  } catch (error) {
    container.querySelector('#business-location-map').innerHTML = `<p class="muted small" style="padding:16px">${escapeHtml(error.message)}</p>`;
  }

  container.querySelector('[data-back]').addEventListener('click', () => {
    if (history.length > 1) history.back(); else location.hash = '#/explore';
  });

  container.querySelector('#share-button').addEventListener('click', async () => {
    const url = `${location.origin}/#/business/${business.id}`;
    try {
      if (navigator.share) await navigator.share({ title: business.name, url });
      else { await navigator.clipboard.writeText(url); toast('Link copied.', 'success'); }
    } catch { /* the visitor dismissed the sheet */ }
  });

  // Editing an existing review is allowed once every 24 hours (server-enforced).
  container.querySelector('#review-button')?.addEventListener('click', () => openReviewForm(business, container));
}

function cardFor(business) {
  return {
    id: business.id,
    name: business.name,
    latitude: business.latitude,
    longitude: business.longitude,
    categoryName: business.categoryName
  };
}

function hoursWeek(business) {
  const week = business.hours || [];
  if (!week.length) {
    return `<p class="muted small">${escapeHtml(business.operatingHours || 'Opening hours have not been published yet.')}</p>`;
  }
  return `<div class="hours-week">
    ${week.map((day) => `
      <div class="${day.today ? 'is-today' : ''}">
        <span>${escapeHtml(day.shortDayName)}</span>
        <span>${day.closed ? '<span class="muted">Closed</span>'
          : `${escapeHtml(day.openTime || '')} – ${escapeHtml(day.closeTime || '')}`}</span>
      </div>`).join('')}
  </div>`;
}

function reviewRow(review) {
  return `
    <div class="review">
      <div class="review__head">
        <span class="avatar avatar--sm">${escapeHtml(review.initials || initials(review.authorName))}</span>
        <div style="flex:1">
          <div class="review__author">${escapeHtml(review.authorName)}
            ${review.mine ? '<span class="badge badge--violet" style="margin-left:6px">You</span>' : ''}</div>
          <div class="row" style="gap:8px;margin-top:2px">
            <span class="stars">${stars(review.rating, 12)}</span>
            <span class="review__date">${escapeHtml(relativeTime(review.createdAt))}</span>
            ${review.verified ? `<span class="badge badge--green">${icon('checkCircle', 11)} Verified</span>` : ''}
          </div>
        </div>
      </div>
      ${review.comment ? `<p class="review__body">${escapeHtml(review.comment)}</p>` : ''}
    </div>`;
}

/* ------------------------------------------------------------ review form */

function openReviewForm(business, container) {
  if (!session.isSignedIn) { location.hash = '#/signin?next=1'; return; }

  let rating = business.myReview?.rating || 0;

  const body = `
    <div class="stack">
      <p class="small muted">One review per person per business. A review can be edited once every 24 hours.</p>
      <div class="star-picker" id="star-picker" role="radiogroup" aria-label="Star rating">
        ${[1, 2, 3, 4, 5].map((value) => `
          <button type="button" role="radio" aria-checked="${value === rating}" data-stars="${value}"
                  class="${value <= rating ? 'is-on' : ''}" aria-label="${value} star${value === 1 ? '' : 's'}">
            ${icon('star', 30)}
          </button>`).join('')}
      </div>
      <div class="field">
        <label for="review-comment">Your review (optional)</label>
        <textarea id="review-comment" maxlength="1000"
          placeholder="What should a student know before they go?">${escapeHtml(business.myReview?.comment || '')}</textarea>
      </div>
      <p class="field-error" id="review-error" hidden></p>
    </div>`;

  openModal({
    title: business.myReview ? 'Edit your review' : `Review ${business.name}`,
    body,
    footer: `
      <button class="btn btn--ghost" type="button" data-modal-close>Cancel</button>
      <button class="btn" type="button" id="review-submit">Post review</button>`,
    onMount(modal) {
      const picker = modal.querySelector('#star-picker');
      const error = modal.querySelector('#review-error');

      picker.addEventListener('click', (event) => {
        const button = event.target.closest('[data-stars]');
        if (!button) return;
        rating = Number(button.dataset.stars);
        picker.querySelectorAll('[data-stars]').forEach((node) => {
          const on = Number(node.dataset.stars) <= rating;
          node.classList.toggle('is-on', on);
          node.setAttribute('aria-checked', String(Number(node.dataset.stars) === rating));
        });
      });

      modal.querySelector('#review-submit').addEventListener('click', async () => {
        error.hidden = true;
        if (!rating) { error.hidden = false; error.textContent = 'Choose a star rating first.'; return; }

        const button = modal.querySelector('#review-submit');
        button.disabled = true;
        button.textContent = 'Posting…';
        try {
          await api.addReview(business.id, {
            rating,
            comment: modal.querySelector('#review-comment').value.trim() || null
          });
          closeModal();
          toast('Thanks — your review is live.', 'success');
          renderBusiness(container, business.id);
        } catch (failure) {
          error.hidden = false;
          error.textContent = failure.message;
        } finally {
          button.disabled = false;
          button.textContent = 'Post review';
        }
      });
    }
  });
}
