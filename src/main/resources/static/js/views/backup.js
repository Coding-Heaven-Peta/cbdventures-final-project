/**
 * System backup and restore (Phase 3 deliverable "System Backup").
 *
 * Administrator screen that
 *  - shows what the current database holds,
 *  - downloads a single JSON backup of every table,
 *  - restores a previously downloaded backup (with an explicit confirmation).
 */
import { api, session } from '../api.js';
import { escapeHtml, icon, openModal, closeModal, toast } from '../ui.js';
// The dark client shows sign-in as a full screen route, not a modal.
const openAuthModal = (mode = 'signin') => { location.hash = mode === 'register' ? '#/signup' : '#/signin'; };
import { state, setState } from '../state.js';

export async function renderBackup(container) {
  const user = state.user || session.user;

  if (!user || user.role !== 'ADMIN') {
    container.innerHTML = `<div class="container section">
      <div class="panel center" style="max-width:520px;margin:40px auto;display:grid;gap:12px;justify-items:center">
        <span class="empty__icon">${icon('shield', 26)}</span>
        <h1 style="font-size:1.5rem">Administrator access only</h1>
        <p class="muted">Backups contain every account and listing, so only an administrator can download or restore one.</p>
        <div class="row" style="justify-content:center;flex-wrap:wrap">
          <button class="button" type="button" id="backup-signin">${icon('user', 17)} Sign in as administrator</button>
          <a class="button button--ghost" href="#/">Back to discover</a>
        </div>
        <p class="muted" style="font-size:.85rem">Demo administrator: admin@cbdventures.co.za · Admin123!</p>
      </div>
    </div>`;
    container.querySelector('#backup-signin')
      .addEventListener('click', () => openAuthModal('signin'));
    return;
  }

  container.innerHTML = `
    <div class="container">
      <header class="dash-head">
        <div>
          <span class="badge badge--violet">${icon('database', 14)} System backup</span>
          <h1 style="margin-top:10px">Backup &amp; restore</h1>
          <p class="muted">Download every table as one JSON file, and restore it on the same or another machine. The database structure itself is created from <code>db/schema_mysql.sql</code>.</p>
        </div>
        <div class="row">
          <a class="button button--ghost button--sm" href="#/admin">${icon('shield', 15)} Verification queue</a>
          <a class="button button--ghost button--sm" href="#/reports">${icon('chart', 15)} Reports</a>
        </div>
      </header>

      <div class="dash-grid" id="backup-summary">
        <div class="skeleton" style="height:96px"></div>
      </div>

      <div class="split-2" style="margin-top:22px">
        <section class="panel stack">
          <h2>${icon('download', 18)} Create a backup</h2>
          <p class="muted">Exports categories, amenities, accounts (with hashed passwords), listings, images, events, reviews and saved preferences into a single <code>.json</code> file dated today.</p>
          <div class="notice notice--info" style="font-size:.88rem">
            Keep the file safe: it contains account records. Treat it as personal information under POPIA.
          </div>
          <div class="row">
            <button class="button" type="button" id="download-backup">${icon('download', 16)} Download backup</button>
          </div>
          <p class="muted" style="font-size:.82rem" id="backup-status">Last backup this session: none</p>
        </section>

        <section class="panel stack">
          <h2>${icon('upload', 18)} Restore from a backup</h2>
          <p class="muted">Choose a backup file produced by the button on the left. The current contents are replaced and every user is signed out (restored accounts get new internal ids). Download a fresh backup first — this cannot be undone.</p>
          <form id="restore-form" class="stack" style="gap:12px">
            <label class="field">
              <span class="field__label">Backup file (.json)</span>
              <input type="file" id="restore-file" accept="application/json,.json" required>
            </label>
            <div class="row">
              <button class="button button--danger" type="submit" id="restore-button">${icon('upload', 16)} Restore backup</button>
            </div>
          </form>
          <p class="muted" style="font-size:.82rem">Restores run in a single transaction: if any row is invalid, nothing is changed.</p>
        </section>
      </div>

      <section class="panel stack" style="margin-top:22px">
        <h2>${icon('info', 18)} Other backup options</h2>
        <div class="table-scroll">
          <table class="data-table">
            <thead><tr><th scope="col">Method</th><th scope="col">Command</th><th scope="col">When to use</th></tr></thead>
            <tbody>
              <tr><td>JSON backup (this screen)</td><td><code>GET /api/admin/backup</code></td><td>Moving demo data between machines or marking a submission snapshot</td></tr>
              <tr><td>MySQL dump</td><td><code>mysqldump -u root -p cbdventures &gt; cbdventures.sql</code></td><td>Production profile — schedule nightly with cron</td></tr>
              <tr><td>MySQL restore</td><td><code>mysql -u root -p cbdventures &lt; cbdventures.sql</code></td><td>Recovering after a failure</td></tr>
              <tr><td>H2 file copy</td><td><code>copy data/cbdventures.mv.db</code></td><td>Demo profile — stop the server first</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>`;

  const summary = container.querySelector('#backup-summary');
  const status = container.querySelector('#backup-status');

  async function loadSummary() {
    try {
      const { data } = await api.backupSummary();
      summary.innerHTML = [
        card(data.businesses, 'Listings'),
        card(data.reviews, 'Reviews'),
        card(data.users, 'Accounts'),
        card(data.events, 'Events'),
        card(data.categories, 'Categories'),
        card(data.images, 'Images')
      ].join('');
    } catch (error) {
      summary.innerHTML = `<p class="notice notice--error">${escapeHtml(error.message)}</p>`;
    }
  }

  container.querySelector('#download-backup').addEventListener('click', async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    try {
      const { data, fileName } = await api.downloadBackup();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(link.href);
      status.textContent = `Last backup this session: ${new Date().toLocaleString('en-ZA')} (${fileName})`;
      toast('Backup downloaded.', 'success');
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      button.disabled = false;
    }
  });

  container.querySelector('#restore-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const input = container.querySelector('#restore-file');
    const file = input.files?.[0];
    if (!file) {
      toast('Choose a backup file first.', 'error');
      return;
    }
    let text;
    try {
      text = await file.text();
    } catch (error) {
      toast('That file could not be read.', 'error');
      return;
    }

    openModal({
      title: 'Replace all current data?',
      body: `<p>Restoring <strong>${escapeHtml(file.name)}</strong> deletes the current listings, reviews and accounts before loading the backup.</p>
             <p class="muted">Download a backup first if you want to keep the current data.</p>`,
      footer: `<button class="button button--ghost" type="button" data-modal-close>Cancel</button>
               <button class="button button--danger" type="button" id="confirm-restore">Yes, restore</button>`,
      onMount: (modal) => {
        modal.querySelector('#confirm-restore').addEventListener('click', async () => {
          closeModal();
          try {
            const { data } = await api.restoreBackup(text);
            // The session belongs to an account that no longer exists, so sign
            // out and ask the administrator to sign in against the restored data.
            session.clear();
            setState({ user: null });
            toast(data.message, 'success');
            openModal({
              title: 'Backup restored',
              body: `<p>${escapeHtml(data.message)}</p>
                     <p class="muted">Everyone has been signed out because the restored accounts have new internal ids. Sign in again with your administrator password.</p>`,
              footer: `<button class="button" type="button" id="reload-after-restore">Sign in again</button>`,
              onMount: (modal) => {
                modal.querySelector('#reload-after-restore').addEventListener('click', () => {
                  closeModal();
                  location.hash = '#/';
                  location.reload();
                });
              }
            });
          } catch (error) {
            toast(error.message, 'error');
          }
        });
      }
    });
  });

  await loadSummary();
}

function card(value, label) {
  return `<div class="stat-card">
    <span class="stat-card__value">${escapeHtml(String(value ?? '—'))}</span>
    <span class="stat-card__label">${escapeHtml(label)}</span>
  </div>`;
}
