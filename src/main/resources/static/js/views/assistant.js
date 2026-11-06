/**
 * Assistant — "Ask the guide".
 *
 * A chat screen wrapped in the app's own shell: same greeting block, chips,
 * cards, tokens and tab bar as the rest of the dark edition. The replies come
 * from the Spring Boot `/api/assistant` endpoints, which are a port of the
 * CBDVentures Assistant project (curated student directory + rule-based guide),
 * so no second server or API key is needed.
 */
import { api, session } from '../api.js';
import { icon, escapeHtml, toast } from '../ui.js';

const GREETING = 'Hey! 👋 I’m the CBD Ventures guide. Looking for lunch, somewhere to study, '
  + 'or directions from Richfield? Ask me anything — even what you could plan with R50.';

const CATEGORY_ICON = {
  food: 'food',
  groceries: 'bag',
  health: 'heart',
  printing: 'printer',
  study: 'study',
  banking: 'wallet',
  grooming: 'salon',
  transport: 'directions',
  fun: 'event'
};

const QUICK_REPLIES = [
  'Food near campus',
  'Somewhere to study',
  'What can I buy with R50?',
  'Directions from Richfield'
];

const PENDING_NAVIGATION_KEY = 'cbdventures.pending-location-navigation';
const LOCATION_FEEDBACK_KEY = 'cbdventures.location-feedback';

/* The transcript survives tab switches; it is cleared on a full reload. */
const transcript = [];
let activeAssistant = null;
let navigationListenersInstalled = false;
let returnCheckTimer = null;

export async function renderAssistant(container) {
  const user = session.user;

  container.innerHTML = `
    <div class="greet">
      <div>
        <p class="greet__hello">CBD Ventures AI Assistance</p>
        <h1 class="greet__title">Ask the guide</h1>
      </div>
      <span class="badge badge--green assistant__online">${icon('sparkles', 12)} Online</span>
    </div>

    <p class="field__hint" style="margin-top:10px">
      ${icon('pin', 12)} 1 km around Richfield, 291 Helen Joseph Street · answers from a curated student directory
    </p>

    <div class="chat" id="assistant-chat" role="log" aria-live="polite" aria-label="Conversation with the guide"></div>

    <div class="chips" id="assistant-quick"></div>

    <form class="composer" id="assistant-form" autocomplete="off">
      <label class="sr-only" for="assistant-input">Ask the guide a question</label>
      <input class="input" id="assistant-input" name="message" type="text" maxlength="2000"
             placeholder="Ask about food, study spots or directions…">
      <button class="composer__send" type="submit" aria-label="Send message" id="assistant-send">
        ${icon('arrowRight', 18)}
      </button>
    </form>

    <div id="assistant-cards"></div>

    <section id="assistant-directory" aria-label="Browse the student directory" style="margin-top:22px"></section>
  `;

  const chat = container.querySelector('#assistant-chat');
  const quick = container.querySelector('#assistant-quick');
  const cards = container.querySelector('#assistant-cards');
  const form = container.querySelector('#assistant-form');
  const input = container.querySelector('#assistant-input');
  const send = container.querySelector('#assistant-send');
  const directory = container.querySelector('#assistant-directory');

  let busy = false;

  if (!transcript.length) {
    transcript.push({ role: 'assistant', content: GREETING, places: [] });
  }

  activeAssistant = { container, chat, transcript, paintChat };
  installNavigationReturnListeners();
  paintChat();
  paintQuick();
  checkForNavigationReturn();

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const text = input.value.trim();
    if (!text || busy) return;
    input.value = '';
    ask(text);
  });

  // Quick-reply chips live under the conversation, so listen there (and inside
  // the transcript, where the same chips can appear).
  quick.addEventListener('click', (event) => {
    const prompt = event.target.closest('[data-prompt]');
    if (prompt) ask(prompt.dataset.prompt);
  });

  chat.addEventListener('click', (event) => {
    const prompt = event.target.closest('[data-prompt]');
    if (prompt) ask(prompt.dataset.prompt);

    const feedback = event.target.closest('[data-location-feedback]');
    if (feedback) submitLocationFeedback(feedback.dataset.locationFeedback, feedback.dataset.feedbackAnswer);
  });

  if (!container.dataset.locationFeedbackListener) {
    container.dataset.locationFeedbackListener = 'true';
    container.addEventListener('click', (event) => {
      const directionsLink = event.target.closest('[data-location-place]');
      if (!directionsLink) return;
      savePendingNavigation(directionsLink.dataset.locationPlace);
    });
  }

  cards.addEventListener('click', (event) => {
    const button = event.target.closest('[data-ask]');
    if (button) ask(`Tell me more about ${button.dataset.ask}`);
  });

  function paintChat() {
    chat.innerHTML = transcript.map(bubble).join('');
    scrollToEnd();
  }

  function paintQuick() {
    quick.innerHTML = QUICK_REPLIES
      .map((reply) => `<button class="chip" type="button" data-prompt="${escapeHtml(reply)}">${escapeHtml(reply)}</button>`)
      .join('');
  }

  function bubble(message) {
    const who = message.role === 'user' ? 'user' : 'assistant';
    const avatar = who === 'assistant' ? icon('sparkles', 15) : initials(user?.fullName);
    return `
      <div class="msg msg--${who}">
        <span class="msg__avatar" aria-hidden="true">${avatar}</span>
        <div class="msg__body">
          <div class="msg__bubble">${message.typing
            ? '<span class="msg__dots"><i></i><i></i><i></i></span>'
            : formatReply(message.content)}</div>
          ${message.locationFeedback && !message.feedbackAnswer
            ? `<div class="msg__feedback" role="group" aria-label="Location accuracy feedback">
                <button class="btn btn--sm btn--ghost" type="button" data-location-feedback="${escapeHtml(message.locationFeedback.id)}" data-feedback-answer="accurate">Yes, accurate</button>
                <button class="btn btn--sm btn--ghost" type="button" data-location-feedback="${escapeHtml(message.locationFeedback.id)}" data-feedback-answer="inaccurate">No, it was off</button>
                <button class="btn btn--sm btn--ghost" type="button" data-location-feedback="${escapeHtml(message.locationFeedback.id)}" data-feedback-answer="not-visited">I didn’t get there</button>
              </div>` : ''}
          ${who === 'assistant' && !message.typing
            ? `<span class="msg__meta">${icon('clock', 11)} ${message.at || stamp()}</span>` : ''}
        </div>
      </div>`;
  }

  function scrollToEnd() {
    // The conversation is its own scroll box, so pin that box to the newest
    // bubble (scrolling the page instead would leave the reply half hidden).
    requestAnimationFrame(() => {
      chat.scrollTop = chat.scrollHeight;
    });
  }

  async function ask(text) {
    if (busy) return;
    busy = true;
    send.disabled = true;
    cards.innerHTML = '';

    transcript.push({ role: 'user', content: text, at: stamp() });
    transcript.push({ role: 'assistant', content: '', typing: true });
    paintChat();

    try {
      const history = transcript
        .filter((message) => !message.typing)
        .map((message) => ({ role: message.role, content: message.content }));
      const { data } = await api.assistantChat(history);

      transcript.pop();
      transcript.push({
        role: 'assistant',
        content: data.reply,
        at: stamp(),
        places: data.places || []
      });
      if (!container.isConnected) return;
      paintChat();
      paintPlaces(data.places || []);
    } catch (error) {
      if (!container.isConnected) return;
      transcript.pop();
      transcript.push({
        role: 'assistant',
        content: error.status === 429
          ? 'You’re asking a lot at once — give me a few seconds and try again.'
          : `I couldn’t reach the guide just then (${error.message}). Please try again.`,
        at: stamp()
      });
      paintChat();
      toast(error.message, 'error');
    } finally {
      busy = false;
      send.disabled = false;
      // preventScroll: focusing must not yank the page away from the reply.
      if (container.isConnected) input.focus({ preventScroll: true });
    }
  }

  function paintPlaces(places) {
    if (!places.length) {
      cards.innerHTML = '';
      return;
    }
    cards.innerHTML = `
      <div class="section-head"><h2>Places mentioned</h2><span class="muted small">${places.length} match${places.length === 1 ? '' : 'es'}</span></div>
      <div class="stack">${places.map(placeCard).join('')}</div>`;
  }

  /* --------------------------------------------------------- directory */

  let allPlaces = [];
  let activeCategory = '';

  try {
    const [config, places] = await Promise.all([api.assistantConfig(), api.assistantPlaces()]);
    allPlaces = places.data || [];
    if (!container.isConnected) return;
    paintDirectory(config.data);
  } catch (error) {
    if (!container.isConnected) return;
    directory.innerHTML = `<div class="card notice notice--error">The student directory could not be loaded (${escapeHtml(error.message)}).</div>`;
    return;
  }

  function paintDirectory(config) {
    const categories = config?.categories || [];
    directory.innerHTML = `
      <div class="section-head">
        <h2>Browse the guide</h2>
        <span class="muted small">${config?.nearbyCount ?? 0} of ${config?.placeCount ?? 0} verified nearby</span>
      </div>
      <div class="chips" id="assistant-categories">
        <button class="chip is-active" type="button" data-category="">All</button>
        ${categories.map((category) => `
          <button class="chip" type="button" data-category="${escapeHtml(category.id)}">
            ${escapeHtml(category.label)}
          </button>`).join('')}
      </div>
      <div class="stack" id="assistant-place-list">${allPlaces.map(placeCard).join('')}</div>
      <p class="field__hint" style="margin-top:14px">
        Listings marked “student-submitted” have not had their exact position confirmed, so the guide quotes
        no distance for them.
      </p>`;

    directory.querySelector('#assistant-categories').addEventListener('click', (event) => {
      const chip = event.target.closest('[data-category]');
      if (!chip) return;
      activeCategory = chip.dataset.category;
      directory.querySelectorAll('.chip').forEach((node) => node.classList.toggle('is-active', node === chip));
      const filtered = activeCategory
        ? allPlaces.filter((place) => place.category === activeCategory)
        : allPlaces;
      const list = directory.querySelector('#assistant-place-list');
      list.innerHTML = filtered.length
        ? filtered.map(placeCard).join('')
        : '<div class="empty-state"><strong>Nothing here yet</strong><p class="muted small">Try another category.</p></div>';
    });

    directory.querySelector('#assistant-place-list').addEventListener('click', (event) => {
      const button = event.target.closest('[data-ask]');
      if (button) ask(`Tell me more about ${button.dataset.ask}`);
    });
  }
}

function installNavigationReturnListeners() {
  if (navigationListenersInstalled) return;
  navigationListenersInstalled = true;

  const returnedToPage = () => checkForNavigationReturn();
  window.addEventListener('blur', markNavigationAsLeft);
  window.addEventListener('focus', returnedToPage);
  window.addEventListener('pageshow', returnedToPage);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') markNavigationAsLeft();
    else returnedToPage();
  });
}

function markNavigationAsLeft() {
  const pending = readPendingNavigation();
  if (!pending) return;
  pending.leftPage = true;
  writePendingNavigation(pending);
}

function savePendingNavigation(placeName) {
  const pending = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
    placeName,
    startedAt: Date.now(),
    leftPage: false
  };
  writePendingNavigation(pending);
}

function checkForNavigationReturn() {
  const pending = readPendingNavigation();
  if (!pending?.leftPage || document.visibilityState === 'hidden') return;

  if (returnCheckTimer) window.clearTimeout(returnCheckTimer);
  const wait = Math.max(0, 1200 - (Date.now() - pending.startedAt));
  returnCheckTimer = window.setTimeout(() => {
    returnCheckTimer = null;
    const current = readPendingNavigation();
    const view = activeAssistant;
    if (!current || current.id !== pending.id || document.visibilityState === 'hidden'
      || !view?.chat.isConnected) return;

    clearPendingNavigation();
    view.transcript.push({
      role: 'assistant',
      content: `You opened directions to ${pending.placeName}. Was the location accurate when you got there?`,
      at: stamp(),
      locationFeedback: { id: pending.id, placeName: pending.placeName }
    });
    view.paintChat();
  }, wait);
}

function submitLocationFeedback(feedbackId, answer) {
  const view = activeAssistant;
  if (!view?.chat.isConnected) return;
  const question = view.transcript.find((message) => message.locationFeedback?.id === feedbackId);
  if (!question || question.feedbackAnswer) return;

  const labels = {
    accurate: 'Yes, the location was accurate.',
    inaccurate: 'No, the location was off.',
    'not-visited': 'I didn’t get there.'
  };
  const label = labels[answer];
  if (!label) return;

  question.feedbackAnswer = answer;
  const saved = storeLocationFeedback({
    placeName: question.locationFeedback.placeName,
    answer,
    submittedAt: new Date().toISOString()
  });
  view.transcript.push({ role: 'user', content: label, at: stamp() });
  view.transcript.push({
    role: 'assistant',
    content: answer === 'not-visited'
      ? 'No problem. You can share feedback after your next visit.'
      : saved
        ? 'Thanks — I’ve recorded your location feedback on this device.'
        : 'Thanks for letting me know. Your feedback is noted in this conversation.',
    at: stamp()
  });
  view.paintChat();
}

function readPendingNavigation() {
  try {
    return JSON.parse(sessionStorage.getItem(PENDING_NAVIGATION_KEY) || 'null');
  } catch (error) {
    return null;
  }
}

function writePendingNavigation(pending) {
  try {
    sessionStorage.setItem(PENDING_NAVIGATION_KEY, JSON.stringify(pending));
  } catch (error) {
    // The location question is a convenience; directions still work if storage is unavailable.
  }
}

function clearPendingNavigation() {
  try {
    sessionStorage.removeItem(PENDING_NAVIGATION_KEY);
  } catch (error) {
    // Ignore unavailable browser storage.
  }
}

function storeLocationFeedback(feedback) {
  try {
    const saved = JSON.parse(localStorage.getItem(LOCATION_FEEDBACK_KEY) || '[]');
    const entries = Array.isArray(saved) ? saved : [];
    entries.push(feedback);
    localStorage.setItem(LOCATION_FEEDBACK_KEY, JSON.stringify(entries.slice(-50)));
    return true;
  } catch (error) {
    // Keep the conversation usable if browser storage is disabled or full.
    return false;
  }
}

/* ------------------------------------------------------------- fragments */

function placeCard(place) {
  const glyph = CATEGORY_ICON[place.category] || 'pin';
  const media = place.image
    ? `<img src="${escapeHtml(place.image)}" alt="${escapeHtml(place.name)}" loading="lazy">`
    : `<span class="place-card__glyph">${icon(glyph, 20)}</span>`;

  const distance = place.withinRadius && Number.isFinite(place.distanceMeters)
    ? `<span class="badge badge--violet place-card__badge">${icon('pin', 10)} ${place.distanceMeters ? `${place.distanceMeters} m` : 'On campus'}</span>`
    : `<span class="badge badge--gold place-card__badge">Unverified</span>`;

  return `
    <article class="place-card">
      <div class="place-card__media">${media}</div>
      <div class="place-card__body">
        <div class="place-card__top">
          <strong class="place-card__title">${escapeHtml(place.name)}</strong>
          ${distance}
        </div>
        <span class="place-card__cat">${escapeHtml(place.categoryLabel)}</span>
        <span class="place-card__meta">${icon('pin', 11)} ${escapeHtml(place.address)}</span>
        <div class="place-card__actions">
          <a class="btn btn--sm btn--ghost" href="${escapeHtml(place.directionsUrl)}" data-location-place="${escapeHtml(place.name)}"
             target="_blank" rel="noopener noreferrer">${icon('directions', 14)} Directions</a>
          <button class="btn btn--sm btn--ghost" type="button" data-ask="${escapeHtml(place.name)}">
            ${icon('chat', 14)} Ask about it
          </button>
        </div>
      </div>
    </article>`;
}

/**
 * Turn a plain-text reply into markup the app's typography expects: paragraphs
 * for prose and a real list for the "• " bullet lines the guide produces.
 * Everything is escaped first, so a reply can never inject HTML.
 */
function formatReply(text) {
  const blocks = String(text || '').split(/\n{2,}/);
  return blocks.map((block) => {
    const lines = block.split('\n').filter((line) => line.trim().length);
    if (!lines.length) return '';
    if (lines.every((line) => line.trim().startsWith('•'))) {
      return `<ul class="msg__list">${lines
        .map((line) => `<li>${inline(line.replace(/^\s*•\s*/, ''))}</li>`)
        .join('')}</ul>`;
    }
    return `<p>${inline(block.replace(/\n/g, ' '))}</p>`;
  }).join('');
}

function inline(text) {
  return escapeHtml(text).replace(/(https?:\/\/[^\s<]+)/g,
    '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');
}

function stamp() {
  return new Intl.DateTimeFormat('en-ZA', { hour: '2-digit', minute: '2-digit' }).format(new Date());
}

function initials(name) {
  if (!name) return icon('user', 14);
  const parts = String(name).trim().split(/\s+/);
  return escapeHtml(((parts[0] || '')[0] || '') + ((parts[1] || '')[0] || '')).toUpperCase() || icon('user', 14);
}
