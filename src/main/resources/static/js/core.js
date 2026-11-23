/* KasiMarket front end: core tools.
   Everything the screens share lives here: small DOM helpers, icons, dialogs, the API caller,
   the login session, GPS tools and the worker's "I am online" heartbeat.

   Safety rule used all over the app: text that came from the server or from a user is ALWAYS
   added with textContent (through the h() helper), never with innerHTML. That stops script
   injection (XSS). The only innerHTML in the app is for the built-in icons, which are fixed text
   written in this file. */

(function () {
  'use strict';

  var KM = window.KM = window.KM || {};
  KM.views = KM.views || {};
  var API = '/api/v1';

  // ------------------------------------------------------------------ constants
  KM.DEMO_WORKER_POS = { lat: -26.0936, lng: 28.0064 };   // fixed positions in Randburg for demos
  KM.DEMO_CLIENT_POS = { lat: -26.1100, lng: 28.0200 };

  KM.STATUS_LABEL = {
    REQUESTED: 'Finding a worker', ACCEPTED: 'Accepted', EN_ROUTE: 'On the way',
    ARRIVED: 'Arrived', IN_PROGRESS: 'Work in progress', COMPLETED: 'Finished',
    PAID: 'Paid', CANCELLED: 'Cancelled', EXPIRED: 'Expired',
    SUBMITTED: 'Waiting for approval', APPROVED: 'Approved', REJECTED: 'Rejected', SUSPENDED: 'Suspended',
    PENDING: 'Waiting for review', OPEN: 'Open', INVESTIGATING: 'Being investigated', RESOLVED: 'Resolved'
  };
  KM.STEPS = ['REQUESTED', 'ACCEPTED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'PAID'];
  KM.STEP_LABEL = ['Booked', 'Accepted', 'On the way', 'Arrived', 'Working', 'Finished', 'Paid'];
  KM.TERMINAL = ['PAID', 'CANCELLED', 'EXPIRED'];
  KM.ACTIVE = ['REQUESTED', 'ACCEPTED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED'];
  KM.TIER_LABEL = { 1: 'Starter', 2: 'Trusted', 3: 'Verified Care' };
  KM.CATEGORY_ICON = { PET_CARE: 'paw', LAUNDRY: 'shirt', CLEANING: 'sparkle', ERRANDS: 'bag', LABOUR: 'box', CHILD_CARE: 'baby', HOME_CARE: 'key' };

  // ------------------------------------------------------------------ DOM helpers
  var $ = KM.$ = function (selector) { return document.querySelector(selector); };

  KM.clear = function (el) {
    while (el.firstChild) { el.removeChild(el.firstChild); }
    return el;
  };

  // Adds a child (a text, an element, or a list of them) to an element.
  var append = KM.append = function (el, child) {
    if (child === null || child === undefined || child === false) { return; }
    if (Array.isArray(child)) {
      child.forEach(function (c) { append(el, c); });
    } else if (child instanceof Node) {
      el.appendChild(child);
    } else {
      el.appendChild(document.createTextNode(String(child)));
    }
  };

  // h('div', {class: 'card'}, 'Hello') builds <div class="card">Hello</div>.
  // Text is added as plain text, so it can never run as code.
  var h = KM.h = function (tag, attrs) {
    var el = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (key) {
        var value = attrs[key];
        if (value === null || value === undefined || value === false) { return; }
        if (key === 'class') {
          el.className = value;
        } else if (key.indexOf('on') === 0 && typeof value === 'function') {
          el.addEventListener(key.slice(2), value);
        } else if (value === true) {
          el.setAttribute(key, '');
        } else {
          el.setAttribute(key, value);
        }
      });
    }
    for (var i = 2; i < arguments.length; i++) { append(el, arguments[i]); }
    return el;
  };

  // ------------------------------------------------------------------ icons
  var ICONS = {
    home: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    list: '<path d="M8 6h13M8 12h13M8 18h13"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/>',
    shield: '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/>',
    users: '<circle cx="9" cy="8" r="3.5"/><path d="M2 20c0-3.5 3-5.5 7-5.5s7 2 7 5.5"/><circle cx="17" cy="9" r="2.5"/><path d="M17 14c3 0 5 1.5 5 4"/>',
    file: '<path d="M6 3h8l4 4v14H6z"/><path d="M14 3v4h4"/>',
    alert: '<path d="M12 3l10 18H2z"/><path d="M12 10v5M12 18h.01"/>',
    check: '<path d="M5 12l5 5 9-10"/>',
    x: '<path d="M6 6l12 12M18 6L6 18"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    pin: '<path d="M12 21s7-6 7-12a7 7 0 10-14 0c0 6 7 12 7 12z"/><circle cx="12" cy="9" r="2.5"/>',
    star: '<path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z"/>',
    paw: '<circle cx="7" cy="10" r="2"/><circle cx="12" cy="7" r="2"/><circle cx="17" cy="10" r="2"/><path d="M12 12c-3 0-5 3-5 5 0 2 2 3 5 2 3 1 5 0 5-2 0-2-2-5-5-5z"/>',
    shirt: '<path d="M8 3L3 7l3 4 2-1v11h8V10l2 1 3-4-5-4c-1 2-5 2-6 0z"/>',
    sparkle: '<path d="M12 3l2 6 6 2-6 2-2 6-2-6-6-2 6-2z"/>',
    bag: '<path d="M5 8h14l-1 13H6z"/><path d="M9 8a3 3 0 016 0"/>',
    box: '<path d="M3 8l9-5 9 5v8l-9 5-9-5z"/><path d="M3 8l9 5 9-5M12 13v8"/>',
    baby: '<circle cx="12" cy="12" r="9"/><path d="M8 14c1 2 7 2 8 0"/><circle cx="9" cy="10" r="1"/><circle cx="15" cy="10" r="1"/>',
    key: '<circle cx="8" cy="15" r="4"/><path d="M11 12l9-9M16 7l3 3"/>',
    power: '<path d="M12 3v9"/><path d="M6.5 6.5a8 8 0 1011 0"/>',
    coin: '<circle cx="12" cy="12" r="9"/><path d="M14.5 9.5c-.5-1-1.5-1.5-2.5-1.5-1.5 0-2.5.8-2.5 2s1 1.7 2.5 2 2.5.8 2.5 2-1 2-2.5 2c-1 0-2-.5-2.5-1.5M12 6v2M12 16v2"/>',
    chart: '<path d="M4 20V4M4 20h16M8 16v-5M12 16V8M16 16v-3"/>',
    lock: '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 018 0v3"/>',
    logout: '<path d="M10 4H5v16h5M15 8l4 4-4 4M19 12H9"/>',
    arrowRight: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    arrowLeft: '<path d="M19 12H5M11 6l-6 6 6 6"/>',
    zap: '<path d="M13 2L4 14h7l-1 8 9-12h-7z"/>',
    flag: '<path d="M5 21V4M5 4h11l-2 4 2 4H5"/>',
    sliders: '<path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2"/><circle cx="8" cy="17" r="2"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="M16 16l5 5"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>',
    camera: '<path d="M3 8h4l2-3h6l2 3h4v12H3z"/><circle cx="12" cy="13" r="4"/>',
    phone: '<path d="M6 3h4l2 5-2.5 1.5a11 11 0 005 5L16 12l5 2v4a2 2 0 01-2 2A16 16 0 014 5a2 2 0 012-2z"/>',
    eye: '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    download: '<path d="M12 4v11M7 11l5 5 5-5M5 20h14"/>',
    heart: '<path d="M12 20s-8-5-8-11a4.5 4.5 0 018-2.5A4.5 4.5 0 0120 9c0 6-8 11-8 11z"/>'
  };

  KM.icon = function (name, size) {
    var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('width', String(size || 20));
    svg.setAttribute('height', String(size || 20));
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '2');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('class', 'icon');
    svg.innerHTML = ICONS[name] || '';   // fixed text from this file, never user data
    return svg;
  };

  // ------------------------------------------------------------------ small formatters
  KM.money = function (amount) { return 'R' + amount; };

  KM.fmtDate = function (iso) {
    if (!iso) { return ''; }
    var d = new Date(iso);
    return isNaN(d.getTime()) ? String(iso) : d.toLocaleString();
  };

  // "5 min ago", "2 h ago", or the date for older things.
  KM.timeAgo = function (iso) {
    if (!iso) { return ''; }
    var d = new Date(iso);
    if (isNaN(d.getTime())) { return ''; }
    var min = Math.round((Date.now() - d.getTime()) / 60000);
    if (min < 1) { return 'just now'; }
    if (min < 60) { return min + ' min ago'; }
    if (min < 1440) { return Math.round(min / 60) + ' h ago'; }
    return d.toLocaleDateString();
  };

  KM.initials = function (name) {
    var parts = String(name || '?').trim().split(/\s+/);
    var first = parts[0] ? parts[0].charAt(0) : '?';
    var last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return (first + last).toUpperCase();
  };

  KM.firstName = function (nameOrEmail) {
    var s = String(nameOrEmail || '').split('@')[0].split(/[\s._-]+/)[0];
    return s ? s.charAt(0).toUpperCase() + s.slice(1) : 'there';
  };

  // ------------------------------------------------------------------ building blocks
  KM.avatar = function (name, size) {
    var hue = 0;
    String(name || '').split('').forEach(function (c) { hue = (hue * 31 + c.charCodeAt(0)) % 360; });
    var el = h('span', { class: 'avatar' + (size === 'lg' ? ' lg' : ''), 'aria-hidden': 'true' }, KM.initials(name));
    el.style.background = 'hsl(' + hue + ', 45%, 38%)';
    return el;
  };

  KM.stars = function (rating) {
    var wrap = h('span', { class: 'stars', 'aria-label': rating ? 'Rated ' + rating + ' out of 5' : 'No rating yet' });
    var full = Math.round(Number(rating) || 0);
    for (var i = 1; i <= 5; i++) {
      var s = KM.icon('star', 15);
      if (i <= full) { s.setAttribute('class', 'icon filled'); }
      wrap.appendChild(s);
    }
    return wrap;
  };

  KM.badge = function (text, kind) {
    return h('span', { class: 'badge' + (kind ? ' ' + kind : '') }, text);
  };

  KM.statusBadge = function (status) {
    var kind = 'info';
    if (['PAID', 'ACCEPTED', 'APPROVED', 'RESOLVED'].indexOf(status) !== -1) { kind = 'good'; }
    else if (['CANCELLED', 'EXPIRED', 'REJECTED', 'SUSPENDED'].indexOf(status) !== -1) { kind = 'bad'; }
    else if (['REQUESTED', 'PENDING', 'SUBMITTED', 'OPEN', 'COMPLETED'].indexOf(status) !== -1) { kind = 'warn'; }
    return KM.badge(KM.STATUS_LABEL[status] || status, kind);
  };

  KM.severityBadge = function (severity) {
    var kind = severity === 'CRITICAL' ? 'bad' : (severity === 'HIGH' ? 'warn' : 'info');
    return KM.badge(severity, kind);
  };

  KM.tierBadge = function (tier) {
    return KM.badge('Tier ' + tier + ' - ' + (KM.TIER_LABEL[tier] || ''), 'tier' + tier);
  };

  KM.progress = function (value, max, label) {
    var pct = max > 0 ? Math.max(0, Math.min(100, Math.round(value / max * 100))) : 0;
    var bar = h('div', { class: 'progress', role: 'progressbar', 'aria-valuemin': '0', 'aria-valuemax': '100', 'aria-valuenow': String(pct), 'aria-label': label || 'Progress' },
      h('span', { style: 'width:' + pct + '%' }));
    return bar;
  };

  KM.statCard = function (label, value, opts) {
    opts = opts || {};
    var card = h(opts.href ? 'a' : 'div', { class: 'stat' + (opts.kind ? ' ' + opts.kind : ''), href: opts.href || null },
      h('div', { class: 'stat-icon' }, KM.icon(opts.icon || 'chart', 20)),
      h('div', { class: 'stat-value' }, String(value)),
      h('div', { class: 'stat-label' }, label),
      opts.hint ? h('div', { class: 'stat-hint' }, opts.hint) : null);
    return card;
  };

  KM.sectionTitle = function (text, action) {
    return h('div', { class: 'section-title' }, h('h2', null, text), action || null);
  };

  KM.emptyState = function (opts) {
    return h('div', { class: 'empty' },
      h('div', { class: 'empty-icon' }, KM.icon(opts.icon || 'info', 28)),
      h('strong', null, opts.title),
      opts.text ? h('p', { class: 'muted' }, opts.text) : null,
      opts.action || null);
  };

  KM.skeleton = function (rows) {
    var box = h('div', { class: 'skeleton-wrap', 'aria-hidden': 'true' });
    for (var i = 0; i < (rows || 3); i++) { box.appendChild(h('div', { class: 'skeleton' })); }
    return box;
  };

  KM.field = function (labelText, input, hint) {
    var id = 'f' + Math.random().toString(36).slice(2, 9);
    input.id = id;
    return h('div', { class: 'field' }, h('label', { for: id }, labelText), input, hint ? h('p', { class: 'hint' }, hint) : null);
  };

  KM.option = function (value, text, selected) {
    var o = h('option', { value: value }, text);
    if (selected) { o.selected = true; }
    return o;
  };

  // A switch (on/off) that works with the keyboard and screen readers.
  KM.switchControl = function (checked, label, onChange) {
    var input = h('input', { type: 'checkbox', role: 'switch', class: 'switch-input' });
    input.checked = !!checked;
    input.addEventListener('change', function () { onChange(input.checked, input); });
    return h('label', { class: 'switch' }, input, h('span', { class: 'switch-track' }, h('span', { class: 'switch-thumb' })), h('span', { class: 'switch-label' }, label));
  };

  // Makes a button that disables itself while it works, and shows any error as a toast.
  KM.button = function (label, cls, handler, iconName) {
    var btn = h('button', { type: 'button', class: 'btn ' + (cls || '') }, iconName ? KM.icon(iconName, 18) : null, label);
    btn.addEventListener('click', KM.wrap(btn, handler));
    return btn;
  };

  // Wraps an async action: stops double clicks, and shows errors nicely.
  KM.wrap = function (btn, handler) {
    return async function (event) {
      if (event && event.preventDefault) { event.preventDefault(); }
      if (btn) { btn.disabled = true; }
      try {
        await handler(event);
      } catch (e) {
        KM.toast(e && e.message ? e.message : 'Something went wrong. Please try again.', true);
      } finally {
        if (btn) { btn.disabled = false; }
      }
    };
  };

  var toastTimer = null;
  KM.toast = function (message, isError) {
    var el = $('#toast');
    el.textContent = message;
    el.className = 'show' + (isError ? ' error' : '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.className = ''; }, isError ? 6500 : 3800);
  };

  // ------------------------------------------------------------------ dialogs
  // Our own dialogs, so every question looks like part of the app (and works the same everywhere).
  function modal(build) {
    return new Promise(function (resolve) {
      var backdrop = h('div', { class: 'modal-backdrop' });
      var box = h('div', { class: 'modal', role: 'dialog', 'aria-modal': 'true' });
      backdrop.appendChild(box);
      function onKey(e) { if (e.key === 'Escape') { close(null); } }
      function close(result) {
        document.removeEventListener('keydown', onKey);
        if (backdrop.parentNode) { backdrop.parentNode.removeChild(backdrop); }
        resolve(result);
      }
      document.addEventListener('keydown', onKey);
      backdrop.addEventListener('click', function (e) { if (e.target === backdrop) { close(null); } });
      build(box, close);
      document.body.appendChild(backdrop);
      var first = box.querySelector('input, textarea, select');
      if (first && first.focus) { first.focus(); }
    });
  }

  // confirmDialog({title, message, confirmText, danger}) answers true or false.
  KM.confirmDialog = function (o) {
    return modal(function (box, close) {
      box.appendChild(h('h3', null, o.title || 'Are you sure?'));
      if (o.message) { box.appendChild(h('p', { class: 'muted' }, o.message)); }
      box.appendChild(h('div', { class: 'actions end' },
        h('button', { type: 'button', class: 'btn secondary', 'data-cancel': '1', onclick: function () { close(false); } }, o.cancelText || 'Cancel'),
        h('button', { type: 'button', class: 'btn ' + (o.danger ? 'danger' : ''), 'data-confirm': '1', onclick: function () { close(true); } }, o.confirmText || 'Confirm')));
    }).then(function (v) { return v === true; });
  };

  // askText({title, label, message, required, multiline, numeric, maxlength, confirmText}) answers a string or null.
  KM.askText = function (o) {
    return modal(function (box, close) {
      var input = o.multiline
        ? h('textarea', { maxlength: String(o.maxlength || 500), placeholder: o.placeholder || '' })
        : h('input', { type: 'text', maxlength: String(o.maxlength || 100), placeholder: o.placeholder || '', inputmode: o.numeric ? 'numeric' : null, autocomplete: 'off' });
      var error = h('p', { class: 'form-error', role: 'alert' });
      var form = h('form', null,
        h('h3', null, o.title || 'Please tell us'),
        o.message ? h('p', { class: 'muted' }, o.message) : null,
        KM.field(o.label || 'Your answer', input),
        error,
        h('div', { class: 'actions end' },
          h('button', { type: 'button', class: 'btn secondary', 'data-cancel': '1', onclick: function () { close(null); } }, 'Cancel'),
          h('button', { type: 'submit', class: 'btn', 'data-confirm': '1' }, o.confirmText || 'Send')));
      form.addEventListener('submit', function (e) {
        e.preventDefault();
        var value = input.value.trim();
        if (o.required !== false && !value) { error.textContent = 'Please fill this in.'; return; }
        if (o.numeric && !/^\d+$/.test(value)) { error.textContent = 'Use numbers only.'; return; }
        close(value);
      });
      box.appendChild(form);
    });
  };

  // ------------------------------------------------------------------ session
  var session = null;
  try { session = JSON.parse(sessionStorage.getItem('km_session')) || null; } catch (e) { session = null; }

  KM.getSession = function () { return session; };

  KM.saveSession = function (s) {
    session = s;
    if (s) { sessionStorage.setItem('km_session', JSON.stringify(s)); } else { sessionStorage.removeItem('km_session'); }
  };

  KM.homeFor = function (role) {
    if (role === 'CLIENT') { return '#/client/home'; }
    if (role === 'WORKER') { return '#/worker/jobs'; }
    return '#/admin/home';
  };

  KM.roleLabel = function (role) {
    if (role === 'CLIENT') { return 'Client'; }
    if (role === 'WORKER') { return 'Worker'; }
    return 'Staff';
  };

  KM.logout = async function () {
    // A worker who logs out goes offline first, so their saved position is erased.
    if (session && session.role === 'WORKER' && KM.isOnline()) {
      try { await KM.api('POST', '/workers/me/offline'); } catch (e) { /* we are leaving anyway */ }
    }
    KM.stopLocationTimer();
    sessionStorage.removeItem('km_online');
    KM.saveSession(null);
    KM.go('#/login');
  };

  // ------------------------------------------------------------------ talking to the server
  function ApiError(status, message) {
    this.name = 'ApiError';
    this.status = status;
    this.message = message;
  }
  ApiError.prototype = Object.create(Error.prototype);
  KM.ApiError = ApiError;

  function fallbackMessage(status) {
    if (status === 400) { return 'Please check the information you entered.'; }
    if (status === 401) { return 'Please log in.'; }
    if (status === 403) { return 'You are not allowed to do that.'; }
    if (status === 404) { return 'We could not find that.'; }
    if (status === 409) { return 'That is not possible right now.'; }
    if (status === 429) { return 'Too many attempts. Please wait a little and try again.'; }
    return 'Something went wrong on our side. Please try again.';
  }

  // Calls the API. Returns the data, or throws an ApiError with a message people can read.
  KM.api = async function (method, path, body, opts) {
    opts = opts || {};
    var headers = {};
    if (session && session.token) { headers['Authorization'] = 'Bearer ' + session.token; }
    var payload;
    if (typeof FormData !== 'undefined' && body instanceof FormData) {
      payload = body;   // the browser sets the right Content-Type for files
    } else if (body !== undefined && body !== null) {
      headers['Content-Type'] = 'application/json';
      payload = JSON.stringify(body);
    }

    var res;
    try {
      res = await fetch(API + path, { method: method, headers: headers, body: payload });
    } catch (e) {
      throw new ApiError(0, 'Cannot reach KasiMarket. Check your internet connection.');
    }

    if (!res.ok) {
      var message = '';
      try {
        var data = await res.json();
        message = data && data.message ? data.message : '';
      } catch (e) { /* the error had no readable body */ }

      // A 401 while we hold a token means the login ran out: send the person to log in again.
      if (res.status === 401 && session && !opts.keepSession) {
        KM.stopLocationTimer();
        KM.saveSession(null);
        KM.go('#/login');
        throw new ApiError(401, 'Your session ended. Please log in again.');
      }
      throw new ApiError(res.status, message || fallbackMessage(res.status));
    }

    if (opts.blob) { return res.blob(); }
    var text = await res.text();
    return text ? JSON.parse(text) : null;
  };

  // Public settings (for example: are the demo accounts switched on?). Never fails.
  var configPromise = null;
  KM.getConfig = function () {
    if (!configPromise) {
      configPromise = fetch(API + '/public/config')
        .then(function (r) { return r.ok ? r.json() : {}; })
        .catch(function () { return {}; });
    }
    return configPromise;
  };

  // ------------------------------------------------------------------ GPS
  KM.getPosition = function () {
    return new Promise(function (resolve, reject) {
      if (!navigator.geolocation) {
        reject(new Error('This device cannot share its location.'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        function (p) { resolve({ lat: p.coords.latitude, lng: p.coords.longitude }); },
        function () { reject(new Error('We could not get your location. Allow location in your browser, or use the demo location.')); },
        { enableHighAccuracy: false, timeout: 10000, maximumAge: 30000 }
      );
    });
  };

  // ---- the worker's "I am online" heartbeat ----
  // While online, the worker's position is sent every minute, so clients see a fresh arrival time.
  var locTimer = null;

  KM.isOnline = function () { return sessionStorage.getItem('km_online') === '1'; };

  KM.stopLocationTimer = function () {
    if (locTimer) { clearInterval(locTimer); locTimer = null; }
  };

  KM.startLocationTimer = function () {
    KM.stopLocationTimer();
    locTimer = setInterval(function () {
      KM.sendWorkerLocation().catch(function () { /* try again next minute */ });
    }, 60000);
  };

  KM.sendWorkerLocation = async function () {
    var mode = sessionStorage.getItem('km_loc_mode') || 'gps';
    var pos = mode === 'demo' ? KM.DEMO_WORKER_POS : await KM.getPosition();
    await KM.api('PUT', '/workers/me/location', { latitude: pos.lat, longitude: pos.lng });
  };

  KM.goOnline = async function (mode) {
    sessionStorage.setItem('km_loc_mode', mode);
    await KM.sendWorkerLocation();
    sessionStorage.setItem('km_online', '1');
    KM.startLocationTimer();
  };

  KM.goOffline = async function () {
    await KM.api('POST', '/workers/me/offline');
    sessionStorage.removeItem('km_online');
    KM.stopLocationTimer();
  };

  // ------------------------------------------------------------------ screen lifecycle
  var cleanups = [];
  KM.onLeave = function (fn) { cleanups.push(fn); };
  KM.runCleanups = function () {
    cleanups.forEach(function (fn) { try { fn(); } catch (e) { /* ignore */ } });
    cleanups = [];
  };

  // Runs a function now and then, but only while this screen is open and the tab is visible.
  KM.poll = function (fn, ms) {
    var timer = setInterval(function () { if (!document.hidden) { fn(); } }, ms);
    KM.onLeave(function () { clearInterval(timer); });
    return timer;
  };

  KM.go = function (hash) {
    if (location.hash === hash) { if (KM.route) { KM.route(); } } else { location.hash = hash; }
  };

  // Shows an error card with a "Try again" button.
  KM.showError = function (view, e) {
    KM.clear(view);
    view.appendChild(h('div', { class: 'card alert' },
      h('p', null, e && e.message ? e.message : 'Something went wrong.'),
      KM.button('Try again', 'secondary small', function () { KM.route(); })));
  };
})();
