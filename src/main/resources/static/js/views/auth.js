/**
 * Authentication screens: sign in, create profile and the six-digit email
 * verification step. All three are full-screen routes (not modals) to match the
 * reference design, and all three talk to the real API — registration returns a
 * code, verification returns the JWT.
 */
import { api, session } from '../api.js';
import { icon, toast, openModal, closeModal, escapeHtml } from '../ui.js';
import { setState } from '../state.js';

const DEMO_ACCOUNTS = [
  { label: 'User', email: 'keziah@student.richfield.ac.za', password: 'Password123!' },
  { label: 'Owner', email: 'thabo@kotakingdom.co.za', password: 'Password123!' },
  { label: 'Admin', email: 'admin@cbdventures.co.za', password: 'Admin123!' }
];

/** Where to go after a successful sign-in. */
function nextTarget(params) {
  return params.next ? '#/profile' : '#/';
}

function passwordField(id, label, { hint = '', autocomplete = 'current-password' } = {}) {
  return `
    <div class="field">
      <label for="${id}">${label}</label>
      <span class="input-wrap">
        <span class="icon">${icon('lock', 18)}</span>
        <input class="input" id="${id}" name="${id}" type="password" autocomplete="${autocomplete}"
               placeholder="••••••••" required>
        <button class="input-wrap__action" type="button" data-toggle="${id}" aria-label="Show password">
          ${icon('eye', 18)}
        </button>
      </span>
      ${hint ? `<span class="field__hint">${hint}</span>` : ''}
    </div>`;
}

/** Wires the show/hide toggles inside a screen. */
function bindPasswordToggles(root) {
  root.querySelectorAll('[data-toggle]').forEach((button) => {
    button.addEventListener('click', () => {
      const input = root.querySelector(`#${button.dataset.toggle}`);
      const showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      button.innerHTML = icon(showing ? 'eye' : 'eyeOff', 18);
      button.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
    });
  });
}

function showError(node, message) {
  node.textContent = message;
  node.hidden = false;
}

/* ------------------------------------------------------------------ sign in */

export async function renderSignIn(container, params = {}) {
  let providers = { google: false, apple: false };
  try {
    const { data } = await api.oauthProviders();
    providers = { ...providers, ...data };
  } catch { /* provider buttons will explain how to configure sign-in */ }

  container.innerHTML = `
    <div class="auth">
      <button class="auth__back" type="button" data-back aria-label="Back">${icon('arrowLeft', 22)}</button>

      <div class="auth__head">
        <h1 class="auth__title">Welcome Back</h1>
        <p class="auth__sub">Sign in to continue exploring Pretoria Central</p>
      </div>

      <form class="auth__body" id="signin-form" novalidate>
        <div class="field">
          <label for="email">Email address</label>
          <span class="input-wrap">
            <span class="icon">${icon('mail', 18)}</span>
            <input class="input" id="email" name="email" type="email" inputmode="email"
                   autocomplete="email" placeholder="you@example.com" required>
          </span>
        </div>

        ${passwordField('password', 'Password')}

        <div class="auth__aside"><a href="#/signin" id="forgot">Forgot Password?</a></div>
        <p class="field-error" id="signin-error" hidden></p>

        <button class="btn btn--block" type="submit" id="signin-submit">Sign In</button>

        <div class="divider">or continue with</div>
        <div class="social-row">
          <button class="social-btn" type="button" data-social="google">${icon('google', 18)} Google</button>
          <button class="social-btn" type="button" data-social="apple">${icon('apple', 18)} Apple</button>
        </div>

        <p class="auth__foot">Don't have an account?
          <a href="#/signup">Create one</a></p>
      </form>

      <div class="card" style="display:grid;gap:10px">
        <div class="row row--between">
          <strong class="small">Demo accounts</strong>
          <span class="badge badge--gold">Marker use</span>
        </div>
        <div class="filter-options">
          ${DEMO_ACCOUNTS.map((account) => `
            <button class="chip" type="button" data-demo="${account.email}">${account.label}</button>`).join('')}
        </div>
        <p class="field__hint">Fills the form with a seeded account so the walkthrough does not type passwords live.</p>
      </div>
    </div>`;

  container.querySelector('[data-back]').addEventListener('click', () => { location.hash = '#/welcome'; });
  bindPasswordToggles(container);

  const form = container.querySelector('#signin-form');
  const error = container.querySelector('#signin-error');
  const submit = container.querySelector('#signin-submit');

  if (params.oauth_error) {
    showError(error, params.oauth_error === 'unverified'
      ? 'This provider did not confirm the email address. Try another account or sign in with email.'
      : 'Sign-in could not be completed. Please try again or use email and password.');
  }

  container.querySelectorAll('[data-demo]').forEach((button) => {
    button.addEventListener('click', () => {
      const account = DEMO_ACCOUNTS.find((item) => item.email === button.dataset.demo);
      form.email.value = account.email;
      form.password.value = account.password;
      error.hidden = true;
    });
  });

  container.querySelectorAll('[data-social]').forEach((button) => {
    button.addEventListener('click', () => {
      const provider = button.dataset.social;
      if (!providers[provider]) {
        const variables = provider === 'google'
          ? 'GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET'
          : 'APPLE_CLIENT_ID and APPLE_CLIENT_SECRET';
        const name = provider === 'google' ? 'Google' : 'Apple';
        toast(`${name} sign-in needs ${variables} in Render Environment.`, 'error');
        return;
      }
      sessionStorage.setItem('cbd.oauth.next', nextTarget(params));
      window.location.assign(`/oauth2/authorization/${provider}`);
    });
  });

  container.querySelector('#forgot').addEventListener('click', (event) => {
    event.preventDefault();
    openModal({
      title: 'Reset your password',
      body: `<div class="stack">
        <p class="small">Sign-in problems are rare, but if a password stops working the platform team
        issues a new one. In the pilot there is no mail relay, so the reset is handled directly:</p>
        <div class="notice notice--info small">Email <strong>admin@cbdventures.co.za</strong> from the address on the
        account. We confirm your identity and set a temporary password you change on first sign-in.</div>
        <p class="small muted">Self-service reset tokens are on the post-MVP backlog (see README §11).</p>
      </div>`,
      footer: '<button class="btn btn--ghost" type="button" data-modal-close>Close</button>'
    });
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.hidden = true;

    const email = form.email.value.trim();
    const password = form.password.value;
    if (!email || !password) {
      showError(error, 'Enter your email address and password.');
      return;
    }

    submit.disabled = true;
    submit.textContent = 'Signing in…';
    try {
      const { data } = await api.login({ email, password });
      const user = session.save(data);
      setState({ user });
      window.dispatchEvent(new Event('cbd:session'));
      toast(`Welcome back, ${user.fullName.split(' ')[0]}.`, 'success');
      location.hash = nextTarget(params);
    } catch (failure) {
      // 403 means "the address is not confirmed yet": send them to the code screen.
      if (failure.status === 403) {
        location.hash = `#/verify?email=${encodeURIComponent(email)}`;
        return;
      }
      showError(error, failure.message);
    } finally {
      submit.disabled = false;
      submit.textContent = 'Sign In';
    }
  });
}

/* ------------------------------------------------------------------ sign up */

export async function renderSignUp(container) {
  container.innerHTML = `
    <div class="auth">
      <button class="auth__back" type="button" data-back aria-label="Back">${icon('arrowLeft', 22)}</button>

      <div class="auth__head">
        <h1 class="auth__title">Create Profile</h1>
        <p class="auth__sub">Join our explorer community today</p>
      </div>

      <form class="auth__body" id="signup-form" novalidate>
        <div style="display:grid;justify-items:center;gap:8px">
          <label class="avatar avatar--lg avatar--upload" for="photo" title="Add a profile photo">
            <span id="photo-preview">${icon('camera', 26)}</span>
            <input id="photo" type="file" accept="image/*" hidden>
          </label>
          <span class="field__label" style="color:var(--gold)">Upload photo</span>
        </div>

        <div class="field">
          <label for="fullName">Full name</label>
          <span class="input-wrap">
            <span class="icon">${icon('user', 18)}</span>
            <input class="input" id="fullName" name="fullName" type="text" autocomplete="name"
                   placeholder="Thabo Mokoena" required maxlength="100">
          </span>
        </div>

        <div class="field">
          <label for="new-email">Email address</label>
          <span class="input-wrap">
            <span class="icon">${icon('mail', 18)}</span>
            <input class="input" id="new-email" name="email" type="email" inputmode="email"
                   autocomplete="email" placeholder="you@example.com" required>
          </span>
        </div>

        ${passwordField('new-password', 'Password', { autocomplete: 'new-password' })}
        ${passwordField('confirm-password', 'Confirm password', { autocomplete: 'new-password' })}

        <div class="field">
          <label for="role">I am joining as</label>
          <select id="role" name="role" class="input">
            <option value="VISITOR">A student, local or visitor</option>
            <option value="OWNER">A business owner (list my business)</option>
          </select>
        </div>

        <label class="row small" style="align-items:flex-start;gap:10px;color:var(--muted)">
          <input type="checkbox" id="terms" name="terms" style="width:18px;height:18px;margin-top:2px;accent-color:var(--orange)">
          <span>I agree to the <a href="#/welcome" class="auth__link">Terms of Service</a> &amp;
          <a href="#/welcome" class="auth__link">Privacy Policy</a></span>
        </label>

        <p class="field-error" id="signup-error" hidden></p>
        <button class="btn btn--block" type="submit" id="signup-submit">Create Account</button>

        <p class="auth__foot">Already have an account? <a href="#/signin">Sign in</a></p>
      </form>
    </div>`;

  container.querySelector('[data-back]').addEventListener('click', () => { location.hash = '#/welcome'; });
  bindPasswordToggles(container);

  // Local-only photo preview: the pilot stores no binaries (POPIA minimisation).
  container.querySelector('#photo').addEventListener('change', (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      container.querySelector('#photo-preview').outerHTML =
        `<img src="${reader.result}" alt="Selected profile photo" style="width:100%;height:100%;object-fit:cover">`;
      toast('Photo added. The pilot keeps images on this device only.');
    };
    reader.readAsDataURL(file);
  });

  const form = container.querySelector('#signup-form');
  const error = container.querySelector('#signup-error');
  const submit = container.querySelector('#signup-submit');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.hidden = true;

    const fullName = form.fullName.value.trim();
    const email = form.email.value.trim();
    const password = form['new-password'].value;
    const confirm = form['confirm-password'].value;

    if (fullName.length < 2) return showError(error, 'Tell us your name so owners know who is reviewing.');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return showError(error, 'That email address does not look right.');
    if (password.length < 8) return showError(error, 'Use at least 8 characters for your password.');
    if (password !== confirm) return showError(error, 'The two passwords do not match.');
    if (!form.terms.checked) return showError(error, 'Please accept the terms to create an account.');

    submit.disabled = true;
    submit.textContent = 'Creating…';
    try {
      const { data } = await api.register({
        fullName, email, password, role: form.role.value, businessName: null
      });
      // The demo transport hands the code back so the screen can show it.
      if (data.demoCode) sessionStorage.setItem('cbd.demoCode', data.demoCode);
      toast('Account created. Check your inbox for the six-digit code.', 'success');
      location.hash = `#/verify?email=${encodeURIComponent(data.email || email)}`;
    } catch (failure) {
      showError(error, failure.message);
    } finally {
      submit.disabled = false;
      submit.textContent = 'Create Account';
    }
  });
}

/* ------------------------------------------------------------------- verify */

export async function renderVerify(container, params = {}) {
  const email = params.email || '';
  const demoCode = sessionStorage.getItem('cbd.demoCode') || '';

  container.innerHTML = `
    <div class="auth">
      <button class="auth__back" type="button" data-back aria-label="Back">${icon('arrowLeft', 22)}</button>

      <div class="otp-badge">${icon('mail', 38)}</div>

      <div class="auth__head" style="text-align:center">
        <h1 class="auth__title" style="font-size:1.7rem">Verify Your Email</h1>
        <p class="auth__sub">We sent a 6-digit verification code<br>to your email address
          <strong style="color:var(--ink)">${escapeHtml(email || 'your inbox')}</strong></p>
      </div>

      ${demoCode ? `<div class="notice notice--info small">
        Demo transport: this build has no mail relay, so your code is
        <strong>${escapeHtml(demoCode)}</strong> (also printed in the server log).
        Codes expire after 10 minutes.</div>` : ''}

      <form class="auth__body" id="verify-form" novalidate>
        <div class="otp" id="otp">
          ${Array.from({ length: 6 }, (_, index) => `
            <input type="text" inputmode="numeric" autocomplete="one-time-code" maxlength="1"
                   aria-label="Digit ${index + 1}" ${index === 0 ? 'autofocus' : ''}>`).join('')}
        </div>

        <p class="field-error" id="verify-error" hidden style="text-align:center"></p>

        <button class="btn btn--block" type="submit" id="verify-submit">Verify</button>

        <div class="resend">
          <span>Didn't receive the code?</span>
          <button type="button" id="resend">Resend Code</button>
        </div>
      </form>

      <p class="secure-note">${icon('shieldCheck', 15)} Protected by one-time email verification</p>
    </div>`;

  container.querySelector('[data-back]').addEventListener('click', () => { location.hash = '#/signup'; });

  const inputs = [...container.querySelectorAll('#otp input')];
  const error = container.querySelector('#verify-error');
  const submit = container.querySelector('#verify-submit');

  const code = () => inputs.map((input) => input.value).join('');

  inputs.forEach((input, index) => {
    input.addEventListener('input', () => {
      input.value = input.value.replace(/\D/g, '').slice(0, 1);
      input.classList.toggle('is-filled', Boolean(input.value));
      if (input.value && index < inputs.length - 1) inputs[index + 1].focus();
      error.hidden = true;
    });
    input.addEventListener('keydown', (event) => {
      if (event.key === 'Backspace' && !input.value && index > 0) {
        inputs[index - 1].focus();
        inputs[index - 1].value = '';
        inputs[index - 1].classList.remove('is-filled');
      }
    });
    input.addEventListener('paste', (event) => {
      const pasted = (event.clipboardData?.getData('text') || '').replace(/\D/g, '').slice(0, 6);
      if (!pasted) return;
      event.preventDefault();
      pasted.split('').forEach((digit, offset) => {
        if (inputs[offset]) {
          inputs[offset].value = digit;
          inputs[offset].classList.add('is-filled');
        }
      });
      inputs[Math.min(pasted.length, inputs.length - 1)].focus();
    });
  });

  container.querySelector('#resend').addEventListener('click', async (event) => {
    event.preventDefault();
    if (!email) return showError(error, 'Go back and create the account again so we know the address.');
    try {
      const { data } = await api.resend({ email });
      if (data.demoCode) {
        sessionStorage.setItem('cbd.demoCode', data.demoCode);
        toast(`New code: ${data.demoCode}`, 'success');
      } else {
        toast('A new code is on its way.', 'success');
      }
    } catch (failure) {
      showError(error, failure.message);
    }
  });

  container.querySelector('#verify-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    error.hidden = true;
    const value = code();
    if (value.length !== 6) return showError(error, 'Enter all six digits.');

    submit.disabled = true;
    submit.textContent = 'Verifying…';
    try {
      const { data } = await api.verify({ email, code: value });
      const user = session.save(data);
      setState({ user });
      sessionStorage.removeItem('cbd.demoCode');
      window.dispatchEvent(new Event('cbd:session'));
      toast(`You're in, ${user.fullName.split(' ')[0]}.`, 'success');
      location.hash = '#/';
    } catch (failure) {
      showError(error, failure.message);
      inputs.forEach((input) => { input.value = ''; input.classList.remove('is-filled'); });
      inputs[0].focus();
    } finally {
      submit.disabled = false;
      submit.textContent = 'Verify';
    }
  });

  if (demoCode) inputs[0].focus();
  closeModal();
}
