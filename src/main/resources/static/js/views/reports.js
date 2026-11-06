/**
 * Administrator reports (Phase 3 deliverable "Reports").
 *
 * Five management reports, each rendered as a generic table from the API and
 * downloadable as CSV so the marker can open them in Excel.
 */
import { api, session } from '../api.js';
import { emptyState, escapeHtml, icon, toast } from '../ui.js';
// The dark client shows sign-in as a full screen route, not a modal.
const openAuthModal = (mode = 'signin') => { location.hash = mode === 'register' ? '#/signup' : '#/signin'; };
import { state } from '../state.js';

export async function renderReports(container) {
  const user = state.user || session.user;

  if (!user || user.role !== 'ADMIN') {
    container.innerHTML = `<div class="container section">
      <div class="panel center" style="max-width:520px;margin:40px auto;display:grid;gap:12px;justify-items:center">
        <span class="empty__icon">${icon('shield', 26)}</span>
        <h1 style="font-size:1.5rem">Administrator access only</h1>
        <p class="muted">Reports summarise every listing, review and event in the pilot zone, so they are restricted to administrators.</p>
        <div class="row" style="justify-content:center;flex-wrap:wrap">
          <button class="button" type="button" id="reports-signin">${icon('user', 17)} Sign in as administrator</button>
          <a class="button button--ghost" href="#/">Back to discover</a>
        </div>
        <p class="muted" style="font-size:.85rem">Demo administrator: admin@cbdventures.co.za · Admin123!</p>
      </div>
    </div>`;
    container.querySelector('#reports-signin')
      .addEventListener('click', () => openAuthModal('signin'));
    return;
  }

  container.innerHTML = `
    <div class="container">
      <header class="dash-head">
        <div>
          <span class="badge badge--violet">${icon('chart', 14)} Reports</span>
          <h1 style="margin-top:10px">Management reports</h1>
          <p class="muted">Five reports covering listings, verification, reviews, owners and events. Download any report as CSV for the project file.</p>
        </div>
        <div class="row">
          <a class="button button--ghost button--sm" href="#/admin">${icon('shield', 15)} Verification queue</a>
          <a class="button button--ghost button--sm" href="#/backup">${icon('database', 15)} System backup</a>
        </div>
      </header>

      <div class="report-picker" id="report-picker" role="tablist" aria-label="Choose a report"></div>
      <div id="report-body" class="stack" style="margin-top:20px">
        <div class="skeleton" style="height:220px"></div>
      </div>
    </div>`;

  const picker = container.querySelector('#report-picker');
  const body = container.querySelector('#report-body');

  let catalogue = [];
  let activeKey = 'listings-by-category';

  try {
    const response = await api.reports();
    catalogue = response.data || [];
    if (catalogue.length && !catalogue.some((r) => r.key === activeKey)) {
      activeKey = catalogue[0].key;
    }
  } catch (error) {
    body.innerHTML = emptyState({
      iconName: 'alert',
      title: 'Reports unavailable',
      text: error.message
    });
    return;
  }

  picker.innerHTML = catalogue.map((report) => `
    <button type="button" role="tab" data-report="${escapeHtml(report.key)}"
            class="report-chip${report.key === activeKey ? ' is-active' : ''}"
            title="${escapeHtml(report.description)}">
      ${escapeHtml(report.title)}
    </button>`).join('');

  picker.querySelectorAll('[data-report]').forEach((button) => {
    button.addEventListener('click', () => {
      activeKey = button.dataset.report;
      picker.querySelectorAll('[data-report]').forEach((other) =>
        other.classList.toggle('is-active', other.dataset.report === activeKey));
      load(activeKey);
    });
  });

  async function load(key) {
    body.innerHTML = '<div class="skeleton" style="height:220px"></div>';
    try {
      const { data } = await api.report(key);
      body.innerHTML = renderReport(data);
      body.querySelector('#download-csv').addEventListener('click', () => downloadCsv(data, key));
      const printButton = body.querySelector('#print-report');
      if (printButton) printButton.addEventListener('click', () => window.print());
    } catch (error) {
      body.innerHTML = emptyState({ iconName: 'alert', title: 'Could not build that report', text: error.message });
    }
  }

  function downloadCsv(report, key) {
    // Same content as GET /api/admin/reports/{key}/csv, fetched with the token
    // so the browser saves it as a file.
    api.reportCsv(key)
      .then(({ text, fileName }) => {
        const blob = new Blob([text], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(link.href);
        toast(`${report.title} downloaded as CSV.`, 'success');
      })
      .catch((error) => toast(error.message, 'error'));
  }

  await load(activeKey);
}

function renderReport(report) {
  const metrics = (report.metrics || []).map((metric) => `
    <div class="stat-card">
      <span class="stat-card__value">${escapeHtml(metric.value)}</span>
      <span class="stat-card__label">${escapeHtml(metric.label)}</span>
    </div>`).join('');

  const rows = (report.rows || []).map((row) => `
    <tr>${row.map((cell) => `<td>${formatCell(cell)}</td>`).join('')}</tr>`).join('');

  return `
    <section class="panel">
      <header class="report-head">
        <div>
          <h2>${escapeHtml(report.title)}</h2>
          <p class="muted">${escapeHtml(report.description)}</p>
          <p class="muted" style="font-size:.85rem">Generated ${escapeHtml(report.generatedAt)}
            · ${report.rows.length} row${report.rows.length === 1 ? '' : 's'}</p>
        </div>
        <div class="row" style="align-content:flex-start">
          <button class="button button--sm" type="button" id="download-csv">
            ${icon('download', 15)} Download CSV
          </button>
          <button class="button button--ghost button--sm" type="button" id="print-report">
            ${icon('printer', 15)} Print
          </button>
        </div>
      </header>

      ${metrics ? `<div class="dash-grid" style="margin:16px 0 4px">${metrics}</div>` : ''}

      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>${report.columns.map((column) => `<th scope="col">${escapeHtml(column)}</th>`).join('')}</tr>
          </thead>
          <tbody>${rows || `<tr><td colspan="${report.columns.length}" class="muted">No data yet.</td></tr>`}</tbody>
        </table>
      </div>
    </section>`;
}

function formatCell(value) {
  const text = value === null || value === undefined || value === '' ? '—' : String(value);
  if (text === 'TOTAL') return '<strong>TOTAL</strong>';
  return escapeHtml(text);
}
