/** Administrator verification queue (Phase 3 “verification before publishing”). */
import { api, session } from '../api.js';
import { emptyState, escapeHtml, icon, openModal, closeModal, statusBadge, toast } from '../ui.js';
// The dark client shows sign-in as a full screen route, not a modal.
const openAuthModal = (mode = 'signin') => { location.hash = mode === 'register' ? '#/signup' : '#/signin'; };
import { state } from '../state.js';

export async function renderAdmin(container) {
  const user = state.user || session.user;

  if (!user || user.role !== 'ADMIN') {
    container.innerHTML = `<div class="container section">
      <div class="panel center" style="max-width:520px;margin:40px auto;display:grid;gap:12px;justify-items:center">
        <span class="empty__icon">${icon('shield', 26)}</span>
        <h1 style="font-size:1.5rem">Administrator access only</h1>
        <p class="muted">Sign in with an administrator account to review and verify listings.</p>
        <div class="row" style="justify-content:center;flex-wrap:wrap">
          <button class="button" type="button" id="admin-signin">${icon('user', 17)} Sign in as administrator</button>
          <a class="button button--ghost" href="#/">Back to discover</a>
        </div>
        <p class="muted" style="font-size:.85rem">Demo administrator: admin@cbdventures.co.za · Admin123!</p>
      </div>
    </div>`;
    container.querySelector('#admin-signin')
      .addEventListener('click', () => openAuthModal('signin'));
    return;
  }

  container.innerHTML = `
    <div class="container">
      <header class="dash-head">
        <div>
          <span class="badge badge--violet">${icon('shield', 14)} Administrator</span>
          <h1 style="margin-top:10px">Verification queue</h1>
          <p class="muted">Approve listings that are complete and inside the pilot zone. Rejected owners can fix and resubmit.</p>
        </div>
        <div class="row" style="flex-wrap:wrap;justify-content:flex-end">
          <a class="button button--ghost button--sm" href="#/reports">${icon('chart', 15)} Reports</a>
          <a class="button button--ghost button--sm" href="#/backup">${icon('database', 15)} System backup</a>
        </div>
      </header>
      <div class="segmented" id="status-tabs" style="margin-bottom:16px">
        <button type="button" data-status="PENDING" class="is-active">Pending</button>
        <button type="button" data-status="ACTIVE">Live</button>
        <button type="button" data-status="REJECTED">Rejected</button>
      </div>
      <div class="dash-grid" id="admin-stats"></div>
      <div id="queue" class="stack" style="margin-top:20px"><div class="skeleton" style="height:104px"></div></div>
    </div>`;

  let status = 'PENDING';

  try {
    const { data: stats } = await api.adminStats();
    container.querySelector('#admin-stats').innerHTML = `
      ${card(stats.totalBusinesses, 'Listings')}
      ${card(stats.activeBusinesses, 'Live')}
      ${card(stats.pendingBusinesses, 'Awaiting review')}
      ${card(stats.reviews, 'Reviews')}
      ${card(stats.users, 'Accounts')}
      ${card(stats.averageRating ? stats.averageRating.toFixed(1) + '★' : '—', 'Average rating')}`;
  } catch (error) {
    container.querySelector('#admin-stats').innerHTML = `<p class="notice notice--error">${escapeHtml(error.message)}</p>`;
  }

  const queue = container.querySelector('#queue');

  const load = async () => {
    queue.innerHTML = '<div class="skeleton" style="height:104px"></div>';
    try {
      const { data: listings } = await api.adminListings(status);
      if (!listings.length) {
        queue.innerHTML = emptyState({
          iconName: 'check',
          title: status === 'PENDING' ? 'Queue is clear' : 'Nothing here',
          text: status === 'PENDING'
            ? 'Every submitted listing has been reviewed. New submissions will appear here.'
            : 'No listings with this status right now.'
        });
        return;
      }

      queue.innerHTML = listings.map((listing) => `
        <article class="listing-row">
          <img src="${escapeHtml(listing.thumbnail)}" alt="" width="76" height="76">
          <div>
            <div class="row" style="gap:8px">
              <span class="listing-row__title">${escapeHtml(listing.name)}</span>
              ${statusBadge(listing.status)}
              <span class="badge badge--violet">${escapeHtml(listing.categoryName)}</span>
            </div>
            <div class="muted small" style="margin-top:3px">${escapeHtml(listing.address)}</div>
            <div class="muted small" style="margin-top:3px">
              ${escapeHtml(listing.priceLevel)} · ${listing.distanceKm != null
                ? (listing.distanceKm <= 1
                    ? 'inside 1 km zone (' + Math.round(listing.distanceKm * 1000) + ' m from Richfield)'
                    : listing.distanceKm.toFixed(1) + ' km from Richfield — outside the pilot zone')
                : 'distance unknown'}
              ${listing.operatingHours ? ' · ' + escapeHtml(listing.operatingHours) : ''}
            </div>
          </div>
          <div class="listing-row__actions">
            <a class="button button--ghost button--sm" href="#/business/${listing.id}">Open profile</a>
            ${listing.status !== 'ACTIVE'
              ? `<button class="button button--sm" type="button" data-approve="${listing.id}">${icon('check', 15)} Approve</button>` : ''}
            ${listing.status !== 'REJECTED'
              ? `<button class="button button--ghost button--sm" type="button" data-reject="${listing.id}">${icon('close', 15)} Reject</button>` : ''}
          </div>
        </article>`).join('');

      queue.querySelectorAll('[data-approve]').forEach((button) => {
        button.addEventListener('click', () => decide(button.dataset.approve, 'approve', load));
      });
      queue.querySelectorAll('[data-reject]').forEach((button) => {
        button.addEventListener('click', () => decide(button.dataset.reject, 'reject', load));
      });
    } catch (error) {
      queue.innerHTML = emptyState({ iconName: 'alert', title: 'Could not load the queue', text: error.message });
    }
  };

  container.querySelectorAll('[data-status]').forEach((button) => {
    button.addEventListener('click', () => {
      status = button.dataset.status;
      container.querySelectorAll('[data-status]').forEach((other) =>
        other.classList.toggle('is-active', other === button));
      load();
    });
  });

  load();
}

function decide(id, action, onDone) {
  openModal({
    title: action === 'approve' ? 'Approve listing' : 'Reject listing',
    body: `
      <p class="muted">${action === 'approve'
        ? 'Approved listings appear in public search immediately.'
        : 'The owner will see your note and can update and resubmit the listing.'}</p>
      <div class="field">
        <label for="decision-note">Note to the owner (optional)</label>
        <textarea id="decision-note" maxlength="255"
          placeholder="${action === 'approve' ? 'Verified in person on 26 October.' : 'Please add operating hours and a photo.'}"></textarea>
      </div>`,
    footer: `
      <button class="button button--ghost" type="button" data-modal-close>Cancel</button>
      <button class="button ${action === 'reject' ? 'button--danger' : ''}" type="button" id="confirm-decision">
        ${action === 'approve' ? 'Approve listing' : 'Reject listing'}
      </button>`,
    onMount(modal) {
      modal.querySelector('#confirm-decision').addEventListener('click', async () => {
        const note = modal.querySelector('#decision-note').value.trim();
        try {
          if (action === 'approve') await api.approve(id, note);
          else await api.reject(id, note);
          closeModal();
          toast(action === 'approve' ? 'Listing approved and published.' : 'Listing rejected.', 'success');
          onDone();
        } catch (error) {
          toast(error.message, 'error');
        }
      });
    }
  });
}

function card(value, label) {
  return `<div class="stat-card"><strong>${escapeHtml(value)}</strong><span>${escapeHtml(label)}</span></div>`;
}
