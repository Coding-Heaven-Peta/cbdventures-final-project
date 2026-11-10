/**
 * Welcome screen — the first thing a new visitor sees.
 * Reference layout: full-bleed hero illustration, wordmark, one primary action
 * (Sign In) and one secondary (Create Account).
 */
import { icon } from '../ui.js';

const SEEN_KEY = 'cbd.welcomeSeen';

export async function renderWelcome(container) {
  localStorage.setItem(SEEN_KEY, '1');

  container.innerHTML = `
    <div class="welcome">
      <div class="welcome__hero">
        <img src="/images/CBD.jpg" alt="Aerial view of the Pretoria CBD" width="480" height="420">
      </div>

      <div>
        <h1 class="welcome__title">CBDVentures</h1>
        <p class="welcome__tag">Discover Pretoria Central</p>
      </div>

      <div class="welcome__actions">
        <a class="btn btn--block" href="#/signin">Sign In</a>
        <a class="btn btn--ghost btn--block" href="#/signup">Create Account</a>
        <button class="btn--link" type="button" id="browse-as-guest" style="justify-self:center;margin-top:4px">
          Browse without an account
        </button>
      </div>

      <p class="welcome__legal">
        Everything here sits inside 1 km of Richfield Graduate Institute of Technology.
        Verified listings, student prices, and ${icon('shieldCheck', 13)} one review per person.
      </p>
    </div>`;

  container.querySelector('#browse-as-guest').addEventListener('click', () => {
    location.hash = '#/';
  });
}
