/**
 * Profile — the account screen, and the door to the owner and administrator
 * modules. The reference design keeps this as the fifth tab.
 */
import { api, session, clearCache } from '../api.js';
import { icon, escapeHtml, toast, openModal, closeModal } from '../ui.js';
import { signOut, setState } from '../state.js';

export async function renderProfile(container) {
  const user = session.user;
  if (!user) { location.hash = '#/signin'; return; }

  const isOwner = session.isOwner;
  const isAdmin = session.isAdmin;

  container.innerHTML = `
    <div class="greet">
      <div>
        <p class="greet__hello">Your account</p>
        <h1 class="greet__title">Profile</h1>
      </div>
    </div>

    <div class="card" style="margin-top:14px">
      <div class="profile-head">
        <span class="avatar avatar--lg">${escapeHtml((user.initials || user.fullName || 'U').slice(0, 2).toUpperCase())}</span>
        <div style="flex:1">
          <div class="profile-head__name">${escapeHtml(user.fullName)}</div>
          <div class="profile-head__mail">${escapeHtml(user.email)}</div>
          <div class="row" style="gap:8px;margin-top:8px">
            <span class="badge badge--violet">${escapeHtml(user.role)}</span>
            ${user.emailVerified === false
              ? '<span class="badge badge--gold">Unverified</span>'
              : `<span class="badge badge--green">${icon('checkCircle', 11)} Verified</span>`}
            ${user.businessCount ? `<span class="badge">${user.businessCount} listing${user.businessCount === 1 ? '' : 's'}</span>` : ''}
          </div>
        </div>
      </div>
    </div>

    <div class="dash-grid">
      <div class="stat-card">
        <span class="stat-card__label">Reviews written</span>
        <span class="stat-card__value" id="stat-reviews">—</span>
      </div>
      <div class="stat-card">
        <span class="stat-card__label">Zone</span>
        <span class="stat-card__value" style="font-size:1rem">1 km</span>
      </div>
    </div>

    <div class="menu-list">
      <a class="menu-item" href="#/explore">
        <span class="menu-item__icon">${icon('search', 18)}</span>
        <span class="menu-item__text"><strong>Browse the zone</strong><small>Filter by price, distance and rating</small></span>
        <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
      </a>

      ${isOwner ? `
        <a class="menu-item" href="#/owner">
          <span class="menu-item__icon">${icon('store', 18)}</span>
          <span class="menu-item__text"><strong>Your listings</strong>
            <small>Create, edit and publish events</small></span>
          <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
        </a>` : `
        <a class="menu-item" href="#/signup">
          <span class="menu-item__icon">${icon('store', 18)}</span>
          <span class="menu-item__text"><strong>List your business</strong>
            <small>Owner accounts publish after verification</small></span>
          <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
        </a>`}

      ${isAdmin ? `
        <a class="menu-item" href="#/admin">
          <span class="menu-item__icon">${icon('shieldCheck', 18)}</span>
          <span class="menu-item__text"><strong>Verification queue</strong><small>Approve or reject submissions</small></span>
          <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
        </a>
        <a class="menu-item" href="#/reports">
          <span class="menu-item__icon">${icon('chart', 18)}</span>
          <span class="menu-item__text"><strong>Reports</strong><small>Five reports with CSV export</small></span>
          <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
        </a>
        <a class="menu-item" href="#/backup">
          <span class="menu-item__icon">${icon('database', 18)}</span>
          <span class="menu-item__text"><strong>System backup</strong><small>Download or restore every table</small></span>
          <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
        </a>` : ''}

      <button class="menu-item" type="button" id="change-password">
        <span class="menu-item__icon">${icon('lock', 18)}</span>
        <span class="menu-item__text"><strong>Change password</strong><small>At least 8 characters with a number</small></span>
        <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
      </button>

      <button class="menu-item" type="button" id="clear-cache">
        <span class="menu-item__icon">${icon('download', 18)}</span>
        <span class="menu-item__text"><strong>Clear offline cache</strong><small>Re-download listings next time you are online</small></span>
        <span class="menu-item__arrow">${icon('chevronRight', 16)}</span>
      </button>

      <button class="menu-item menu-item--danger" type="button" id="sign-out">
        <span class="menu-item__icon">${icon('logout', 18)}</span>
        <span class="menu-item__text"><strong>Sign out</strong><small>Ends this session on the device</small></span>
      </button>
    </div>

    <p class="field__hint" style="margin-top:16px;text-align:center">
      CBDVentures · Group 5 · dark edition build
    </p>`;

  // Review count is derived from the listings the visitor has reviewed.
  try {
    const { data } = await api.businesses({ size: 60 });
    const mine = (data.items || []).filter((business) => business.myReview).length;
    container.querySelector('#stat-reviews').textContent = String(mine);
  } catch { /* the tile stays as a dash when offline */ }

  container.querySelector('#clear-cache').addEventListener('click', () => {
    clearCache();
    toast('Offline cache cleared.', 'success');
  });

  container.querySelector('#sign-out').addEventListener('click', () => {
    openModal({
      title: 'Sign out?',
      body: '<p class="small muted">Saved listings on this device stay cached. You can sign back in at any time.</p>',
      footer: `<button class="btn btn--ghost" type="button" data-modal-close>Stay</button>
               <button class="btn button--danger" type="button" id="confirm-signout">Sign out</button>`,
      onMount(modal) {
        modal.querySelector('#confirm-signout').addEventListener('click', () => {
          signOut();
          setState({ user: null });
          window.dispatchEvent(new Event('cbd:session'));
          closeModal();
          toast('Signed out.');
          location.hash = '#/welcome';
        });
      }
    });
  });

  container.querySelector('#change-password').addEventListener('click', () => openPasswordModal());
}

function openPasswordModal() {
  openModal({
    title: 'Change password',
    body: `
      <div class="stack">
        <div class="field">
          <label for="current-password">Current password</label>
          <input class="input" id="current-password" type="password" autocomplete="current-password">
        </div>
        <div class="field">
          <label for="next-password">New password</label>
          <input class="input" id="next-password" type="password" autocomplete="new-password">
          <span class="field__hint">At least 8 characters and one number.</span>
        </div>
        <p class="field-error" id="password-error" hidden></p>
      </div>`,
    footer: `<button class="btn btn--ghost" type="button" data-modal-close>Cancel</button>
             <button class="btn" type="button" id="save-password">Save</button>`,
    onMount(modal) {
      const error = modal.querySelector('#password-error');
      modal.querySelector('#save-password').addEventListener('click', async () => {
        error.hidden = true;
        const currentPassword = modal.querySelector('#current-password').value;
        const newPassword = modal.querySelector('#next-password').value;
        if (newPassword.length < 8 || !/\d/.test(newPassword)) {
          error.hidden = false;
          error.textContent = 'Use at least 8 characters including one number.';
          return;
        }
        try {
          await api.changePassword({ currentPassword, newPassword });
          closeModal();
          toast('Password updated.', 'success');
        } catch (failure) {
          error.hidden = false;
          error.textContent = failure.message;
        }
      });
    }
  });
}
