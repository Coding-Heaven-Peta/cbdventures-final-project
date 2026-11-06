/** Business owner portal (FR-03): create, edit and manage listings and events. */
import { api, session } from '../api.js';
import {
  closeModal, confirmDialog, escapeHtml, emptyState, formatDateTime, icon, openModal,
  statusBadge, toast
} from '../ui.js';
import { setState, state } from '../state.js';
// The dark client shows sign-in as a full screen route, not a modal.
const openAuthModal = (mode = 'signin') => { location.hash = mode === 'register' ? '#/signup' : '#/signin'; };

const COVER_POOL = ['01', '02', '03', '04'];

export async function renderOwner(container) {
  const user = state.user || session.user;

  if (!user) {
    container.innerHTML = gate({
      title: 'List your business on CBDVentures',
      text: 'Business owner accounts can create and edit a verified listing, publish events and respond to students within the 1 km pilot zone.',
      actions: `
        <button class="button" type="button" id="owner-register">${icon('store', 17)} Create a business account</button>
        <button class="button button--ghost" type="button" id="owner-signin">Sign in</button>`
    });
    container.querySelector('#owner-register').addEventListener('click', () => openAuthModal('register', 'OWNER'));
    container.querySelector('#owner-signin').addEventListener('click', () => openAuthModal('signin', 'OWNER'));
    return;
  }

  if (user.role === 'VISITOR') {
    container.innerHTML = gate({
      title: 'This area is for business owners',
      text: 'You are signed in as a student/visitor account. Create a business owner account to add a listing — your reviews stay attached to this account.',
      actions: `
        <button class="button" type="button" id="owner-register">${icon('store', 17)} Create a business account</button>
        <a class="button button--ghost" href="#/">Back to discover</a>`
    });
    container.querySelector('#owner-register').addEventListener('click', () => openAuthModal('register', 'OWNER'));
    return;
  }

  container.innerHTML = `
    <div class="container">
      <header class="dash-head">
        <div>
          <span class="badge badge--violet">${icon('store', 14)} Owner dashboard</span>
          <h1 style="margin-top:10px">Your listings</h1>
          <p class="muted">Only verified listings appear in public search. New and edited listings are reviewed by the CBDVentures team.</p>
        </div>
        <div class="row">
          <button class="button button--ghost" type="button" id="refresh">${icon('compass', 17)} Refresh</button>
          <button class="button" type="button" id="new-listing">${icon('plus', 17)} Add listing</button>
        </div>
      </header>
      <div class="dash-grid" id="owner-stats"></div>
      <div id="owner-list" class="stack" style="margin-top:20px">
        <div class="skeleton" style="height:104px"></div>
      </div>
    </div>`;

  container.querySelector('#refresh').addEventListener('click', () => renderOwner(container));

  let meta = state.filters;
  if (!meta) {
    try {
      meta = (await api.filters()).data;
      setState({ filters: meta, zone: meta.zone });
    } catch (error) {
      toast(error.message, 'error');
    }
  }

  container.querySelector('#new-listing').addEventListener('click', () => {
    if (!meta) return;
    openListingForm({ meta, onSaved: () => renderOwner(container) });
  });

  try {
    const { data: listings } = await api.myListings();
    const live = listings.filter((listing) => listing.status === 'ACTIVE').length;
    const pending = listings.filter((listing) => listing.status === 'PENDING').length;
    const reviews = listings.reduce((total, listing) => total + (listing.reviewCount || 0), 0);
    const average = listings.filter((l) => l.reviewCount).length
      ? (listings.filter((l) => l.reviewCount).reduce((sum, l) => sum + l.rating, 0)
        / listings.filter((l) => l.reviewCount).length).toFixed(1)
      : '—';

    container.querySelector('#owner-stats').innerHTML = `
      ${statCard(listings.length, 'Total listings')}
      ${statCard(live, 'Live in the zone')}
      ${statCard(pending, 'Awaiting review')}
      ${statCard(average, 'Average rating')}
      ${statCard(reviews, 'Reviews received')}`;

    const list = container.querySelector('#owner-list');

    if (!listings.length) {
      list.innerHTML = emptyState({
        iconName: 'store',
        title: 'No listings yet',
        text: 'Add your first listing: name, category, address and hours. It takes about two minutes and publishes after verification.',
        action: '<button class="button" type="button" id="empty-add">Add my first listing</button>'
      });
      list.querySelector('#empty-add').addEventListener('click', () => openListingForm({ meta, onSaved: () => renderOwner(container) }));
      return;
    }

    list.innerHTML = listings.map((listing) => `
      <article class="listing-row">
        <img src="${escapeHtml(listing.thumbnail)}" alt="" width="76" height="76">
        <div>
          <div class="row" style="gap:8px">
            <span class="listing-row__title">${escapeHtml(listing.name)}</span>
            ${statusBadge(listing.status)}
          </div>
          <div class="muted small" style="margin-top:3px">
            ${escapeHtml(listing.categoryName)} · ${escapeHtml(listing.priceLevel)} ·
            ${escapeHtml(listing.address)}
          </div>
          <div class="muted small" style="margin-top:3px">
            ${listing.reviewCount ? `${listing.rating.toFixed(1)}★ from ${listing.reviewCount} reviews` : 'No reviews yet'}
            ${listing.operatingHours ? ` · ${escapeHtml(listing.operatingHours)}` : ''}
          </div>
        </div>
        <div class="listing-row__actions">
          <a class="button button--ghost button--sm" href="#/business/${listing.id}">View</a>
          <button class="button button--ghost button--sm" type="button" data-edit="${listing.id}">${icon('edit', 15)} Edit</button>
          <button class="button button--ghost button--sm" type="button" data-events="${listing.id}">${icon('calendar', 15)} Events</button>
          <button class="icon-button icon-button--sm" type="button" data-delete="${listing.id}"
            aria-label="Delete ${escapeHtml(listing.name)}">${icon('trash', 15)}</button>
        </div>
      </article>`).join('');

    list.querySelectorAll('[data-edit]').forEach((button) => {
      button.addEventListener('click', () => {
        const listing = listings.find((item) => String(item.id) === button.dataset.edit);
        if (listing && meta) openListingForm({ meta, listing, onSaved: () => renderOwner(container) });
      });
    });

    list.querySelectorAll('[data-events]').forEach((button) => {
      button.addEventListener('click', () => {
        const listing = listings.find((item) => String(item.id) === button.dataset.events);
        if (listing) openEventsManager(listing, () => renderOwner(container));
      });
    });

    list.querySelectorAll('[data-delete]').forEach((button) => {
      button.addEventListener('click', async () => {
        const ok = await confirmDialog({
          title: 'Remove this listing?',
          message: 'The listing, its events and its reviews will be permanently removed from CBDVentures.',
          confirmLabel: 'Remove listing',
          danger: true
        });
        if (!ok) return;
        try {
          await api.deleteListing(button.dataset.delete);
          toast('Listing removed.', 'success');
          renderOwner(container);
        } catch (error) {
          toast(error.message, 'error');
        }
      });
    });
  } catch (error) {
    container.querySelector('#owner-list').innerHTML = emptyState({
      iconName: 'alert', title: 'Could not load your listings', text: error.message
    });
  }
}

function statCard(value, label) {
  return `<div class="stat-card"><strong>${escapeHtml(value)}</strong><span>${escapeHtml(label)}</span></div>`;
}

function gate({ title, text, actions }) {
  return `<div class="container section">
    <div class="panel" style="max-width:620px;margin:34px auto;display:grid;gap:14px;justify-items:center;text-align:center">
      <span class="empty__icon">${icon('store', 26)}</span>
      <h1 style="font-size:1.6rem">${escapeHtml(title)}</h1>
      <p class="muted">${escapeHtml(text)}</p>
      <div class="row" style="justify-content:center">${actions}</div>
    </div>
  </div>`;
}

/* --------------------------------------------------------------- listing form */

export function openListingForm({ meta, listing = null, onSaved }) {
  const zone = meta.zone || state.zone;
  const amenities = meta.amenities || [];
  const selectedAmenities = new Set();
  const images = listing ? [] : [];

  const body = `
    <form class="form-grid" id="listing-form" novalidate>
      <div class="form-grid form-grid--two">
        <div class="field">
          <label for="listing-name">Business name *</label>
          <input id="listing-name" name="name" required maxlength="120" value="${escapeHtml(listing?.name || '')}"
                 placeholder="e.g. Kota Kingdom">
        </div>
        <div class="field">
          <label for="listing-category">Category *</label>
          <select id="listing-category" name="categoryId" required>
            ${meta.categories.map((category) => `
              <option value="${category.id}" ${listing?.categoryId === category.id ? 'selected' : ''}>
                ${escapeHtml(category.name)}</option>`).join('')}
          </select>
        </div>
      </div>

      <div class="field">
        <label for="listing-address">Street address *</label>
        <input id="listing-address" name="address" required maxlength="255"
               value="${escapeHtml(listing?.address || '')}" placeholder="e.g. 268 Pretorius Street, Pretoria Central">
        <span class="field__hint">Must be inside the ${zone.primaryRadiusKm} km pilot zone around Richfield.</span>
      </div>

      <div class="form-grid form-grid--two">
        <div class="field">
          <label for="listing-lat">Latitude *</label>
          <input id="listing-lat" name="latitude" type="number" step="0.000001" required
                 value="${listing?.latitude ?? zone.anchorLatitude.toFixed(6)}">
        </div>
        <div class="field">
          <label for="listing-lng">Longitude *</label>
          <input id="listing-lng" name="longitude" type="number" step="0.000001" required
                 value="${listing?.longitude ?? zone.anchorLongitude.toFixed(6)}">
        </div>
      </div>
      <div id="zone-feedback" class="notice notice--info">
        Distances are measured from the Richfield campus anchor. Coordinates inside 1 km publish immediately after review.
      </div>

      <div class="field">
        <label for="listing-description">Description</label>
        <textarea id="listing-description" name="description" maxlength="3000"
          placeholder="What do you sell, what should students know?">${escapeHtml(listing?.description || '')}</textarea>
      </div>

      <div class="form-grid form-grid--two">
        <div class="field">
          <label for="listing-price">Price range *</label>
          <select id="listing-price" name="priceLevel" required>
            <option value="R" ${listing?.priceLevel === 'R' ? 'selected' : ''}>R — budget (under R80)</option>
            <option value="RR" ${listing?.priceLevel === 'RR' ? 'selected' : ''}>RR — mid (R80–R180)</option>
            <option value="RRR" ${listing?.priceLevel === 'RRR' ? 'selected' : ''}>RRR — premium (R180+)</option>
          </select>
        </div>
        <div class="field">
          <label for="listing-hours">Hours summary (optional)</label>
          <input id="listing-hours" name="operatingHours" maxlength="120"
                 value="${escapeHtml(listing?.operatingHours || '')}" placeholder="Mon-Sat 07:00 - 17:00">
        </div>
      </div>

      <fieldset class="field" style="border:0;padding:0;margin:0 0 18px">
        <legend style="font-weight:650;margin-bottom:4px">Opening hours per day</legend>
        <span class="field__hint" style="display:block;margin-bottom:10px">
          Tick “Closed” for days you do not trade. Each open day needs an opening
          and a closing time, and the opening time must be earlier.
        </span>
        <div class="hours-editor">
          ${['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
            .map((day, index) => `
              <div class="hours-row" data-day="${index + 1}">
                <span class="hours-row__day">${day.slice(0, 3)}</span>
                <input type="time" data-hours-open aria-label="${day} opening time">
                <span class="hours-row__dash">–</span>
                <input type="time" data-hours-close aria-label="${day} closing time">
                <label class="hours-row__closed">
                  <input type="checkbox" data-hours-closed> Closed
                </label>
              </div>`).join('')}
        </div>
      </fieldset>

      <div class="form-grid form-grid--two">
        <div class="field">
          <label for="listing-open">Default opening time</label>
          <input id="listing-open" name="openTime" type="time">
          <span class="field__hint">Only used when no per-day hours are given.</span>
        </div>
        <div class="field">
          <label for="listing-close">Default closing time</label>
          <input id="listing-close" name="closeTime" type="time">
        </div>
      </div>

      <div class="form-grid form-grid--two">
        <div class="field">
          <label for="listing-phone">Phone</label>
          <input id="listing-phone" name="phone" maxlength="40" value="${escapeHtml(listing?.phone || '')}"
                 placeholder="072 445 1180">
        </div>
        <div class="field">
          <label for="listing-whatsapp">WhatsApp</label>
          <input id="listing-whatsapp" name="whatsapp" maxlength="40" value="${escapeHtml(listing?.whatsapp || '')}"
                 placeholder="072 445 1180">
        </div>
      </div>

      <div class="field">
        <label for="listing-website">Website</label>
        <input id="listing-website" name="website" type="url" maxlength="200"
               value="${escapeHtml(listing?.website || '')}" placeholder="https://example.co.za">
      </div>

      <div class="field">
        <label>Amenities</label>
        <div class="filter-options" id="amenity-picker">
          ${amenities.map((amenity) => `
            <button type="button" class="chip" data-amenity="${amenity.id}">${escapeHtml(amenity.name)}</button>`).join('')}
        </div>
      </div>

      <div class="field">
        <label for="listing-images">Photos (one URL per line)</label>
        <textarea id="listing-images" name="imageUrls" placeholder="/images/covers/food-drinks-01.svg"></textarea>
        <span class="field__hint">The first photo becomes the card thumbnail. Pick a sample below or paste your own URLs.</span>
        <div class="filter-options" id="cover-picker" style="margin-top:6px"></div>
      </div>

      <p class="notice notice--error" id="listing-error" hidden></p>
    </form>`;

  openModal({
    title: listing ? `Edit ${listing.name}` : 'Add a listing',
    wide: true,
    body,
    footer: `
      <button class="button button--ghost" type="button" data-modal-close>Cancel</button>
      <button class="button" type="submit" form="listing-form" id="listing-submit">
        ${listing ? 'Save changes' : 'Submit for review'}
      </button>`,
    onMount(modal) {
      const form = modal.querySelector('#listing-form');
      const error = modal.querySelector('#listing-error');
      const submit = modal.querySelector('#listing-submit');

      if (listing?.openTime) modal.querySelector('#listing-open').value = listing.openTime.slice(0, 5);
      if (listing?.closeTime) modal.querySelector('#listing-close').value = listing.closeTime.slice(0, 5);

      // Per-day hours: preload the saved week, otherwise prefill weekdays from the
      // single default window so an owner can adjust instead of retyping.
      const week = new Map((listing?.hours || []).map((row) => [row.dayOfWeek, row]));
      modal.querySelectorAll('.hours-row').forEach((row) => {
        const day = Number(row.dataset.day);
        const open = row.querySelector('[data-hours-open]');
        const close = row.querySelector('[data-hours-close]');
        const closed = row.querySelector('[data-hours-closed]');
        const saved = week.get(day);
        if (saved) {
          closed.checked = Boolean(saved.closed);
          if (saved.openTime) open.value = saved.openTime.slice(0, 5);
          if (saved.closeTime) close.value = saved.closeTime.slice(0, 5);
        } else if (!listing && day <= 5) {
          open.value = listing?.openTime?.slice(0, 5) || '08:00';
          close.value = listing?.closeTime?.slice(0, 5) || '17:00';
        }
        const sync = () => {
          open.disabled = closed.checked;
          close.disabled = closed.checked;
          row.classList.toggle('is-closed', closed.checked);
        };
        closed.addEventListener('change', sync);
        sync();
      });

      if (listing?.images?.length) {
        modal.querySelector('#listing-images').value = listing.images.map((image) => image.url).join('\n');
      }

      modal.querySelectorAll('[data-amenity]').forEach((button) => {
        const id = Number(button.dataset.amenity);
        const owned = listing?.amenities?.some((amenity) => amenity.id === id);
        if (owned) {
          selectedAmenities.add(id);
          button.classList.add('is-active');
        }
        button.addEventListener('click', () => {
          if (selectedAmenities.has(id)) {
            selectedAmenities.delete(id);
            button.classList.remove('is-active');
          } else {
            selectedAmenities.add(id);
            button.classList.add('is-active');
          }
        });
      });

      const categorySelect = modal.querySelector('#listing-category');
      const coverPicker = modal.querySelector('#cover-picker');
      const imagesInput = modal.querySelector('#listing-images');

      const renderCovers = () => {
        const category = meta.categories.find((item) => String(item.id) === categorySelect.value);
        const slug = category?.slug || 'food-drinks';
        coverPicker.innerHTML = COVER_POOL.map((index) => {
          const url = `/images/covers/${slug}-${index}.svg`;
          return `<button type="button" class="chip" data-cover="${url}">
            ${icon('plus', 14)} Sample ${index}</button>`;
        }).join('');
        coverPicker.querySelectorAll('[data-cover]').forEach((button) => {
          button.addEventListener('click', () => {
            const current = imagesInput.value.trim();
            imagesInput.value = current ? `${current}\n${button.dataset.cover}` : button.dataset.cover;
          });
        });
      };
      renderCovers();
      categorySelect.addEventListener('change', renderCovers);

      const latInput = modal.querySelector('#listing-lat');
      const lngInput = modal.querySelector('#listing-lng');
      const feedback = modal.querySelector('#zone-feedback');

      const updateZoneFeedback = () => {
        const lat = Number(latInput.value);
        const lng = Number(lngInput.value);
        if (!lat || !lng) return;
        const metresPerDegreeLat = 111320;
        const metresPerDegreeLng = 111320 * Math.cos((zone.anchorLatitude * Math.PI) / 180);
        const north = (lat - zone.anchorLatitude) * metresPerDegreeLat;
        const east = (lng - zone.anchorLongitude) * metresPerDegreeLng;
        const km = Math.sqrt(north * north + east * east) / 1000;
        feedback.className = km <= zone.primaryRadiusKm
          ? 'notice notice--info'
          : km <= zone.extendedRadiusKm
            ? 'notice'
            : 'notice notice--error';
        feedback.textContent = km <= zone.primaryRadiusKm
          ? `Inside the 1 km pilot zone (${(km * 1000).toFixed(0)} m from Richfield). This listing can publish after review.`
          : km <= zone.extendedRadiusKm
            ? `${km.toFixed(2)} km from Richfield — inside the future 5 km zone, but it stays hidden until the extended zone opens.`
            : `${km.toFixed(1)} km from Richfield — outside the 5 km boundary. Move the pin closer to the CBD to save.`;
      };
      latInput.addEventListener('input', updateZoneFeedback);
      lngInput.addEventListener('input', updateZoneFeedback);
      updateZoneFeedback();

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        error.hidden = true;

        const data = new FormData(form);
        const payload = {
          name: (data.get('name') || '').trim(),
          description: (data.get('description') || '').trim() || null,
          address: (data.get('address') || '').trim(),
          latitude: Number(data.get('latitude')),
          longitude: Number(data.get('longitude')),
          priceLevel: data.get('priceLevel'),
          categoryId: Number(data.get('categoryId')),
          operatingHours: (data.get('operatingHours') || '').trim() || null,
          openTime: data.get('openTime') || null,
          closeTime: data.get('closeTime') || null,
          hours: [...modal.querySelectorAll('.hours-row')].map((row) => ({
            dayOfWeek: Number(row.dataset.day),
            closed: row.querySelector('[data-hours-closed]').checked,
            openTime: row.querySelector('[data-hours-open]').value || null,
            closeTime: row.querySelector('[data-hours-close]').value || null
          })).filter((row) => row.closed || (row.openTime && row.closeTime)),
          phone: (data.get('phone') || '').trim() || null,
          whatsapp: (data.get('whatsapp') || '').trim() || null,
          website: (data.get('website') || '').trim() || null,
          amenityIds: [...selectedAmenities],
          imageUrls: (data.get('imageUrls') || '').split('\n').map((url) => url.trim()).filter(Boolean),
          reactivate: true
        };

        if (!payload.name || !payload.address || !payload.categoryId) {
          error.hidden = false;
          error.textContent = 'Name, address and category are required.';
          return;
        }

        submit.disabled = true;
        submit.innerHTML = '<span class="spinner"></span>';
        try {
          if (listing) {
            await api.updateListing(listing.id, payload);
            toast('Listing updated and sent for verification.', 'success');
          } else {
            await api.createListing(payload);
            toast('Listing submitted. The team usually verifies within a day.', 'success');
          }
          closeModal();
          onSaved();
        } catch (err) {
          error.hidden = false;
          error.textContent = err.message;
          submit.disabled = false;
          submit.textContent = listing ? 'Save changes' : 'Submit for review';
        }
      });
    }
  });
}

/* --------------------------------------------------------------- events manager */

async function openEventsManager(listing, onDone) {
  openModal({
    title: `Events at ${listing.name}`,
    wide: true,
    body: `<div id="events-body"><div class="skeleton" style="height:120px"></div></div>`,
    footer: `<button class="button button--ghost" type="button" data-modal-close>Close</button>`,
    onMount(modal) {
      const body = modal.querySelector('#events-body');

      const load = async () => {
        body.innerHTML = '<div class="skeleton" style="height:120px"></div>';
        try {
          const { data: events } = await api.listingEvents(listing.id);
          body.innerHTML = `
            ${events.length ? `<div class="stack">${events.map((event) => `
              <div class="listing-row" style="grid-template-columns:minmax(0,1fr) auto">
                <div>
                  <div class="listing-row__title">${escapeHtml(event.title)}</div>
                  <div class="muted small">${formatDateTime(event.eventDateTime)} ·
                    ${event.free ? 'Free entry' : 'R' + Number(event.ticketPrice).toFixed(0)}
                    ${event.venue ? ' · ' + escapeHtml(event.venue) : ''}</div>
                </div>
                <button class="icon-button icon-button--sm" type="button" data-remove="${event.id}"
                  aria-label="Delete event">${icon('trash', 15)}</button>
              </div>`).join('')}</div>`
              : `<p class="muted small">No events yet. Add your first performance, market or pop-up below.</p>`}
            <form class="form-grid" id="event-form" style="margin-top:16px">
              <h3 style="font-size:1rem">Add an event</h3>
              <div class="field">
                <label for="event-title">Title *</label>
                <input id="event-title" name="title" required maxlength="100" placeholder="e.g. Live acoustic session">
              </div>
              <div class="field">
                <label for="event-description">Description</label>
                <textarea id="event-description" name="description" maxlength="2000"></textarea>
              </div>
              <div class="form-grid form-grid--two">
                <div class="field">
                  <label for="event-when">Date &amp; time *</label>
                  <input id="event-when" name="eventDateTime" type="datetime-local" required>
                </div>
                <div class="field">
                  <label for="event-price">Ticket price (R)</label>
                  <input id="event-price" name="ticketPrice" type="number" min="0" step="0.01" value="0">
                </div>
              </div>
              <div class="field">
                <label for="event-venue">Room / area in venue</label>
                <input id="event-venue" name="venue" maxlength="100">
              </div>
              <p class="notice notice--error" id="event-error" hidden></p>
              <div class="row"><button class="button" type="submit" id="event-submit">${icon('plus', 16)} Add event</button></div>
            </form>`;

          body.querySelectorAll('[data-remove]').forEach((button) => {
            button.addEventListener('click', async () => {
              const ok = await confirmDialog({
                title: 'Delete this event?', message: 'Visitors will no longer see it on the events calendar.',
                confirmLabel: 'Delete event', danger: true
              });
              if (!ok) return;
              try {
                await api.deleteEvent(button.dataset.remove);
                toast('Event deleted.', 'success');
                load();
                onDone();
              } catch (error) {
                toast(error.message, 'error');
              }
            });
          });

          body.querySelector('#event-form').addEventListener('submit', async (event) => {
            event.preventDefault();
            const error = body.querySelector('#event-error');
            const submit = body.querySelector('#event-submit');
            error.hidden = true;
            const form = event.currentTarget;
            const data = new FormData(form);
            const when = data.get('eventDateTime');
            if (!when) {
              error.hidden = false;
              error.textContent = 'Please choose a date and time.';
              return;
            }
            submit.disabled = true;
            submit.innerHTML = '<span class="spinner"></span>';
            try {
              await api.createEvent(listing.id, {
                title: data.get('title'),
                description: data.get('description') || null,
                venue: data.get('venue') || null,
                eventDateTime: when.length === 16 ? `${when}:00` : when,
                ticketPrice: Number(data.get('ticketPrice') || 0)
              });
              toast('Event published.', 'success');
              load();
              onDone();
            } catch (err) {
              error.hidden = false;
              error.textContent = err.message;
              submit.disabled = false;
              submit.innerHTML = `${icon('plus', 16)} Add event`;
            }
          });
        } catch (error) {
          body.innerHTML = `<p class="notice notice--error">${escapeHtml(error.message)}</p>`;
        }
      };

      load();
    }
  });
}
