/* CBDventures: front-end logic.
   Accounts are a browser-only demo; places come live from OpenStreetMap. */
(function () {
  'use strict';

  /* ---------- Settings: change file names here if you rename pages ---------- */
  var PAGES = {
    login: 'page2.html',
    signup: 'signup.html',
    verify: 'page3.html',
    profile: 'page4.html',
    home: 'page5.html'
  };
  var CODE_MINUTES = 10;     // how long a verification code stays valid
  var RESEND_SECONDS = 30;   // wait before "Resend Code" works

  /* ---------- Backup places (prices in rands), shown only if live data can't load ---------- */
  var SAMPLE_PLACES = [
    { name: 'Union Buildings',       tag: 'Attraction',  category: 'Attractions', distance: 3.5, rating: 4.8, price: 'Free',   featured: true,  image: 'images/union-buildings.jpg' },
    { name: 'Church Square',         tag: 'Attraction',  category: 'Attractions', distance: 0.4, rating: 4.6, price: 'Free',   featured: true,  image: 'images/church-square.jpg' },
    { name: 'Voortrekker Monument',  tag: 'Culture',     category: 'Culture',     distance: 6.0, rating: 4.7, price: 'R110',   featured: true,  image: 'images/voortrekker.jpg' },
    { name: 'National Zoo',          tag: 'Attraction',  category: 'Attractions', distance: 1.2, rating: 4.7, price: 'R90',    featured: false, image: 'images/national-zoo.jpg' },
    { name: 'The Blue Room Jazz',    tag: 'Restaurants', category: 'Restaurants', distance: 0.8, rating: 4.9, price: 'R270',   featured: false, image: 'images/blue-room-jazz.jpg' },
    { name: 'Kopano Street Kitchen', tag: 'Restaurants', category: 'Restaurants', distance: 1.0, rating: 4.5, price: 'R160',   featured: false, image: 'images/kopano-kitchen.jpg' },
    { name: 'Capital Suites Hotel',  tag: 'Hotels',      category: 'Hotels',      distance: 1.5, rating: 4.4, price: 'R1 100', featured: false, image: 'images/capital-suites.jpg' },
    { name: 'Rooftop Lounge 012',    tag: 'Nightlife',   category: 'Nightlife',   distance: 2.1, rating: 4.5, price: 'R180',   featured: false, image: 'images/rooftop-lounge.jpg' },
    { name: 'City Heritage Museum',  tag: 'Culture',     category: 'Culture',     distance: 1.8, rating: 4.3, price: 'R70',    featured: false, image: 'images/heritage-museum.jpg' }
  ];

  /* ---------- Storage helpers ---------- */
  var KEYS = { users: 'cbd_users', session: 'cbd_session', pending: 'cbd_pending' };

  function read(key, fallback) {
    try {
      var v = localStorage.getItem(key);
      return v ? JSON.parse(v) : fallback;
    } catch (e) { return fallback; }
  }
  function write(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); return true; }
    catch (e) { return false; }
  }
  function remove(key) { try { localStorage.removeItem(key); } catch (e) {} }

  function getUsers() { return read(KEYS.users, {}); }
  function saveUsers(users) { return write(KEYS.users, users); }
  function getSession() { return read(KEYS.session, null); }
  function currentUser() {
    var s = getSession();
    return s && !s.guest ? (getUsers()[s.email] || null) : null;
  }

  /* ---------- Small utilities ---------- */
  function go(page) { window.location.href = page; }

  function esc(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function initials(name) {
    return name.split(/\s+/).filter(Boolean).slice(0, 2)
      .map(function (w) { return w[0].toUpperCase(); }).join('') || '?';
  }

  // Passwords are never stored as plain text: only a SHA-256 hash is kept
  function hashPassword(text) {
    var data = 'cbdventures:' + text;
    if (window.crypto && crypto.subtle && window.TextEncoder) {
      return crypto.subtle.digest('SHA-256', new TextEncoder().encode(data)).then(function (buf) {
        return Array.prototype.map.call(new Uint8Array(buf), function (b) {
          return ('0' + b.toString(16)).slice(-2);
        }).join('');
      });
    }
    return Promise.resolve('plain:' + btoa(unescape(encodeURIComponent(data))));
  }

  function makeCode() {
    var n = new Uint32Array(1);
    if (window.crypto && crypto.getRandomValues) crypto.getRandomValues(n);
    else n[0] = Math.floor(Math.random() * 4294967295);
    return String(100000 + (n[0] % 900000));
  }

  function startVerification(email) {
    var pending = {
      email: email,
      code: makeCode(),
      sentAt: Date.now(),
      expires: Date.now() + CODE_MINUTES * 60000
    };
    write(KEYS.pending, pending);
    return pending;
  }

  function showError(form, msg) {
    var el = form.querySelector('.form-error');
    if (!msg) { if (el) el.remove(); return; }
    if (!el) {
      el = document.createElement('p');
      el.className = 'form-error';
      el.setAttribute('role', 'alert');
      var btn = form.querySelector('[type="submit"]');
      btn.parentNode.insertBefore(el, btn);
    }
    el.textContent = msg;
  }

  function busy(form, on) {
    var b = form.querySelector('[type="submit"]');
    if (b) b.disabled = on;
  }

  function wireGuestLinks() {
    document.querySelectorAll('.guest').forEach(function (a) {
      a.addEventListener('click', function (e) {
        e.preventDefault();
        write(KEYS.session, { guest: true });
        go(PAGES.home);
      });
    });
  }

  function resizeImage(file, maxSize) {
    return new Promise(function (resolve, reject) {
      var reader = new FileReader();
      reader.onerror = reject;
      reader.onload = function () {
        var img = new Image();
        img.onerror = reject;
        img.onload = function () {
          var scale = Math.min(1, maxSize / Math.max(img.width, img.height));
          var c = document.createElement('canvas');
          c.width = Math.round(img.width * scale);
          c.height = Math.round(img.height * scale);
          c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
          resolve(c.toDataURL('image/jpeg', 0.85));
        };
        img.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }

  /* ================= Splash ================= */
  function initSplash() {
    var s = getSession();
    var btn = document.querySelector('.splash-screen .btn');
    // Signed-in users skip the sign-in page
    if (btn && s && !s.guest && getUsers()[s.email]) btn.setAttribute('href', PAGES.home);
  }

  /* ================= Sign in ================= */
  function initLogin(form) {
    wireGuestLinks();

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      showError(form, '');

      var email = form.elements.email.value.trim().toLowerCase();
      var password = form.elements.password.value;
      var user = getUsers()[email];

      busy(form, true);
      hashPassword(password).then(function (hash) {
        busy(form, false);

        // Same message for "no account" and "wrong password"
        if (!user || user.hash !== hash) {
          showError(form, 'Incorrect email or password.');
          return;
        }
        if (!user.verified) {
          startVerification(email);
          go(PAGES.verify);
          return;
        }
        write(KEYS.session, { email: email, guest: false });
        go(user.profile ? PAGES.home : PAGES.profile);
      });
    });
  }

  /* ================= Sign up ================= */
  function initSignup(form) {
    wireGuestLinks();

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      showError(form, '');

      var name = form.elements.name.value.trim();
      var email = form.elements.email.value.trim().toLowerCase();
      var password = form.elements.password.value;
      var users = getUsers();

      if (users[email] && users[email].verified) {
        showError(form, 'An account with this email already exists. Try signing in.');
        return;
      }

      busy(form, true);
      hashPassword(password).then(function (hash) {
        busy(form, false);
        users[email] = { name: name, email: email, hash: hash, verified: false, profile: null };

        if (!saveUsers(users)) {
          showError(form, "We couldn't save your account. Check that browser storage is allowed.");
          return;
        }
        startVerification(email);
        go(PAGES.verify);
      });
    });
  }

  /* ================= Email verification ================= */
  function initVerify(form) {
    var pending = read(KEYS.pending, null);
    if (!pending) { go(PAGES.login); return; }

    var emailEl = document.querySelector('.verify-header strong');
    if (emailEl) emailEl.textContent = pending.email;

    var boxes = Array.prototype.slice.call(form.querySelectorAll('.code-row input'));
    boxes.forEach(function (b) { b.value = ''; b.classList.remove('active'); });

    // No email is sent in this demo, so the code is shown on screen
    var note = document.createElement('p');
    note.className = 'demo-note';
    var header = document.querySelector('.verify-header');
    header.parentNode.insertBefore(note, header.nextSibling);

    function showCode() {
      var p = read(KEYS.pending, pending);
      note.innerHTML = 'Demo mode: no email is sent. Your code is <strong>' + esc(p.code) + '</strong>';
    }
    showCode();

    // Code boxes: type, backspace, arrows and paste
    boxes.forEach(function (box, i) {
      box.addEventListener('input', function () {
        box.value = box.value.replace(/\D/g, '').slice(-1);
        if (box.value && boxes[i + 1]) boxes[i + 1].focus();
      });
      box.addEventListener('keydown', function (e) {
        if (e.key === 'Backspace' && !box.value && boxes[i - 1]) {
          e.preventDefault();
          boxes[i - 1].value = '';
          boxes[i - 1].focus();
        } else if (e.key === 'ArrowLeft' && boxes[i - 1]) {
          boxes[i - 1].focus();
        } else if (e.key === 'ArrowRight' && boxes[i + 1]) {
          boxes[i + 1].focus();
        }
      });
      box.addEventListener('focus', function () { box.select(); });
      box.addEventListener('paste', function (e) {
        var text = (e.clipboardData || window.clipboardData).getData('text')
          .replace(/\D/g, '').slice(0, boxes.length);
        if (!text) return;
        e.preventDefault();
        var start = text.length === boxes.length ? 0 : i;
        text.split('').forEach(function (d, k) { if (boxes[start + k]) boxes[start + k].value = d; });
        boxes[Math.min(start + text.length, boxes.length - 1)].focus();
      });
    });
    boxes[0].focus();

    // Verify
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      showError(form, '');

      var entered = boxes.map(function (b) { return b.value; }).join('');
      var p = read(KEYS.pending, null);
      if (!p) { go(PAGES.login); return; }

      if (entered.length < boxes.length) { showError(form, 'Please enter all 6 digits.'); return; }
      if (Date.now() > p.expires) { showError(form, 'This code has expired. Tap "Resend Code" for a new one.'); return; }
      if (entered !== p.code) { showError(form, 'That code is not correct. Please try again.'); return; }

      var users = getUsers();
      var user = users[p.email];
      if (!user) { go(PAGES.signup); return; }

      user.verified = true;
      saveUsers(users);
      remove(KEYS.pending);
      write(KEYS.session, { email: p.email, guest: false });
      go(user.profile ? PAGES.home : PAGES.profile);
    });

    // Resend with countdown
    var resend = document.getElementById('resend');
    if (resend) {
      var canResend = false;
      var timer = null;

      var startCountdown = function () {
        var left = RESEND_SECONDS;
        canResend = false;
        resend.textContent = 'Resend in ' + left + 's';
        resend.style.opacity = '.6';
        clearInterval(timer);
        timer = setInterval(function () {
          left--;
          if (left <= 0) {
            clearInterval(timer);
            canResend = true;
            resend.textContent = 'Resend Code';
            resend.style.opacity = '';
          } else {
            resend.textContent = 'Resend in ' + left + 's';
          }
        }, 1000);
      };

      resend.addEventListener('click', function (e) {
        e.preventDefault();
        if (!canResend) return;
        startVerification(pending.email);
        showCode();
        showError(form, '');
        boxes.forEach(function (b) { b.value = ''; });
        boxes[0].focus();
        startCountdown();
      });

      startCountdown();
    }
  }

  /* ================= Profile ================= */
  function initProfile(form) {
    var session = getSession();
    if (!session || session.guest) { go(PAGES.login); return; }
    var users = getUsers();
    var user = users[session.email];
    if (!user) { go(PAGES.login); return; }

    var prof = user.profile || {};
    var el = form.elements;

    // Fill the form with anything already saved
    el.fullname.value = prof.fullname || user.name || '';
    if (el.preferredname) el.preferredname.value = prof.preferredname || '';
    el.phone.value = prof.phone || '';
    if (prof.nationality) el.nationality.value = prof.nationality;

    var chips = form.querySelectorAll('.chips input[type="checkbox"]');
    chips.forEach(function (c) {
      c.checked = (prof.prefs || []).indexOf(c.value) !== -1;
      c.dispatchEvent(new Event('change'));   // refreshes the "pick at least one" check
    });

    // Photo upload with preview
    var avatar = form.querySelector('.avatar');
    var photoInput = avatar.querySelector('input[type="file"]');
    var camera = avatar.querySelector('svg');
    var photoData = prof.photo || '';

    function showPhoto(src) {
      avatar.style.backgroundImage = 'url("' + src + '")';
      avatar.style.backgroundSize = 'cover';
      avatar.style.backgroundPosition = 'center';
      if (camera) camera.style.display = 'none';
    }
    if (photoData) showPhoto(photoData);

    photoInput.addEventListener('change', function () {
      var file = photoInput.files && photoInput.files[0];
      if (!file) return;
      showError(form, '');
      if (file.type.indexOf('image/') !== 0) { showError(form, 'Please choose an image file.'); return; }

      resizeImage(file, 256).then(function (dataUrl) {
        photoData = dataUrl;
        showPhoto(dataUrl);
      }).catch(function () {
        showError(form, "We couldn't read that image. Try a different one.");
      });
    });

    // Save
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      showError(form, '');

      var prefs = [];
      chips.forEach(function (c) { if (c.checked) prefs.push(c.value); });

      user.name = el.fullname.value.trim();
      user.profile = {
        fullname: el.fullname.value.trim(),
        preferredname: el.preferredname ? el.preferredname.value.trim() : '',
        phone: el.phone.value.trim(),
        nationality: el.nationality.value,
        prefs: prefs,
        photo: photoData
      };
      users[session.email] = user;

      if (!saveUsers(users)) {
        showError(form, "We couldn't save your profile. Try a smaller photo.");
        return;
      }
      go(PAGES.home);
    });
  }

  /* ================= Home (live places from OpenStreetMap) ================= */
  var CENTER = { lat: -25.7463, lon: 28.1876 };   // Church Square, Pretoria CBD
  var RADIUS_M = 2500;                            // search radius in metres
  var CACHE_HOURS = 6;                            // reuse results to be kind to the free server
  var CACHE_KEY = 'cbd_places';
  var OVERPASS = [
    'https://overpass-api.de/api/interpreter',
    'https://overpass.kumi.systems/api/interpreter'
  ];

  function buildQuery() {
    var a = '(around:' + RADIUS_M + ',' + CENTER.lat + ',' + CENTER.lon + ')';
    return '[out:json][timeout:25];(' +
      'nwr["name"]["amenity"~"^(restaurant|fast_food|cafe|bar|pub|nightclub|theatre|arts_centre)$"]' + a + ';' +
      'nwr["name"]["tourism"~"^(hotel|guest_house|hostel|motel|attraction|museum|gallery|zoo|viewpoint)$"]' + a + ';' +
      'nwr["name"]["historic"~"^(monument|memorial|castle|fort)$"]' + a + ';' +
      ');out center tags 250;';
  }

  function overpassFetch(query) {
    var i = 0;
    function attempt() {
      if (i >= OVERPASS.length) return Promise.reject(new Error('All servers failed'));
      var url = OVERPASS[i++];
      var ctrl = window.AbortController ? new AbortController() : null;
      var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, 25000) : null;
      return fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'data=' + encodeURIComponent(query),
        signal: ctrl ? ctrl.signal : undefined
      }).then(function (r) {
        if (timer) clearTimeout(timer);
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      }).catch(function () {
        if (timer) clearTimeout(timer);
        return attempt();   // try the next server
      });
    }
    return attempt();
  }

  function classify(t) {
    var am = t.amenity, to = t.tourism, hi = t.historic;
    if (am === 'nightclub') return { category: 'Nightlife', tag: 'Nightclub' };
    if (am === 'bar' || am === 'pub') return { category: 'Nightlife', tag: am === 'pub' ? 'Pub' : 'Bar' };
    if (am === 'restaurant' || am === 'fast_food') return { category: 'Restaurants', tag: 'Restaurant' };
    if (am === 'cafe') return { category: 'Restaurants', tag: 'Cafe' };
    if (to === 'hotel' || to === 'guest_house' || to === 'hostel' || to === 'motel') return { category: 'Hotels', tag: 'Hotel' };
    if (to === 'museum') return { category: 'Culture', tag: 'Museum' };
    if (to === 'gallery' || am === 'arts_centre') return { category: 'Culture', tag: 'Gallery' };
    if (am === 'theatre') return { category: 'Culture', tag: 'Theatre' };
    if (to === 'attraction' || to === 'zoo' || to === 'viewpoint' || hi) return { category: 'Attractions', tag: 'Attraction' };
    return null;
  }

  function photoFor(t) {
    var c = t.wikimedia_commons || '';
    if (c.indexOf('File:') === 0) {
      return 'https://commons.wikimedia.org/wiki/Special:FilePath/' + encodeURIComponent(c.slice(5)) + '?width=600';
    }
    return '';
  }

  function detailFor(t) {
    if (t.cuisine) {
      var c = t.cuisine.split(';')[0].replace(/_/g, ' ');
      return c.charAt(0).toUpperCase() + c.slice(1);
    }
    return t['addr:street'] || '';
  }

  function parseElements(els) {
    var seen = {}, out = [];
    els.forEach(function (el) {
      var t = el.tags || {};
      var lat = el.lat != null ? el.lat : (el.center && el.center.lat);
      var lon = el.lon != null ? el.lon : (el.center && el.center.lon);
      if (!t.name || lat == null || lon == null) return;
      var c = classify(t);
      if (!c) return;
      var key = (t.name + '|' + c.category).toLowerCase();
      if (seen[key]) return;
      seen[key] = true;
      out.push({
        name: t.name, category: c.category, tag: c.tag,
        lat: lat, lon: lon, detail: detailFor(t), image: photoFor(t),
        score: (t.wikimedia_commons ? 3 : 0) + (t.wikipedia ? 2 : 0) + (t.wikidata ? 1 : 0) +
               (t.tourism === 'attraction' ? 1 : 0) + (t.historic ? 1 : 0),
        featured: false, distance: null
      });
    });
    return out;
  }

  function haversine(aLat, aLon, bLat, bLon) {
    var R = 6371, rad = Math.PI / 180;
    var dLat = (bLat - aLat) * rad, dLon = (bLon - aLon) * rad;
    var x = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(aLat * rad) * Math.cos(bLat * rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return 2 * R * Math.asin(Math.sqrt(x));
  }

  function pickFeatured(items) {
    items.forEach(function (p) { p.featured = false; });
    items.filter(function (p) { return (p.category === 'Attractions' || p.category === 'Culture') && p.score > 0; })
      .sort(function (a, b) { return (b.score - a.score) || ((a.distance || 0) - (b.distance || 0)); })
      .slice(0, 5)
      .forEach(function (p) { p.featured = true; });
  }

  function initHome(main) {
    var session = getSession();
    if (!session) { go(PAGES.login); return; }

    var user = currentUser();
    if (!session.guest) {
      if (!user) { go(PAGES.login); return; }
      if (!user.profile) { go(PAGES.profile); return; }
    }

    // Greeting and avatar
    var displayName = session.guest
      ? 'Guest Explorer'
      : (user.profile.preferredname || user.profile.fullname || user.name);
    var nameEl = main.querySelector('.username');
    if (nameEl) nameEl.textContent = displayName;

    var av = main.querySelector('.avatar-sm');
    if (av) {
      var photo = !session.guest && user.profile && user.profile.photo;
      var img = av.querySelector('img');
      if (photo) {
        if (!img) { img = document.createElement('img'); av.appendChild(img); }
        img.src = photo;
        img.style.display = 'block';
      } else {
        if (img) img.remove();
        var span = document.createElement('span');
        span.className = 'initials';
        span.textContent = initials(displayName);
        av.appendChild(span);
      }
      if (session.guest) av.setAttribute('href', PAGES.signup);
    }

    // Log out
    var out = document.getElementById('logout');
    if (out) {
      out.addEventListener('click', function (e) {
        e.preventDefault();
        remove(KEYS.session);
        go(PAGES.login);
      });
    }

    // Places
    var featuredEl = main.querySelector('.featured');
    var featuredHead = featuredEl.previousElementSibling;
    var nearEl = main.querySelector('.nearby');
    var searchEl = main.querySelector('.search input');
    var pills = main.querySelectorAll('.cat');
    var state = { cat: 'All', q: '', all: [], userPos: null };

    // Start clean: no leftover search text, and "All" highlighted to match the filter
    if (searchEl) searchEl.value = '';
    pills.forEach(function (b) {
      b.classList.toggle('active', b.textContent.trim() === 'All');
    });

    // Status line (loading / count / errors) under the category pills
    var status = document.createElement('p');
    status.className = 'live-note';
    main.querySelector('.cats').insertAdjacentElement('afterend', status);

    // Required attribution for OpenStreetMap data
    var credit = document.createElement('p');
    credit.className = 'attribution';
    credit.innerHTML = 'Place data &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors';
    main.appendChild(credit);

    function setStatus(html) {
      status.innerHTML = html;
      status.style.display = html ? 'block' : 'none';
    }

    var star = '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" aria-hidden="true"><polygon points="12 2 15 9 22 9.5 17 14.5 18.5 22 12 18 5.5 22 7 14.5 2 9.5 9 9"/></svg>';

    function distText(p) {
      if (p.distance == null) return '';
      var d = p.distance < 1 ? Math.round(p.distance * 1000) + ' m' : p.distance.toFixed(1) + ' km';
      if (p.lat == null) return d + ' away';               // sample data
      return d + (state.userPos ? ' away' : ' from centre');
    }

    function featCard(p) {
      return '<a class="feat-card" href="#">' +
        (p.image ? '<img src="' + esc(p.image) + '" alt="" loading="lazy" onerror="this.style.display=\'none\'">' : '') +
        '<span class="badge">' + esc(p.tag.toUpperCase()) + '</span>' +
        '<span class="feat-title">' + esc(p.name) + '</span></a>';
    }

    function placeCard(p) {
      var thumb = p.image
        ? '<img src="' + esc(p.image) + '" alt="" loading="lazy" onerror="this.style.display=\'none\'">'
        : '';
      var info = p.rating
        ? '<p class="rating">' + star + ' ' + p.rating.toFixed(1) + '</p>'
        : (p.detail ? '<p class="detail">' + esc(p.detail) + '</p>' : '');
      return '<a class="place" href="#">' +
        '<div class="thumb"><span class="thumb-ph">' + esc(p.name.charAt(0).toUpperCase()) + '</span>' + thumb + '</div>' +
        '<div class="place-info">' +
          '<p class="meta"><span class="tag">' + esc(p.tag) + '</span>' + (p.distance != null ? ' &nbsp;•&nbsp; ' + distText(p) : '') + '</p>' +
          '<h3 class="place-name">' + esc(p.name) + '</h3>' + info +
        '</div>' +
        (p.price ? '<span class="price">' + esc(p.price) + '</span>' : '') + '</a>';
    }

    function matches(p) {
      var okCat = state.cat === 'All' || p.category === state.cat;
      var hay = (p.name + ' ' + p.category + ' ' + p.tag + ' ' + (p.detail || '')).toLowerCase();
      return okCat && (!state.q || hay.indexOf(state.q) !== -1);
    }

    function render() {
      var feat = state.all.filter(function (p) { return p.featured && matches(p); });

      // The nearby list shows EVERY matching place, so it is never empty while places exist
      var near = state.all.filter(matches)
        .sort(function (a, b) { return (a.distance || 0) - (b.distance || 0); })
        .slice(0, 40);

      featuredEl.innerHTML = feat.map(featCard).join('');
      var show = feat.length ? '' : 'none';
      featuredEl.style.display = show;
      featuredHead.style.display = show;

      nearEl.innerHTML = near.length
        ? near.map(placeCard).join('')
        : '<p class="empty-state">' +
            (state.all.length ? 'No places match your search.' : 'No places to show yet.') +
          '</p>';
    }

    function updateDistances() {
      var ref = state.userPos || CENTER;
      state.all.forEach(function (p) {
        if (p.lat != null) p.distance = haversine(ref.lat, ref.lon, p.lat, p.lon);
      });
    }

    function applyItems(items) {
      state.all = items;
      updateDistances();
      pickFeatured(state.all);
      setStatus(items.length + ' places found around Pretoria CBD');
      render();
      locate();
    }

    // Use the visitor's location for distances (only if they are near Pretoria)
    function locate() {
      if (!navigator.geolocation) return;
      navigator.geolocation.getCurrentPosition(function (pos) {
        var me = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        if (haversine(me.lat, me.lon, CENTER.lat, CENTER.lon) > 30) return;
        state.userPos = me;
        updateDistances();
        render();
      }, function () {}, { timeout: 8000, maximumAge: 600000 });
    }

    function load(force) {
      setStatus('Finding places around Pretoria CBD…');

      var cached = force ? null : read(CACHE_KEY, null);
      if (cached && cached.items && cached.items.length && Date.now() - cached.time < CACHE_HOURS * 3600000) {
        applyItems(cached.items);
        return;
      }

      overpassFetch(buildQuery()).then(function (data) {
        var items = parseElements(data.elements || []);
        if (!items.length) throw new Error('No places returned');
        write(CACHE_KEY, { time: Date.now(), items: items });
        applyItems(items);
      }).catch(function () {
        state.all = SAMPLE_PLACES.slice();
        render();
        setStatus("Couldn't load live places, so sample places are showing. <a href=\"#\" id=\"retry\">Try again</a>");
        var retry = document.getElementById('retry');
        if (retry) retry.addEventListener('click', function (e) { e.preventDefault(); load(true); });
      });
    }

    pills.forEach(function (btn) {
      btn.addEventListener('click', function () {
        pills.forEach(function (b) { b.classList.remove('active'); });
        btn.classList.add('active');
        state.cat = btn.textContent.trim();
        render();
      });
    });

    if (searchEl) {
      searchEl.addEventListener('input', function () {
        state.q = searchEl.value.trim().toLowerCase();
        render();
      });
    }

    // Placeholder cards don't open a details page yet
    [featuredEl, nearEl].forEach(function (box) {
      box.addEventListener('click', function (e) {
        if (e.target.closest('a')) e.preventDefault();
      });
    });

    load(false);
  }

  /* ---------- Start the right logic for the current page ---------- */
  var f;
  if (document.querySelector('.splash-screen')) initSplash();
  if ((f = document.getElementById('loginForm'))) initLogin(f);
  if ((f = document.getElementById('signupForm'))) initSignup(f);
  if ((f = document.getElementById('verifyForm'))) initVerify(f);
  if ((f = document.getElementById('profileForm'))) initProfile(f);
  if ((f = document.querySelector('main.home'))) initHome(f);
})();
