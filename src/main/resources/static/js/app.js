/* KasiMarket front end: the router and the sign-in screens.
   It decides which screen to draw for the address in the browser (the part after the #),
   draws the top bar and the bottom menu, and starts everything. */

(function () {
  'use strict';

  var KM = window.KM;
  var h = KM.h;
  var icon = KM.icon;

  // ====================================================================
  // Log in and register
  // ====================================================================
  async function doLogin(email, password) {
    var result = await KM.api('POST', '/auth/login', { email: email, password: password }, { keepSession: true });
    KM.saveSession({ token: result.token, email: result.email, role: result.role });
    KM.go(KM.homeFor(result.role));
  }

  KM.views.login = async function (view) {
    var config = await KM.getConfig();

    view.appendChild(h('div', { class: 'hero landing' },
      h('div', { class: 'brand-mark' }, 'K'),
      h('h1', null, 'Everyday help, from people in your community'),
      h('p', null, 'Book a task near you. Approved, tracked workers. Fixed prices.')));

    var features = h('ul', { class: 'feature-row' });
    [['shield', 'Workers approved by staff'], ['key', 'Start code at your door'], ['coin', 'Fixed, fair prices']].forEach(function (f) {
      features.appendChild(h('li', null, h('span', { class: 'point-icon' }, icon(f[0], 18)), f[1]));
    });
    view.appendChild(features);

    var email = h('input', { type: 'email', autocomplete: 'username', required: true, inputmode: 'email', placeholder: 'you@example.com' });
    var password = h('input', { type: 'password', autocomplete: 'current-password', required: true });
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Log in');
    var form = h('form', null, KM.field('Email', email), KM.field('Password', password), submit);
    form.addEventListener('submit', KM.wrap(submit, function () { return doLogin(email.value, password.value); }));
    view.appendChild(h('div', { class: 'card' }, h('h2', null, 'Log in'),
      form,
      h('p', { class: 'muted small' }, 'KasiMarket staff use the same form. Staff accounts are created by KasiMarket, not through sign-up.')));

    // Demo accounts, only when the server says development mode is on.
    if (config.devMode && config.demoAccounts && config.demoAccounts.length) {
      var demo = h('div', { class: 'card demo' },
        h('h3', null, icon('zap', 18), 'Try a demo account'),
        h('p', { class: 'muted small' }, 'Development mode only. These accounts are switched off in production.'));
      var buttons = h('div', { class: 'demo-grid' });
      config.demoAccounts.forEach(function (a) {
        buttons.appendChild(KM.button(a.label, 'secondary small', function () { return doLogin(a.email, a.password); }));
      });
      demo.appendChild(buttons);
      view.appendChild(demo);
    }

    view.appendChild(h('p', { class: 'center' }, 'New here? ', h('a', { href: '#/register' }, 'Create an account')));
  };

  // A very simple password strength guide (length and a mix of characters).
  function strength(pw) {
    var score = 0;
    if (pw.length >= 8) { score++; }
    if (pw.length >= 12) { score++; }
    if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) { score++; }
    if (/\d/.test(pw)) { score++; }
    if (/[^A-Za-z0-9]/.test(pw)) { score++; }
    return score;
  }

  KM.views.register = function (view) {
    var role = 'CLIENT';
    view.appendChild(h('h1', null, 'Create your account'));

    // Two big choices instead of two small radio buttons.
    var tiles = h('div', { class: 'role-tiles' });
    function drawTiles() {
      KM.clear(tiles);
      [['CLIENT', 'search', 'I need help', 'Book tasks near me'], ['WORKER', 'zap', 'I want to earn', 'Do tasks near me']].forEach(function (r) {
        var b = h('button', { type: 'button', class: 'role-tile' + (role === r[0] ? ' selected' : ''), 'aria-pressed': role === r[0] ? 'true' : 'false' },
          icon(r[1], 26), h('strong', null, r[2]), h('span', { class: 'muted small' }, r[3]));
        b.addEventListener('click', function () { role = r[0]; drawTiles(); });
        tiles.appendChild(b);
      });
    }
    drawTiles();
    view.appendChild(tiles);

    var fullName = h('input', { type: 'text', autocomplete: 'name', required: true, maxlength: '100' });
    var email = h('input', { type: 'email', autocomplete: 'username', required: true, inputmode: 'email' });
    var phone = h('input', { type: 'tel', autocomplete: 'tel', inputmode: 'tel' });
    var password = h('input', { type: 'password', autocomplete: 'new-password', required: true, minlength: '8' });
    var meter = h('div', { class: 'meter' }, h('span'));
    var meterText = h('p', { class: 'hint' }, 'Use at least 8 characters. Longer is stronger.');
    password.addEventListener('input', function () {
      var s = strength(password.value);
      meter.firstChild.style.width = (s * 20) + '%';
      meter.className = 'meter s' + s;
      meterText.textContent = password.value ? (s <= 2 ? 'Weak. Add length, numbers or capital letters.' : (s === 3 ? 'Good.' : 'Strong.')) : 'Use at least 8 characters. Longer is stronger.';
    });
    var terms = h('input', { type: 'checkbox', id: 'terms', required: true });
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Create account');

    var form = h('form', null,
      KM.field('Full name', fullName),
      KM.field('Email', email),
      KM.field('Phone (optional)', phone),
      KM.field('Password', password), meter, meterText,
      h('div', { class: 'check' }, terms, h('label', { for: 'terms' }, 'I agree to use KasiMarket respectfully and to the safety rules.')),
      submit);

    form.addEventListener('submit', KM.wrap(submit, async function () {
      var body = { email: email.value, password: password.value, fullName: fullName.value, role: role };
      if (phone.value.trim()) { body.phone = phone.value.trim(); }
      await KM.api('POST', '/auth/register', body, { keepSession: true });
      var result = await KM.api('POST', '/auth/login', { email: email.value, password: password.value }, { keepSession: true });
      KM.saveSession({ token: result.token, email: result.email, role: result.role });
      if (result.role === 'WORKER') {
        KM.toast('Account created. Upload your ID and police clearance, then wait for approval.');
        KM.go('#/worker/docs');
      } else {
        KM.toast('Account created. Welcome!');
        KM.go(KM.homeFor(result.role));
      }
    }));
    view.appendChild(h('div', { class: 'card' }, form));
    view.appendChild(h('p', { class: 'center' }, 'Already registered? ', h('a', { href: '#/login' }, 'Log in')));
  };

  // ====================================================================
  // The top bar and the bottom menu
  // ====================================================================
  function tabItems(role) {
    if (role === 'CLIENT') {
      return [['Home', '#/client/home', '#/client/home', 'home'], ['Book', '#/client/book', '#/client/book', 'plus'],
              ['Jobs', '#/client/jobs', '#/client/job', 'list'], ['Reports', '#/reports', '#/reports', 'flag'], ['Safety', '#/safety', '#/safety', 'shield']];
    }
    if (role === 'WORKER') {
      return [['Jobs', '#/worker/jobs', '#/worker/jobs', 'search'], ['My work', '#/worker/work', '#/worker/work', 'zap'],
              ['Documents', '#/worker/docs', '#/worker/docs', 'file'], ['Reports', '#/reports', '#/reports', 'flag'], ['Safety', '#/safety', '#/safety', 'shield']];
    }
    return [['Home', '#/admin/home', '#/admin/home', 'chart'], ['Workers', '#/admin/workers', '#/admin/workers', 'users'],
            ['Incidents', '#/admin/incidents', '#/admin/incidents', 'alert'], ['Tasks', '#/admin/tasks', '#/admin/tasks', 'sliders'],
            ['Controls', '#/admin/controls', '#/admin/controls', 'flag']];
  }

  function renderChrome() {
    var session = KM.getSession();
    var top = KM.$('#topbar');
    KM.clear(top);
    top.className = session && session.role === 'ADMIN' ? 'staff' : '';
    top.appendChild(h('a', { class: 'brand', href: session ? KM.homeFor(session.role) : '#/login' },
      h('span', { class: 'logo', 'aria-hidden': 'true' }, 'K'), 'KasiMarket'));
    if (session) {
      top.appendChild(h('div', { class: 'who' },
        h('span', { class: 'role' }, KM.roleLabel(session.role)),
        h('button', { class: 'link', type: 'button', onclick: KM.logout, 'aria-label': 'Log out' }, icon('logout', 18))));
    }

    var tabs = KM.$('#tabs');
    KM.clear(tabs);
    if (!session) { tabs.hidden = true; return; }
    tabs.hidden = false;
    var current = location.hash || '';
    tabItems(session.role).forEach(function (item) {
      var active = current.indexOf(item[2]) === 0;
      tabs.appendChild(h('a', { href: item[1], class: active ? 'active' : '', 'aria-current': active ? 'page' : null }, icon(item[3], 22), h('span', null, item[0])));
    });
  }

  // ====================================================================
  // The router
  // ====================================================================
  KM.route = function () {
    KM.runCleanups();
    var session = KM.getSession();
    var path = (location.hash || '').replace(/^#\/?/, '');
    var seg = path.split('/');
    var view = KM.$('#view');
    KM.clear(view);
    renderChrome();
    window.scrollTo(0, 0);

    var screen = null;

    if (!session) {
      screen = path === 'register' ? function () { return KM.views.register(view); } : function () { return KM.views.login(view); };
    } else {
      var role = session.role;
      var key = seg.slice(0, 2).join('/');
      if (seg[0] === 'reports' && role !== 'ADMIN') {
        screen = function () { return KM.views.reports(view); };
      } else if (seg[0] === 'safety' && role !== 'ADMIN') {
        screen = function () { return KM.views.safety(view); };
      } else if (role === 'CLIENT' && key === 'client/home') {
        screen = function () { return KM.views.clientHome(view); };
      } else if (role === 'CLIENT' && key === 'client/book') {
        screen = function () { return KM.views.book(view, seg[2]); };
      } else if (role === 'CLIENT' && key === 'client/jobs') {
        screen = function () { return KM.views.clientJobs(view); };
      } else if (role === 'CLIENT' && key === 'client/job') {
        screen = function () { return KM.views.clientJob(view, seg[2]); };
      } else if (role === 'WORKER' && key === 'worker/jobs') {
        screen = function () { return KM.views.workerHome(view); };
      } else if (role === 'WORKER' && key === 'worker/work') {
        screen = function () { return KM.views.workerWork(view); };
      } else if (role === 'WORKER' && key === 'worker/docs') {
        screen = function () { return KM.views.workerDocs(view); };
      } else if (role === 'ADMIN' && key === 'admin/home') {
        screen = function () { return KM.views.adminHome(view); };
      } else if (role === 'ADMIN' && key === 'admin/workers') {
        screen = function () { return KM.views.adminWorkers(view); };
      } else if (role === 'ADMIN' && key === 'admin/incidents') {
        screen = function () { return KM.views.adminIncidents(view); };
      } else if (role === 'ADMIN' && key === 'admin/tasks') {
        screen = function () { return KM.views.adminTasks(view); };
      } else if (role === 'ADMIN' && key === 'admin/controls') {
        screen = function () { return KM.views.adminControls(view); };
      }
      if (!screen) { KM.go(KM.homeFor(role)); return; }
    }

    view.appendChild(KM.skeleton(3));
    Promise.resolve()
      .then(function () { KM.clear(view); return screen(); })
      .catch(function (e) { KM.showError(view, e); });
  };

  // ====================================================================
  // Start
  // ====================================================================
  window.addEventListener('hashchange', KM.route);
  window.addEventListener('online', function () { KM.$('#offline').hidden = true; });
  window.addEventListener('offline', function () { KM.$('#offline').hidden = false; });
  if (navigator.onLine === false) { KM.$('#offline').hidden = false; }

  // If a worker was online before the page was reloaded, keep sending their position.
  var session = KM.getSession();
  if (session && session.role === 'WORKER' && KM.isOnline()) { KM.startLocationTimer(); }

  KM.route();
})();
