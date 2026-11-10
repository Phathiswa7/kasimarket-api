/* KasiMarket front end.
   One small app in plain JavaScript: no framework and no build step.
   It talks to the KasiMarket API (/api/v1) and draws every screen into <main id="view">.

   Safety rule used all over this file: text that came from the server or from a user is
   ALWAYS added with textContent (through the h() helper below), never with innerHTML.
   That stops "script injection" (XSS) attacks. */

(function () {
  'use strict';

  var API = '/api/v1';

  // Fixed positions in Randburg, used for demos on a computer that has no GPS.
  var DEMO_WORKER_POS = { lat: -26.0936, lng: 28.0064 };
  var DEMO_CLIENT_POS = { lat: -26.1100, lng: 28.0200 };

  // How a job's status is shown to people.
  var STATUS_LABEL = {
    REQUESTED: 'Waiting for a worker', ACCEPTED: 'Accepted', EN_ROUTE: 'On the way',
    ARRIVED: 'Arrived', IN_PROGRESS: 'Work in progress', COMPLETED: 'Finished',
    PAID: 'Paid', CANCELLED: 'Cancelled', EXPIRED: 'Expired'
  };
  var STEPS = ['REQUESTED', 'ACCEPTED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'PAID'];
  var STEP_LABEL = ['Booked', 'Accepted', 'On the way', 'Arrived', 'Working', 'Finished', 'Paid'];
  var TERMINAL = ['PAID', 'CANCELLED', 'EXPIRED'];

  // ====================================================================
  // 1. Small helper tools
  // ====================================================================

  function $(selector) {
    return document.querySelector(selector);
  }

  function clear(el) {
    while (el.firstChild) {
      el.removeChild(el.firstChild);
    }
  }

  // Adds a child (a text, an element, or a list of them) to an element.
  function append(el, child) {
    if (child === null || child === undefined || child === false) {
      return;
    }
    if (Array.isArray(child)) {
      child.forEach(function (c) { append(el, c); });
    } else if (child instanceof Node) {
      el.appendChild(child);
    } else {
      el.appendChild(document.createTextNode(String(child)));
    }
  }

  // h('div', {class: 'card'}, 'Hello') builds <div class="card">Hello</div>.
  // Text is added as plain text, so it can never run as code.
  function h(tag, attrs) {
    var el = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (key) {
        var value = attrs[key];
        if (value === null || value === undefined || value === false) {
          return;
        }
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
    for (var i = 2; i < arguments.length; i++) {
      append(el, arguments[i]);
    }
    return el;
  }

  var toastTimer = null;
  function toast(message, isError) {
    var el = $('#toast');
    el.textContent = message;
    el.className = 'show' + (isError ? ' error' : '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.className = ''; }, isError ? 6000 : 3500);
  }

  function money(amount) {
    return 'R' + amount;
  }

  function fmtDate(iso) {
    if (!iso) { return ''; }
    var d = new Date(iso);
    return isNaN(d.getTime()) ? String(iso) : d.toLocaleString();
  }

  function statusBadge(status) {
    var cls = 'badge';
    if (status === 'PAID' || status === 'ACCEPTED' || status === 'APPROVED' || status === 'RESOLVED') { cls += ' good'; }
    else if (status === 'CANCELLED' || status === 'EXPIRED' || status === 'REJECTED' || status === 'SUSPENDED') { cls += ' bad'; }
    else if (status === 'REQUESTED' || status === 'PENDING' || status === 'SUBMITTED' || status === 'OPEN' || status === 'COMPLETED') { cls += ' warn'; }
    else { cls += ' info'; }
    return h('span', { class: cls }, STATUS_LABEL[status] || status);
  }

  function severityBadge(severity) {
    var cls = 'badge';
    if (severity === 'CRITICAL') { cls += ' bad'; }
    else if (severity === 'HIGH') { cls += ' warn'; }
    else { cls += ' info'; }
    return h('span', { class: cls }, severity);
  }

  // Makes a button that disables itself while it works, and shows any error as a toast.
  function button(label, cls, handler) {
    var btn = h('button', { type: 'button', class: 'btn ' + (cls || '') }, label);
    btn.addEventListener('click', wrap(btn, handler));
    return btn;
  }

  // Wraps an async action: stops double clicks, and shows errors nicely.
  function wrap(btn, handler) {
    return async function (event) {
      if (event && event.preventDefault) { event.preventDefault(); }
      if (btn) { btn.disabled = true; }
      try {
        await handler(event);
      } catch (e) {
        toast(e && e.message ? e.message : 'Something went wrong. Please try again.', true);
      } finally {
        if (btn) { btn.disabled = false; }
      }
    };
  }

  function field(labelText, input, hint) {
    var id = 'f' + Math.random().toString(36).slice(2, 9);
    input.id = id;
    return h('div', null, h('label', { for: id }, labelText), input, hint ? h('p', { class: 'muted' }, hint) : null);
  }

  function option(value, text, selected) {
    var o = h('option', { value: value }, text);
    if (selected) { o.selected = true; }
    return o;
  }

  // ====================================================================
  // 2. Session (who is logged in)
  // ====================================================================
  // The login token is kept in sessionStorage: it disappears when the browser tab closes.

  var session = loadSession();

  function loadSession() {
    try {
      return JSON.parse(sessionStorage.getItem('km_session')) || null;
    } catch (e) {
      return null;
    }
  }

  function saveSession(s) {
    session = s;
    if (s) {
      sessionStorage.setItem('km_session', JSON.stringify(s));
    } else {
      sessionStorage.removeItem('km_session');
    }
  }

  function homeFor(role) {
    if (role === 'CLIENT') { return '#/client/book'; }
    if (role === 'WORKER') { return '#/worker/jobs'; }
    return '#/admin/workers';
  }

  async function logout() {
    // A worker who logs out goes offline first, so their position is erased.
    if (session && session.role === 'WORKER' && sessionStorage.getItem('km_online') === '1') {
      try { await api('POST', '/workers/me/offline'); } catch (e) { /* we are leaving anyway */ }
    }
    stopLocationTimer();
    sessionStorage.removeItem('km_online');
    saveSession(null);
    go('#/login');
  }

  // ====================================================================
  // 3. Talking to the server
  // ====================================================================

  function ApiError(status, message) {
    this.name = 'ApiError';
    this.status = status;
    this.message = message;
  }
  ApiError.prototype = Object.create(Error.prototype);

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
  async function api(method, path, body, opts) {
    opts = opts || {};
    var headers = {};
    if (session && session.token) {
      headers['Authorization'] = 'Bearer ' + session.token;
    }
    var payload;
    if (typeof FormData !== 'undefined' && body instanceof FormData) {
      payload = body; // the browser sets the right Content-Type for files
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
        stopLocationTimer();
        saveSession(null);
        go('#/login');
        throw new ApiError(401, 'Your session ended. Please log in again.');
      }
      throw new ApiError(res.status, message || fallbackMessage(res.status));
    }

    if (opts.blob) {
      return res.blob();
    }
    var text = await res.text();
    return text ? JSON.parse(text) : null;
  }

  // ====================================================================
  // 4. Position (GPS) tools
  // ====================================================================

  function getPosition() {
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
  }

  // ---- the worker's "I am online" heartbeat ----
  // While online, the worker's position is sent every minute, so clients see a fresh arrival time.
  var locTimer = null;

  function isOnline() {
    return sessionStorage.getItem('km_online') === '1';
  }

  function stopLocationTimer() {
    if (locTimer) {
      clearInterval(locTimer);
      locTimer = null;
    }
  }

  function startLocationTimer() {
    stopLocationTimer();
    locTimer = setInterval(function () {
      sendWorkerLocation().catch(function () { /* try again next minute */ });
    }, 60000);
  }

  async function sendWorkerLocation() {
    var mode = sessionStorage.getItem('km_loc_mode') || 'gps';
    var pos = mode === 'demo' ? DEMO_WORKER_POS : await getPosition();
    await api('PUT', '/workers/me/location', { latitude: pos.lat, longitude: pos.lng });
  }

  async function goOnline(mode) {
    sessionStorage.setItem('km_loc_mode', mode);
    await sendWorkerLocation();
    sessionStorage.setItem('km_online', '1');
    startLocationTimer();
  }

  async function goOffline() {
    await api('POST', '/workers/me/offline');
    sessionStorage.removeItem('km_online');
    stopLocationTimer();
  }

  // ====================================================================
  // 5. Router: which screen to draw
  // ====================================================================

  var cleanups = [];

  function onLeave(fn) {
    cleanups.push(fn);
  }

  function runCleanups() {
    cleanups.forEach(function (fn) {
      try { fn(); } catch (e) { /* ignore */ }
    });
    cleanups = [];
  }

  // Runs a function now and then, but only while this screen is open and the tab is visible.
  function poll(fn, ms) {
    var timer = setInterval(function () {
      if (!document.hidden) {
        fn();
      }
    }, ms);
    onLeave(function () { clearInterval(timer); });
    return timer;
  }

  function go(hash) {
    if (location.hash === hash) {
      route();
    } else {
      location.hash = hash;
    }
  }

  function tabItems(role) {
    if (role === 'CLIENT') {
      return [['Book', '#/client/book', '#/client/book'],
              ['My jobs', '#/client/jobs', '#/client/job'],
              ['Reports', '#/reports', '#/reports']];
    }
    if (role === 'WORKER') {
      return [['Jobs', '#/worker/jobs', '#/worker/jobs'],
              ['My work', '#/worker/work', '#/worker/work'],
              ['Documents', '#/worker/docs', '#/worker/docs'],
              ['Reports', '#/reports', '#/reports']];
    }
    return [['Workers', '#/admin/workers', '#/admin/workers'],
            ['Incidents', '#/admin/incidents', '#/admin/incidents']];
  }

  function roleLabel(role) {
    if (role === 'CLIENT') { return 'Client'; }
    if (role === 'WORKER') { return 'Worker'; }
    return 'Admin';
  }

  function renderChrome() {
    var top = $('#topbar');
    clear(top);
    top.appendChild(h('a', { class: 'brand', href: session ? homeFor(session.role) : '#/login' },
      h('span', { class: 'logo', 'aria-hidden': 'true' }, 'K'), 'KasiMarket'));
    if (session) {
      top.appendChild(h('div', { class: 'who' },
        h('span', { class: 'role' }, roleLabel(session.role)),
        h('button', { class: 'link', type: 'button', onclick: logout }, 'Log out')));
    }

    var tabs = $('#tabs');
    clear(tabs);
    if (!session) {
      tabs.hidden = true;
      return;
    }
    tabs.hidden = false;
    var current = location.hash || '';
    tabItems(session.role).forEach(function (item) {
      tabs.appendChild(h('a', { href: item[1], class: current.indexOf(item[2]) === 0 ? 'active' : '' }, item[0]));
    });
  }

  function showError(view, e) {
    clear(view);
    view.appendChild(h('div', { class: 'card alert' },
      h('p', null, e && e.message ? e.message : 'Something went wrong.'),
      button('Try again', 'secondary small', function () { route(); })));
  }

  function route() {
    runCleanups();
    var path = (location.hash || '').replace(/^#\/?/, '');
    var seg = path.split('/');
    var view = $('#view');
    clear(view);
    renderChrome();
    window.scrollTo(0, 0);

    if (!session) {
      if (path === 'register') { renderRegister(view); } else { renderLogin(view); }
      return;
    }

    var role = session.role;
    var key = seg.slice(0, 2).join('/');
    var screen = null;

    if (key === 'reports' || seg[0] === 'reports') {
      if (role !== 'ADMIN') { screen = function () { return renderReports(view); }; }
    } else if (role === 'CLIENT' && key === 'client/book') {
      screen = function () { return renderBook(view); };
    } else if (role === 'CLIENT' && key === 'client/jobs') {
      screen = function () { return renderClientJobs(view); };
    } else if (role === 'CLIENT' && key === 'client/job') {
      screen = function () { return renderClientJob(view, seg[2]); };
    } else if (role === 'WORKER' && key === 'worker/jobs') {
      screen = function () { return renderWorkerJobs(view); };
    } else if (role === 'WORKER' && key === 'worker/work') {
      screen = function () { return renderWorkerWork(view); };
    } else if (role === 'WORKER' && key === 'worker/docs') {
      screen = function () { return renderWorkerDocs(view); };
    } else if (role === 'ADMIN' && key === 'admin/workers') {
      screen = function () { return renderAdminWorkers(view); };
    } else if (role === 'ADMIN' && key === 'admin/incidents') {
      screen = function () { return renderAdminIncidents(view); };
    }

    if (!screen) {
      go(homeFor(role));
      return;
    }
    view.appendChild(h('p', { class: 'loading' }, 'Loading...'));
    Promise.resolve()
      .then(function () { clear(view); return screen(); })
      .catch(function (e) { showError(view, e); });
  }

  // ====================================================================
  // 6. Log in and register
  // ====================================================================

  function renderLogin(view) {
    var email = h('input', { type: 'email', autocomplete: 'username', required: true, inputmode: 'email' });
    var password = h('input', { type: 'password', autocomplete: 'current-password', required: true });
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Log in');
    var form = h('form', null, field('Email', email), field('Password', password), submit);

    form.addEventListener('submit', wrap(submit, async function () {
      var result = await api('POST', '/auth/login', { email: email.value, password: password.value }, { keepSession: true });
      saveSession({ token: result.token, email: result.email, role: result.role });
      go(homeFor(result.role));
    }));

    view.appendChild(h('h1', null, 'Welcome to KasiMarket'));
    view.appendChild(h('p', { class: 'muted' }, 'Everyday help, from people in your community.'));
    view.appendChild(h('div', { class: 'card' }, form));
    view.appendChild(h('p', null, 'New here? ', h('a', { href: '#/register' }, 'Create an account')));
  }

  function renderRegister(view) {
    var role = 'CLIENT';
    var roleClient = h('input', { type: 'radio', name: 'role', value: 'CLIENT', checked: true });
    var roleWorker = h('input', { type: 'radio', name: 'role', value: 'WORKER' });
    roleClient.addEventListener('change', function () { role = 'CLIENT'; });
    roleWorker.addEventListener('change', function () { role = 'WORKER'; });

    var fullName = h('input', { type: 'text', autocomplete: 'name', required: true, maxlength: '100' });
    var email = h('input', { type: 'email', autocomplete: 'username', required: true, inputmode: 'email' });
    var phone = h('input', { type: 'tel', autocomplete: 'tel', inputmode: 'tel' });
    var password = h('input', { type: 'password', autocomplete: 'new-password', required: true, minlength: '8' });
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Create account');

    var form = h('form', null,
      h('div', { class: 'check' }, roleClient, h('label', { for: 'r1' }, 'I need help with a task')),
      h('div', { class: 'check' }, roleWorker, h('label', { for: 'r2' }, 'I want to earn money doing tasks')),
      field('Full name', fullName),
      field('Email', email),
      field('Phone (optional)', phone),
      field('Password', password, 'At least 8 characters.'),
      submit);
    roleClient.id = 'r1';
    roleWorker.id = 'r2';

    form.addEventListener('submit', wrap(submit, async function () {
      var body = { email: email.value, password: password.value, fullName: fullName.value, role: role };
      if (phone.value.trim()) { body.phone = phone.value.trim(); }
      await api('POST', '/auth/register', body, { keepSession: true });
      var result = await api('POST', '/auth/login', { email: email.value, password: password.value }, { keepSession: true });
      saveSession({ token: result.token, email: result.email, role: result.role });
      if (result.role === 'WORKER') {
        toast('Account created. Upload your ID and police clearance, then wait for approval.');
        go('#/worker/docs');
      } else {
        toast('Account created. Welcome!');
        go(homeFor(result.role));
      }
    }));

    view.appendChild(h('h1', null, 'Create your account'));
    view.appendChild(h('div', { class: 'card' }, form));
    view.appendChild(h('p', null, 'Already registered? ', h('a', { href: '#/login' }, 'Log in')));
  }

  // ====================================================================
  // 7. Safety: reports and SOS (used by clients and workers)
  // ====================================================================

  async function sendSos(jobId) {
    var sure = window.confirm('Send an SOS?\n\nKasiMarket staff will see it with your position. '
      + 'In danger, also call the police on 10111, or 112 from a mobile phone.');
    if (!sure) { return; }
    var body = {};
    try {
      var pos = await getPosition();
      body = { latitude: pos.lat, longitude: pos.lng };
    } catch (e) { /* the SOS must still work without a position */ }
    await api('POST', '/jobs/' + jobId + '/sos', body);
    toast('SOS sent. If you are in danger, call 10111 or 112 now.', true);
  }

  // The "Safety and help" box under a job.
  function safetyPanel(jobId, allowSos) {
    var severity = h('select', null, option('LOW', 'Small problem'), option('MEDIUM', 'Serious problem', true), option('HIGH', 'Unsafe or damage'));
    var description = h('textarea', { maxlength: '500', required: true, placeholder: 'What happened?' });
    var submit = h('button', { type: 'submit', class: 'btn secondary small' }, 'Send report');
    var form = h('form', null, field('How serious is it?', severity), field('What happened?', description), submit);

    form.addEventListener('submit', wrap(submit, async function () {
      await api('POST', '/jobs/' + jobId + '/incidents', { severity: severity.value, description: description.value });
      description.value = '';
      toast('Report sent. You can follow it under Reports.');
    }));

    var panel = h('details', { class: 'safety' },
      h('summary', null, 'Safety and help'),
      h('h3', null, 'Report a problem'),
      form);

    if (allowSos) {
      panel.appendChild(h('hr', { class: 'divider' }));
      panel.appendChild(h('div', { class: 'card sos' },
        h('h3', null, 'Emergency'),
        h('p', { class: 'muted' }, 'In danger, call 10111 (police), or 112 from a mobile phone. The SOS button tells KasiMarket staff, but staff may not see it straight away.'),
        button('SOS - I need help now', 'danger block', function () { return sendSos(jobId); })));
    }
    return panel;
  }

  async function renderReports(view) {
    view.appendChild(h('h1', null, 'My reports'));
    var list = await api('GET', '/incidents/mine');
    if (!list.length) {
      view.appendChild(h('p', { class: 'empty' }, 'You have not reported anything. You can report a problem from any job.'));
      return;
    }
    list.forEach(function (i) {
      view.appendChild(h('div', { class: 'card' + (i.sos ? ' sos' : '') },
        h('div', { class: 'row' }, h('strong', null, i.sos ? 'SOS' : 'Job #' + i.jobId), h('span', null, severityBadge(i.severity), ' ', statusBadge(i.status))),
        h('p', null, i.description),
        i.resolution ? h('p', null, h('strong', null, 'Answer from KasiMarket: '), i.resolution) : null,
        h('p', { class: 'muted' }, fmtDate(i.createdAt))));
    });
  }

  // ====================================================================
  // 8. The client: book a task
  // ====================================================================

  async function renderBook(view) {
    var tasks = await api('GET', '/catalog/task-types');
    view.appendChild(h('h1', null, 'Book a task'));

    var position = null;

    var taskSelect = h('select', null);
    tasks.forEach(function (t) {
      taskSelect.appendChild(option(String(t.id), t.name + ' (' + money(t.baseRatePerHour) + ' per hour, minimum ' + t.minHours + 'h)'));
    });
    var hours = h('input', { type: 'number', min: '1', max: '24', value: '1', inputmode: 'numeric' });
    var urgent = h('input', { type: 'checkbox' });
    var address = h('input', { type: 'text', maxlength: '200', required: true, autocomplete: 'street-address', placeholder: 'For example: 12 Main Road, Randburg' });
    var notes = h('textarea', { maxlength: '300', placeholder: 'Anything the worker should know?' });

    var posText = h('p', { class: 'muted' }, 'We need the position of the job to find workers near you.');
    var priceBox = h('div', { class: 'card notice' }, h('p', { class: 'muted' }, 'Choose a task to see the price.'));
    var bookBtn = h('button', { type: 'submit', class: 'btn block' }, 'Book now');

    function currentTask() {
      return tasks.filter(function (t) { return String(t.id) === taskSelect.value; })[0];
    }

    function syncHours() {
      var t = currentTask();
      if (!t) { return; }
      hours.min = String(t.minHours);
      if (Number(hours.value) < t.minHours) { hours.value = String(t.minHours); }
    }

    // Asks the server for the price whenever the task, hours or urgency change.
    var quoteCounter = 0;
    async function refreshQuote() {
      var t = currentTask();
      var n = Number(hours.value);
      if (!t || !n || n < 1) { return; }
      var mine = ++quoteCounter;
      try {
        var q = await api('POST', '/pricing/quote', { taskTypeId: t.id, hours: n, urgent: urgent.checked });
        if (mine !== quoteCounter) { return; } // a newer question was asked meanwhile
        clear(priceBox);
        priceBox.appendChild(h('div', { class: 'row' },
          h('div', null, h('div', { class: 'muted' }, q.hours + ' hour(s)' + (q.urgent ? ', urgent' : '')),
            h('div', { class: 'big' }, money(q.totalPrice))),
          h('div', { class: 'muted right' }, 'The price is fixed once you book.')));
      } catch (e) {
        if (mine === quoteCounter) {
          clear(priceBox);
          priceBox.appendChild(h('p', null, e.message));
        }
      }
    }

    taskSelect.addEventListener('change', function () { syncHours(); refreshQuote(); });
    hours.addEventListener('change', refreshQuote);
    urgent.addEventListener('change', refreshQuote);

    function setPosition(p, label) {
      position = p;
      posText.textContent = label;
    }

    var gpsBtn = button('Use my location', 'secondary small', async function () {
      var p = await getPosition();
      setPosition(p, 'Location saved from your device.');
    });
    var demoBtn = button('Use demo location (Randburg)', 'secondary small', async function () {
      setPosition(DEMO_CLIENT_POS, 'Demo location set: Randburg.');
    });

    var form = h('form', null,
      field('What do you need?', taskSelect),
      field('How many hours?', hours),
      h('div', { class: 'check' }, urgent, h('label', null, 'It is urgent (costs 30% more)')),
      field('Address', address),
      field('Notes (optional)', notes),
      h('h2', null, 'Where is the job?'),
      posText,
      h('div', { class: 'actions' }, gpsBtn, demoBtn),
      h('h2', null, 'Price'),
      priceBox,
      bookBtn);

    urgent.id = 'urgentBox';
    urgent.nextSibling.setAttribute('for', 'urgentBox');

    form.addEventListener('submit', wrap(bookBtn, async function () {
      if (!position) {
        throw new Error('Please share the location of the job first.');
      }
      var t = currentTask();
      var job = await api('POST', '/jobs', {
        taskTypeId: t.id,
        hours: Number(hours.value),
        urgent: urgent.checked,
        address: address.value,
        latitude: position.lat,
        longitude: position.lng,
        notes: notes.value.trim() ? notes.value.trim() : null
      });
      toast('Job booked. We are finding a worker near you.');
      go('#/client/job/' + job.id);
    }));

    view.appendChild(h('div', { class: 'card' }, form));
    syncHours();
    refreshQuote();
  }

  // ====================================================================
  // 9. The client: my jobs, and following one job
  // ====================================================================

  async function renderClientJobs(view) {
    view.appendChild(h('h1', null, 'My jobs'));
    var list = h('div');
    view.appendChild(list);

    async function load() {
      var jobs = await api('GET', '/jobs/mine');
      clear(list);
      if (!jobs.length) {
        list.appendChild(h('p', { class: 'empty' }, 'You have not booked anything yet.'));
        list.appendChild(h('a', { class: 'btn', href: '#/client/book' }, 'Book a task'));
        return;
      }
      jobs.forEach(function (j) {
        list.appendChild(h('a', { href: '#/client/job/' + j.id, style: 'text-decoration:none;color:inherit' },
          h('div', { class: 'card' },
            h('div', { class: 'row' }, h('strong', null, j.taskName), statusBadge(j.status)),
            h('div', { class: 'row' }, h('span', { class: 'muted' }, j.address), h('strong', null, money(j.totalPrice))),
            h('p', { class: 'muted' }, fmtDate(j.createdAt)))));
      });
    }

    await load();
    poll(function () { load().catch(function () { /* keep what we show */ }); }, 15000);
  }

  function stepsBar(status) {
    var index = STEPS.indexOf(status);
    var ol = h('ol', { class: 'steps' });
    STEPS.forEach(function (s, i) {
      var cls = i < index ? 'done' : (i === index ? 'current' : '');
      ol.appendChild(h('li', { class: cls }, STEP_LABEL[i]));
    });
    return ol;
  }

  async function renderClientJob(view, idText) {
    var id = Number(idText);
    view.appendChild(h('p', null, h('a', { href: '#/client/jobs' }, '< All my jobs')));

    var head = h('div', { class: 'card' });
    var statusBox = h('div', { class: 'card' });
    var actions = h('div');
    var safety = h('div');
    view.appendChild(statusBox);
    view.appendChild(actions);
    view.appendChild(head);
    view.appendChild(safety);

    var lastStatus = null;
    var photoUrl = null;
    onLeave(function () { if (photoUrl) { URL.revokeObjectURL(photoUrl); } });

    async function refresh() {
      var jobs = await api('GET', '/jobs/mine');
      var job = jobs.filter(function (j) { return j.id === id; })[0];
      if (!job) {
        clear(statusBox);
        statusBox.appendChild(h('p', null, 'We could not find this job.'));
        return true;
      }
      var t = await api('GET', '/jobs/' + id + '/tracking');

      clear(head);
      head.appendChild(h('div', { class: 'row' }, h('h3', null, job.taskName), h('strong', null, money(job.totalPrice))));
      head.appendChild(h('p', { class: 'muted' }, job.address + ' - ' + job.hours + ' hour(s)' + (job.urgent ? ' - urgent' : '')));
      head.appendChild(h('p', { class: 'muted' }, 'Booked ' + fmtDate(job.createdAt)));

      clear(statusBox);
      statusBox.appendChild(h('div', { class: 'statusmsg' }, t.message));
      if (t.etaMinutes) {
        statusBox.appendChild(h('span', { class: 'eta' }, 'About ' + t.etaMinutes + ' min away'));
      }
      if (t.workerName) {
        var rating = t.workerRating ? ' - rated ' + t.workerRating + ' / 5' : ' - new worker';
        statusBox.appendChild(h('p', { class: 'muted' }, 'Your worker: ' + t.workerName + rating + ' (' + t.workerJobsCompleted + ' jobs)'));
      }
      if (TERMINAL.indexOf(job.status) === -1 || job.status === 'PAID') {
        if (job.status !== 'CANCELLED' && job.status !== 'EXPIRED') { statusBox.appendChild(stepsBar(job.status)); }
      } else {
        statusBox.appendChild(statusBadge(job.status));
      }

      if (job.status !== lastStatus) {
        lastStatus = job.status;
        buildActions(job);
        clear(safety);
        safety.appendChild(safetyPanel(id, !!t.workerName && job.status !== 'CANCELLED' && job.status !== 'EXPIRED' && job.status !== 'PAID'));
      }
      return TERMINAL.indexOf(job.status) !== -1;
    }

    function buildActions(job) {
      clear(actions);
      var s = job.status;

      if (s === 'REQUESTED' || s === 'ACCEPTED' || s === 'EN_ROUTE' || s === 'ARRIVED') {
        actions.appendChild(h('div', { class: 'actions' },
          button('Cancel job', 'secondary', async function () {
            if (!window.confirm('Cancel this job?')) { return; }
            await api('POST', '/jobs/' + id + '/cancel');
            toast('Job cancelled.');
            await refresh();
          })));
      }

      if (s === 'EXPIRED') {
        actions.appendChild(h('div', { class: 'card notice' },
          h('p', null, 'No worker was free in time. Please try again.'),
          h('a', { class: 'btn', href: '#/client/book' }, 'Book again')));
      }

      if (s === 'COMPLETED') {
        var photoBox = h('div', null, h('p', { class: 'muted' }, 'Loading the photo of the finished work...'));
        var confirmCard = h('div', { class: 'card notice' },
          h('h3', null, 'The work is finished'),
          h('p', null, 'Please check the photo. Paying releases the money to your worker.'),
          photoBox,
          button('Confirm and pay ' + money(job.totalPrice), 'block', async function () {
            await api('POST', '/jobs/' + id + '/confirm');
            toast('Thank you. The job is paid.');
            await refresh();
          }));
        actions.appendChild(confirmCard);
        api('GET', '/jobs/' + id + '/photo', null, { blob: true }).then(function (blob) {
          photoUrl = URL.createObjectURL(blob);
          clear(photoBox);
          photoBox.appendChild(h('img', { class: 'proof', src: photoUrl, alt: 'Photo of the finished work' }));
        }).catch(function (e) {
          clear(photoBox);
          photoBox.appendChild(h('p', null, e.message));
        });
      }

      if (s === 'PAID') {
        actions.appendChild(ratingForm(id));
      }
    }

    var done = await refresh();
    if (!done) {
      var timer = poll(function () {
        refresh().then(function (isDone) { if (isDone) { clearInterval(timer); } }).catch(function () { /* try again soon */ });
      }, 5000);
    }
  }

  function ratingForm(jobId) {
    function scale() {
      var s = h('select', null);
      [5, 4, 3, 2, 1].forEach(function (n) { s.appendChild(option(String(n), n + (n === 5 ? ' - excellent' : (n === 1 ? ' - poor' : '')))); });
      return s;
    }
    var punctuality = scale();
    var quality = scale();
    var communication = scale();
    var comment = h('textarea', { maxlength: '500', placeholder: 'Anything to add? (optional)' });
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Send my rating');
    var form = h('form', null,
      field('Was the worker on time?', punctuality),
      field('How good was the work?', quality),
      field('How was the communication?', communication),
      field('Comment', comment),
      submit);
    var card = h('div', { class: 'card' }, h('h3', null, 'Rate your worker'), form);

    form.addEventListener('submit', wrap(submit, async function () {
      try {
        await api('POST', '/jobs/' + jobId + '/rating', {
          punctuality: Number(punctuality.value),
          quality: Number(quality.value),
          communication: Number(communication.value),
          comment: comment.value.trim() ? comment.value.trim() : null
        });
        toast('Thank you for rating.');
      } catch (e) {
        if (e.status !== 409) { throw e; }
        toast('You have already rated this job.');
      }
      clear(card);
      card.appendChild(h('p', null, 'Thank you. Your rating helps keep KasiMarket safe.'));
    }));
    return card;
  }

  // ====================================================================
  // 10. The worker: go online, find jobs, do the work
  // ====================================================================

  async function renderWorkerJobs(view) {
    view.appendChild(h('h1', null, 'Jobs near you'));

    var statusCard = h('div', { class: 'card' });
    var list = h('div');
    view.appendChild(statusCard);
    view.appendChild(list);

    function drawStatus() {
      clear(statusCard);
      if (isOnline()) {
        statusCard.appendChild(h('div', { class: 'row' },
          h('div', null, h('strong', null, 'You are online'), h('p', { class: 'muted' }, 'Your position is shared while you are online, so nearby clients can find you.')),
          button('Go offline', 'secondary small', async function () {
            await goOffline();
            toast('You are offline. Your position was erased.');
            drawStatus();
            await load();
          })));
      } else {
        statusCard.appendChild(h('p', null, h('strong', null, 'You are offline.'), ' Go online to see jobs close to you.'));
        statusCard.appendChild(h('div', { class: 'actions' },
          button('Go online (my location)', '', async function () {
            await goOnline('gps');
            drawStatus();
            await load();
          }),
          button('Go online (demo location)', 'secondary', async function () {
            await goOnline('demo');
            drawStatus();
            await load();
          })));
      }
    }

    async function load() {
      clear(list);
      if (!isOnline()) {
        list.appendChild(h('p', { class: 'empty' }, 'Go online to see jobs.'));
        return;
      }
      var jobs;
      try {
        jobs = await api('GET', '/jobs/available');
      } catch (e) {
        if (e.status === 409) {
          sessionStorage.removeItem('km_online');
          stopLocationTimer();
          drawStatus();
          list.appendChild(h('p', { class: 'empty' }, 'Go online first, so we know where you are.'));
          return;
        }
        if (e.status === 403) {
          list.appendChild(h('div', { class: 'card notice' },
            h('p', null, e.message),
            h('a', { class: 'btn', href: '#/worker/docs' }, 'Upload my documents')));
          return;
        }
        throw e;
      }
      if (!jobs.length) {
        list.appendChild(h('p', { class: 'empty' }, 'No jobs near you right now. This page refreshes by itself.'));
        return;
      }
      jobs.forEach(function (j) {
        list.appendChild(h('div', { class: 'card' },
          h('div', { class: 'row' }, h('strong', null, j.taskName), j.urgent ? h('span', { class: 'badge warn' }, 'Urgent') : null),
          h('div', { class: 'row' },
            h('span', { class: 'muted' }, 'About ' + j.distanceKm + ' km away - ' + j.hours + ' hour(s)'),
            h('span', { class: 'big' }, money(j.workerEarning))),
          h('p', { class: 'muted' }, 'You earn ' + money(j.workerEarning) + '. The address is shown after you accept.'),
          button('Accept this job', 'block', async function () {
            await api('POST', '/jobs/' + j.id + '/accept');
            toast('Job accepted.');
            go('#/worker/work');
          })));
      });
    }

    drawStatus();
    try {
      await load();
    } catch (e) {
      list.appendChild(h('p', null, e.message));
    }
    poll(function () { if (isOnline()) { load().catch(function () { /* keep what we show */ }); } }, 10000);
  }

  // The next button a worker sees, for each stage of a job.
  var NEXT_STEP = {
    ACCEPTED: ['EN_ROUTE', 'I am on my way'],
    EN_ROUTE: ['ARRIVED', 'I have arrived'],
    ARRIVED: ['IN_PROGRESS', 'Start the work']
  };

  async function renderWorkerWork(view) {
    view.appendChild(h('h1', null, 'My work'));
    var list = h('div');
    view.appendChild(list);

    async function load() {
      var jobs = await api('GET', '/jobs/my-work');
      clear(list);
      if (!jobs.length) {
        list.appendChild(h('p', { class: 'empty' }, 'You have not accepted any jobs yet.'));
        list.appendChild(h('a', { class: 'btn', href: '#/worker/jobs' }, 'Find a job'));
        return;
      }
      jobs.forEach(function (j) { list.appendChild(workCard(j, load)); });
    }

    await load();
    poll(function () {
      // Do not redraw while the worker is choosing a photo or typing a report.
      if (list.querySelector('details[open]') || list.querySelector('input[type="file"]:focus')) { return; }
      load().catch(function () { /* keep what we show */ });
    }, 15000);
  }

  function workCard(j, reload) {
    var card = h('div', { class: 'card' + (j.status === 'IN_PROGRESS' ? ' notice' : '') },
      h('div', { class: 'row' }, h('strong', null, j.taskName), statusBadge(j.status)),
      h('p', null, h('strong', null, 'Address: '), j.address),
      j.notes ? h('p', null, h('strong', null, 'Notes: '), j.notes) : null,
      h('p', { class: 'muted' }, j.hours + ' hour(s)' + (j.urgent ? ' - urgent' : '') + ' - you earn ' + money(j.workerEarning)),
      h('a', { class: 'btn secondary small', target: '_blank', rel: 'noopener noreferrer',
               href: 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(j.address) }, 'Open in Maps'));

    var actions = h('div', { class: 'actions' });
    var next = NEXT_STEP[j.status];

    if (next) {
      actions.appendChild(button(next[1], '', async function () {
        await api('POST', '/jobs/' + j.id + '/status', { status: next[0] });
        // While travelling, the client sees how far away the worker is, so send a fresh position.
        if (next[0] === 'EN_ROUTE' && isOnline()) { sendWorkerLocation().catch(function () { /* ignore */ }); }
        toast('Updated.');
        await reload();
      }));
      actions.appendChild(button('Drop this job', 'secondary', async function () {
        if (!window.confirm('Give this job back? It goes back to the list for other workers.')) { return; }
        await api('POST', '/jobs/' + j.id + '/drop');
        toast('The job went back to the list.');
        await reload();
      }));
    }

    if (j.status === 'IN_PROGRESS') {
      var file = h('input', { type: 'file', accept: 'image/*', capture: 'environment' });
      var finish = h('button', { type: 'button', class: 'btn block' }, 'Finish the job with this photo');
      finish.addEventListener('click', wrap(finish, async function () {
        if (!file.files || !file.files[0]) {
          throw new Error('Take or choose a photo of the finished work first.');
        }
        var form = new FormData();
        form.append('file', file.files[0]);
        await api('POST', '/jobs/' + j.id + '/complete', form);
        toast('Job finished. Now we wait for the client to confirm.');
        await reload();
      }));
      card.appendChild(field('Photo of the finished work', file, 'A JPEG or PNG picture.'));
      card.appendChild(finish);
    }

    if (j.status === 'COMPLETED') {
      card.appendChild(h('p', { class: 'muted' }, 'Finished. Waiting for the client to confirm and pay.'));
    }
    if (j.status === 'PAID') {
      card.appendChild(h('p', { class: 'muted' }, 'Paid. You earned ' + money(j.workerEarning) + '.'));
    }
    if (j.status === 'CANCELLED') {
      card.appendChild(h('p', { class: 'muted' }, 'The client cancelled this job.'));
    }

    if (actions.childNodes.length) { card.appendChild(actions); }

    var active = ['ACCEPTED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED'].indexOf(j.status) !== -1;
    if (active) {
      card.appendChild(safetyPanel(j.id, true));
    }
    return card;
  }

  var DOC_LABEL = { ID_DOCUMENT: 'ID document', POLICE_CLEARANCE: 'Police clearance', REFERENCE_LETTER: 'Reference letter' };

  async function renderWorkerDocs(view) {
    view.appendChild(h('h1', null, 'My documents'));
    view.appendChild(h('div', { class: 'card notice' },
      h('p', null, 'To be approved you need an accepted ID document and a police clearance that is still valid. A KasiMarket admin checks every document.'),
      h('p', { class: 'muted' }, 'Only admins can open your documents.')));

    var list = h('div');
    var type = h('select', null,
      option('ID_DOCUMENT', 'ID document'),
      option('POLICE_CLEARANCE', 'Police clearance certificate'),
      option('REFERENCE_LETTER', 'Reference letter'));
    var file = h('input', { type: 'file', accept: '.pdf,.jpg,.jpeg,.png,image/*,application/pdf' });
    var validUntil = h('input', { type: 'date' });
    var validField = field('Valid until', validUntil, 'Write the date on the certificate.');
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Upload');
    var form = h('form', null, field('Which document?', type), field('The file (PDF, JPEG or PNG)', file), validField, submit);

    function syncValid() {
      validField.hidden = type.value !== 'POLICE_CLEARANCE';
    }
    type.addEventListener('change', syncValid);
    syncValid();

    form.addEventListener('submit', wrap(submit, async function () {
      if (!file.files || !file.files[0]) {
        throw new Error('Choose a file first.');
      }
      var data = new FormData();
      data.append('type', type.value);
      if (type.value === 'POLICE_CLEARANCE' && validUntil.value) { data.append('validUntil', validUntil.value); }
      data.append('file', file.files[0]);
      await api('POST', '/workers/me/documents', data);
      toast('Uploaded. An admin will check it.');
      form.reset();
      syncValid();
      await load();
    }));

    async function load() {
      var docs = await api('GET', '/workers/me/documents');
      clear(list);
      if (!docs.length) {
        list.appendChild(h('p', { class: 'empty' }, 'You have not uploaded anything yet.'));
        return;
      }
      docs.forEach(function (d) {
        list.appendChild(h('div', { class: 'card' },
          h('div', { class: 'row' }, h('strong', null, DOC_LABEL[d.type] || d.type), statusBadge(d.reviewStatus)),
          d.validUntil ? h('p', { class: 'muted' }, 'Valid until ' + d.validUntil) : null,
          d.reviewNote ? h('p', null, h('strong', null, 'Admin note: '), d.reviewNote) : null,
          h('p', { class: 'muted' }, 'Uploaded ' + fmtDate(d.uploadedAt))));
      });
    }

    view.appendChild(h('div', { class: 'card' }, h('h3', null, 'Upload a document'), form));
    view.appendChild(h('h2', null, 'Uploaded documents'));
    view.appendChild(list);
    await load();
  }

  // ====================================================================
  // 11. The admin: workers and incidents
  // ====================================================================

  async function renderAdminWorkers(view) {
    view.appendChild(h('h1', null, 'Workers'));
    var filter = h('select', null,
      option('', 'All workers'), option('SUBMITTED', 'Waiting for approval', true),
      option('APPROVED', 'Approved'), option('REJECTED', 'Rejected'), option('SUSPENDED', 'Suspended'));
    var list = h('div');
    view.appendChild(field('Show', filter));
    view.appendChild(list);

    async function load() {
      var query = filter.value ? '?status=' + encodeURIComponent(filter.value) : '';
      var workers = await api('GET', '/admin/workers' + query);
      clear(list);
      if (!workers.length) {
        list.appendChild(h('p', { class: 'empty' }, 'No workers in this list.'));
        return;
      }
      workers.forEach(function (w) { list.appendChild(workerCard(w, load)); });
    }

    filter.addEventListener('change', function () { load().catch(function (e) { toast(e.message, true); }); });
    await load();
  }

  function workerCard(w, reload) {
    function decide(status, question) {
      return async function () {
        if (question && !window.confirm(question)) { return; }
        await api('PUT', '/admin/workers/' + w.id + '/decision', { status: status });
        toast('Worker updated.');
        await reload();
      };
    }

    var card = h('div', { class: 'card' },
      h('div', { class: 'row' }, h('strong', null, w.fullName), statusBadge(w.vettingStatus)),
      h('p', { class: 'muted' }, w.email + (w.area ? ' - ' + w.area : '') + ' - tier ' + w.tier));

    var actions = h('div', { class: 'actions' });
    if (w.vettingStatus !== 'APPROVED') { actions.appendChild(button('Approve', 'small', decide('APPROVED'))); }
    if (w.vettingStatus !== 'REJECTED') { actions.appendChild(button('Reject', 'secondary small', decide('REJECTED', 'Reject ' + w.fullName + '?'))); }
    if (w.vettingStatus !== 'SUSPENDED') { actions.appendChild(button('Suspend', 'danger small', decide('SUSPENDED', 'Suspend ' + w.fullName + '?'))); }
    card.appendChild(actions);

    // The worker's documents are loaded only when the admin opens this box.
    var docsBox = h('div');
    var docsDetails = h('details', null, h('summary', null, 'Documents'), docsBox);
    var loaded = false;
    docsDetails.addEventListener('toggle', function () {
      if (docsDetails.open && !loaded) {
        loaded = true;
        loadWorkerDocs(w.id, docsBox);
      }
    });
    card.appendChild(docsDetails);

    if (w.vettingStatus === 'APPROVED') {
      card.appendChild(tierBox(w, reload));
    }
    return card;
  }

  async function loadWorkerDocs(profileId, box) {
    box.appendChild(h('p', { class: 'muted' }, 'Loading...'));
    try {
      var docs = await api('GET', '/admin/vetting/workers/' + profileId + '/documents');
      clear(box);
      if (!docs.length) {
        box.appendChild(h('p', { class: 'muted' }, 'No documents uploaded yet.'));
        return;
      }
      docs.forEach(function (d) { box.appendChild(adminDocCard(d, function () { loadWorkerDocs(profileId, box); })); });
    } catch (e) {
      clear(box);
      box.appendChild(h('p', null, e.message));
    }
  }

  function extensionFor(contentType) {
    if (contentType === 'application/pdf') { return '.pdf'; }
    if (contentType === 'image/png') { return '.png'; }
    return '.jpg';
  }

  function adminDocCard(d, reload) {
    var card = h('div', { class: 'card' },
      h('div', { class: 'row' }, h('strong', null, DOC_LABEL[d.type] || d.type), statusBadge(d.reviewStatus)),
      d.validUntil ? h('p', { class: 'muted' }, 'Valid until ' + d.validUntil) : null,
      d.reviewNote ? h('p', null, 'Note: ' + d.reviewNote) : null);

    var actions = h('div', { class: 'actions' });
    actions.appendChild(button('Download', 'secondary small', async function () {
      var blob = await api('GET', '/admin/vetting/documents/' + d.id + '/file', null, { blob: true });
      var url = URL.createObjectURL(blob);
      var link = h('a', { href: url, download: 'document-' + d.id + extensionFor(d.contentType) });
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
    }));

    if (d.reviewStatus === 'PENDING') {
      actions.appendChild(button('Accept', 'small', async function () {
        await api('PUT', '/admin/vetting/documents/' + d.id + '/review', { status: 'ACCEPTED' });
        toast('Document accepted.');
        reload();
      }));
      actions.appendChild(button('Reject', 'danger small', async function () {
        var note = window.prompt('Why is this document rejected? The worker will read this.');
        if (!note || !note.trim()) { return; }
        await api('PUT', '/admin/vetting/documents/' + d.id + '/review', { status: 'REJECTED', note: note.trim() });
        toast('Document rejected.');
        reload();
      }));
    }
    card.appendChild(actions);
    return card;
  }

  function tierBox(w, reload) {
    var tier = h('select', null, option('3', 'Tier 3 (babysitting, house watching)'), option('2', 'Tier 2 (back to automatic)'));
    var refs = h('input', { type: 'checkbox' });
    var checks = h('input', { type: 'checkbox' });
    var submit = h('button', { type: 'submit', class: 'btn small' }, 'Change tier');
    var form = h('form', null,
      field('New tier', tier),
      h('div', { class: 'check' }, refs, h('label', null, 'I verified the references myself')),
      h('div', { class: 'check' }, checks, h('label', null, 'The enhanced background check is complete')),
      submit);
    refs.id = 'refs' + w.id;
    refs.nextSibling.setAttribute('for', refs.id);
    checks.id = 'checks' + w.id;
    checks.nextSibling.setAttribute('for', checks.id);

    form.addEventListener('submit', wrap(submit, async function () {
      await api('PUT', '/admin/workers/' + w.id + '/tier', {
        tier: Number(tier.value), referencesVerified: refs.checked, enhancedCheckDone: checks.checked
      });
      toast('Tier changed.');
      await reload();
    }));
    return h('details', null, h('summary', null, 'Change tier'),
      h('p', { class: 'muted' }, 'Tier 3 also needs a strong track record. The system checks this for you.'), form);
  }

  async function renderAdminIncidents(view) {
    view.appendChild(h('h1', null, 'Incidents'));
    var filter = h('select', null, option('OPEN', 'Open', true), option('INVESTIGATING', 'Being investigated'), option('RESOLVED', 'Resolved'), option('', 'All'));
    var list = h('div');
    view.appendChild(field('Show', filter));
    view.appendChild(list);

    async function load() {
      var query = filter.value ? '?status=' + encodeURIComponent(filter.value) : '';
      var incidents = await api('GET', '/admin/incidents' + query);
      clear(list);
      if (!incidents.length) {
        list.appendChild(h('p', { class: 'empty' }, 'Nothing here. Good news.'));
        return;
      }
      incidents.forEach(function (i) { list.appendChild(incidentCard(i, load)); });
    }

    filter.addEventListener('change', function () { load().catch(function (e) { toast(e.message, true); }); });
    await load();
    poll(function () { load().catch(function () { /* keep what we show */ }); }, 10000);
  }

  function incidentCard(i, reload) {
    var card = h('div', { class: 'card' + (i.sos ? ' sos' : (i.severity === 'CRITICAL' ? ' alert' : '')) },
      h('div', { class: 'row' }, h('strong', null, (i.sos ? 'SOS - ' : '') + 'Job #' + i.jobId), h('span', null, severityBadge(i.severity), ' ', statusBadge(i.status))),
      h('p', null, i.description),
      h('p', { class: 'muted' }, 'Reported by ' + i.reportedBy + ' (' + i.reporterRole + ')' + (i.otherParty ? ' about ' + i.otherParty : '')),
      h('p', { class: 'muted' }, fmtDate(i.createdAt)));

    if (i.latitude !== null && i.latitude !== undefined && i.longitude !== null && i.longitude !== undefined) {
      var lat = Number(i.latitude);
      var lng = Number(i.longitude);
      card.appendChild(h('p', null, h('a', { target: '_blank', rel: 'noopener noreferrer',
        href: 'https://www.openstreetmap.org/?mlat=' + lat + '&mlon=' + lng + '#map=16/' + lat + '/' + lng }, 'Open the position on a map')));
    }
    if (i.resolution) {
      card.appendChild(h('p', null, h('strong', null, 'Resolution: '), i.resolution));
    }

    var actions = h('div', { class: 'actions' });
    if (i.status === 'OPEN') {
      actions.appendChild(button('Start investigating', 'secondary small', async function () {
        await api('PUT', '/admin/incidents/' + i.id, { status: 'INVESTIGATING' });
        await reload();
      }));
    }
    if (i.status !== 'RESOLVED') {
      actions.appendChild(button('Resolve', 'small', async function () {
        var text = window.prompt('How was this resolved? This is kept on record, and the person who reported it can read it.');
        if (!text || !text.trim()) { return; }
        await api('PUT', '/admin/incidents/' + i.id, { status: 'RESOLVED', resolution: text.trim() });
        toast('Incident resolved.');
        await reload();
      }));
    }
    card.appendChild(actions);
    return card;
  }

  // ====================================================================
  // 12. Start the app
  // ====================================================================

  window.addEventListener('hashchange', route);

  // If a worker was online before the page was reloaded, keep sending their position.
  if (session && session.role === 'WORKER' && isOnline()) {
    startLocationTimer();
  }

  route();
})();
