/* KasiMarket front end: the client's screens.
   Home, the 3-step booking wizard, "My jobs", and the live page for following one job. */

(function () {
  'use strict';

  var KM = window.KM;
  var h = KM.h;
  var icon = KM.icon;
  var money = KM.money;

  // ====================================================================
  // Home
  // ====================================================================
  KM.views.clientHome = async function (view) {
    var session = KM.getSession();
    view.appendChild(h('div', { class: 'hero' },
      h('p', { class: 'hero-eyebrow' }, 'Welcome back'),
      h('h1', null, 'Hi, ' + KM.firstName(session.email)),
      h('p', null, 'What do you need help with today?')));
    var body = h('div');
    view.appendChild(body);
    body.appendChild(KM.skeleton(3));

    var results = await Promise.all([KM.api('GET', '/catalog/task-types'), KM.api('GET', '/jobs/mine')]);
    var tasks = results[0];
    var jobs = results[1];
    KM.clear(body);

    // A job that is happening right now gets the top spot, like a delivery app.
    var active = jobs.filter(function (j) { return KM.ACTIVE.indexOf(j.status) !== -1; })[0];
    if (active) {
      var tracking = null;
      try { tracking = await KM.api('GET', '/jobs/' + active.id + '/tracking'); } catch (e) { /* show the card anyway */ }
      body.appendChild(h('a', { class: 'card live', href: '#/client/job/' + active.id },
        h('div', { class: 'live-head' }, h('span', { class: 'live-dot' }), h('strong', null, 'Your job is happening')),
        h('p', { class: 'live-message' }, tracking ? tracking.message : KM.STATUS_LABEL[active.status]),
        h('div', { class: 'row' },
          h('span', { class: 'muted' }, active.taskName + ' - ' + money(active.totalPrice)),
          tracking && tracking.etaMinutes ? h('span', { class: 'eta' }, tracking.etaMinutes + ' min away') : KM.statusBadge(active.status),
          icon('arrowRight', 18))));
    }

    body.appendChild(KM.sectionTitle('Book a task'));
    if (!tasks.length) {
      body.appendChild(KM.emptyState({ icon: 'info', title: 'No tasks are open right now', text: 'Please check again soon.' }));
    } else {
      var grid = h('div', { class: 'tiles' });
      tasks.forEach(function (t) {
        grid.appendChild(h('a', { class: 'tile', href: '#/client/book/' + t.id },
          h('span', { class: 'tile-icon' }, icon(KM.CATEGORY_ICON[t.category] || 'sparkle', 24)),
          h('strong', null, t.name),
          h('span', { class: 'muted small' }, 'From ' + money(t.baseRatePerHour) + ' per hour')));
      });
      body.appendChild(grid);
    }

    if (jobs.length) {
      body.appendChild(KM.sectionTitle('Recent jobs', h('a', { href: '#/client/jobs' }, 'See all')));
      jobs.slice(0, 3).forEach(function (j) { body.appendChild(jobCard(j)); });
    }

    body.appendChild(h('a', { class: 'card tip', href: '#/safety' },
      h('span', { class: 'point-icon' }, icon('shield', 22)),
      h('div', null, h('strong', null, 'How we keep you safe'),
        h('p', { class: 'muted' }, 'Approved workers, a start code at your door, photo proof and an SOS button.')),
      icon('arrowRight', 18)));
  };

  function jobCard(j) {
    return h('a', { class: 'card job-card', href: '#/client/job/' + j.id },
      h('div', { class: 'row' }, h('strong', null, j.taskName), KM.statusBadge(j.status)),
      h('div', { class: 'row' }, h('span', { class: 'muted' }, j.address), h('strong', null, money(j.totalPrice))),
      h('p', { class: 'muted small' }, KM.timeAgo(j.createdAt)));
  }

  // ====================================================================
  // Booking wizard
  // ====================================================================
  KM.views.book = async function (view, presetId) {
    var tasks = await KM.api('GET', '/catalog/task-types');
    view.appendChild(h('h1', null, 'Book a task'));
    if (!tasks.length) {
      view.appendChild(KM.emptyState({ icon: 'info', title: 'No tasks are open right now', text: 'Please check again soon.' }));
      return;
    }

    var preset = tasks.filter(function (t) { return String(t.id) === String(presetId); })[0];
    var state = { step: 1, taskId: (preset || tasks[0]).id, hours: 1, urgent: false, address: '', notes: '', pos: null, quote: null };
    state.hours = (preset || tasks[0]).minHours;

    var root = h('div');
    view.appendChild(root);
    var priceBox = null;
    var quoteCounter = 0;

    function currentTask() {
      return tasks.filter(function (t) { return t.id === state.taskId; })[0];
    }

    function drawPrice() {
      if (!priceBox) { return; }
      KM.clear(priceBox);
      var q = state.quote;
      if (!q) {
        priceBox.appendChild(h('p', { class: 'muted' }, 'Working out the price...'));
        return;
      }
      priceBox.appendChild(h('div', { class: 'row' },
        h('div', null,
          h('div', { class: 'muted' }, q.hours + ' hour(s)' + (q.urgent ? ' - urgent' : '')),
          h('div', { class: 'big' }, money(q.totalPrice))),
        h('div', { class: 'muted right small' }, 'The price is fixed', h('br'), 'once you book.')));
    }

    // Asks the server for the price whenever the task, hours or urgency change.
    async function refreshQuote() {
      var mine = ++quoteCounter;
      state.quote = null;
      drawPrice();
      try {
        var q = await KM.api('POST', '/pricing/quote', { taskTypeId: state.taskId, hours: state.hours, urgent: state.urgent });
        if (mine !== quoteCounter) { return; }   // a newer question was asked meanwhile
        state.quote = q;
      } catch (e) {
        if (mine !== quoteCounter) { return; }
        KM.clear(priceBox);
        priceBox.appendChild(h('p', null, e.message));
        return;
      }
      drawPrice();
    }

    function stepper() {
      var labels = ['Task', 'Place', 'Review'];
      var ol = h('ol', { class: 'wizard' });
      labels.forEach(function (l, i) {
        var n = i + 1;
        ol.appendChild(h('li', { class: n < state.step ? 'done' : (n === state.step ? 'current' : '') },
          h('span', { class: 'wizard-dot' }, n < state.step ? icon('check', 14) : String(n)), l));
      });
      return ol;
    }

    function draw() {
      KM.clear(root);
      root.appendChild(stepper());
      if (state.step === 1) { drawStep1(); } else if (state.step === 2) { drawStep2(); } else { drawStep3(); }
    }

    function drawStep1() {
      var t = currentTask();
      root.appendChild(h('h2', null, 'What do you need?'));
      var list = h('div', { class: 'choices' });
      tasks.forEach(function (x) {
        var btn = h('button', { type: 'button', class: 'choice' + (x.id === state.taskId ? ' selected' : ''), 'aria-pressed': x.id === state.taskId ? 'true' : 'false' },
          h('span', { class: 'tile-icon' }, icon(KM.CATEGORY_ICON[x.category] || 'sparkle', 22)),
          h('span', { class: 'choice-text' }, h('strong', null, x.name), h('span', { class: 'muted small' }, money(x.baseRatePerHour) + ' per hour, minimum ' + x.minHours + 'h')),
          x.id === state.taskId ? icon('check', 20) : null);
        btn.addEventListener('click', function () {
          state.taskId = x.id;
          state.hours = x.minHours;   // start from the shortest booking for the chosen task
          draw();
          refreshQuote();
        });
        list.appendChild(btn);
      });
      root.appendChild(list);

      root.appendChild(h('h2', null, 'How long?'));
      var minus = h('button', { type: 'button', class: 'round', 'aria-label': 'One hour less' }, '-');
      var plus = h('button', { type: 'button', class: 'round', 'aria-label': 'One hour more' }, '+');
      var count = h('span', { class: 'count', 'aria-live': 'polite' }, state.hours + ' hour' + (state.hours > 1 ? 's' : ''));
      minus.addEventListener('click', function () {
        if (state.hours > t.minHours) { state.hours--; draw(); refreshQuote(); }
        else { KM.toast('The minimum for this task is ' + t.minHours + ' hour(s).'); }
      });
      plus.addEventListener('click', function () {
        if (state.hours < 12) { state.hours++; draw(); refreshQuote(); }
      });
      root.appendChild(h('div', { class: 'card stepper' }, minus, count, plus));

      root.appendChild(h('div', { class: 'card' },
        KM.switchControl(state.urgent, 'It is urgent (costs 30% more)', function (on) { state.urgent = on; refreshQuote(); })));

      priceBox = h('div', { class: 'card price' });
      root.appendChild(priceBox);
      drawPrice();
      if (state.quote === null) { /* the quote arrives shortly */ }

      root.appendChild(h('button', { type: 'button', class: 'btn block', onclick: function () { state.step = 2; draw(); } }, 'Next: where?', icon('arrowRight', 18)));
    }

    function drawStep2() {
      root.appendChild(h('h2', null, 'Where is the job?'));
      var address = h('input', { type: 'text', maxlength: '200', autocomplete: 'street-address', placeholder: 'For example: 12 Main Road, Randburg' });
      address.value = state.address;
      address.addEventListener('input', function () { state.address = address.value; });
      var notes = h('textarea', { maxlength: '300', placeholder: 'Anything the worker should know? (optional)' });
      notes.value = state.notes;
      notes.addEventListener('input', function () { state.notes = notes.value; });

      var posChip = h('p', { class: 'chip-line' });
      function drawPos() {
        KM.clear(posChip);
        posChip.appendChild(state.pos
          ? KM.badge('Location saved', 'good')
          : KM.badge('Location needed', 'warn'));
        posChip.appendChild(h('span', { class: 'muted small' }, ' We use it to find workers near you. Workers only see an approximate distance.'));
      }
      drawPos();

      root.appendChild(h('div', { class: 'card' },
        KM.field('Address', address),
        KM.field('Notes', notes),
        posChip,
        h('div', { class: 'actions' },
          KM.button('Use my location', 'secondary small', async function () {
            state.pos = await KM.getPosition();
            drawPos();
          }, 'pin'),
          KM.button('Use demo location (Randburg)', 'secondary small', async function () {
            state.pos = KM.DEMO_CLIENT_POS;
            drawPos();
          }))));

      root.appendChild(h('div', { class: 'actions between' },
        h('button', { type: 'button', class: 'btn secondary', onclick: function () { state.step = 1; draw(); } }, icon('arrowLeft', 18), 'Back'),
        h('button', {
          type: 'button', class: 'btn', onclick: function () {
            if (!state.address.trim()) { KM.toast('Please type the address.', true); return; }
            if (!state.pos) { KM.toast('Please share the location of the job first.', true); return; }
            state.step = 3;
            draw();
          }
        }, 'Next: review', icon('arrowRight', 18))));
    }

    function drawStep3() {
      var t = currentTask();
      root.appendChild(h('h2', null, 'Check and book'));
      root.appendChild(h('div', { class: 'card' },
        h('dl', { class: 'summary' },
          h('dt', null, 'Task'), h('dd', null, t.name),
          h('dt', null, 'Time'), h('dd', null, state.hours + ' hour(s)' + (state.urgent ? ', urgent' : '')),
          h('dt', null, 'Address'), h('dd', null, state.address.trim()),
          state.notes.trim() ? h('dt', null, 'Notes') : null, state.notes.trim() ? h('dd', null, state.notes.trim()) : null)));
      priceBox = h('div', { class: 'card price' });
      root.appendChild(priceBox);
      drawPrice();

      var steps = h('ul', { class: 'tips' });
      ['A worker near you accepts. You see their name, rating and arrival time.',
       'You get a 4-digit start code. Give it to the worker only when they arrive.',
       'When they finish you see a photo. You pay only after you confirm.'].forEach(function (x) { steps.appendChild(h('li', null, x)); });
      root.appendChild(h('div', { class: 'card' }, h('h3', null, 'What happens next'), steps));

      var book = h('button', { type: 'button', class: 'btn block' }, icon('check', 18), 'Book now');
      book.addEventListener('click', KM.wrap(book, async function () {
        var job = await KM.api('POST', '/jobs', {
          taskTypeId: t.id,
          hours: state.hours,
          urgent: state.urgent,
          address: state.address.trim(),
          latitude: state.pos.lat,
          longitude: state.pos.lng,
          notes: state.notes.trim() ? state.notes.trim() : null
        });
        KM.toast('Job booked. We are finding a worker near you.');
        KM.go('#/client/job/' + job.id);
      }));
      root.appendChild(book);
      root.appendChild(h('button', { type: 'button', class: 'btn secondary block', onclick: function () { state.step = 2; draw(); } }, 'Back'));
    }

    draw();
    refreshQuote();
  };

  // ====================================================================
  // My jobs
  // ====================================================================
  KM.views.clientJobs = async function (view) {
    view.appendChild(h('h1', null, 'My jobs'));
    var filter = 'active';
    var seg = h('div', { class: 'segmented', role: 'tablist' });
    var list = h('div');
    view.appendChild(seg);
    view.appendChild(list);
    var jobs = [];

    function drawSeg() {
      KM.clear(seg);
      [['active', 'Active'], ['past', 'Past']].forEach(function (x) {
        var b = h('button', { type: 'button', role: 'tab', class: filter === x[0] ? 'on' : '', 'aria-selected': filter === x[0] ? 'true' : 'false' }, x[1]);
        b.addEventListener('click', function () { filter = x[0]; drawSeg(); drawList(); });
        seg.appendChild(b);
      });
    }

    function drawList() {
      KM.clear(list);
      var shown = jobs.filter(function (j) { return (KM.ACTIVE.indexOf(j.status) !== -1) === (filter === 'active'); });
      if (!shown.length) {
        list.appendChild(KM.emptyState({
          icon: 'list',
          title: filter === 'active' ? 'No active jobs' : 'No past jobs yet',
          text: filter === 'active' ? 'Book a task and we will find a worker near you.' : 'Finished and cancelled jobs show up here.',
          action: filter === 'active' ? h('a', { class: 'btn', href: '#/client/book' }, 'Book a task') : null
        }));
        return;
      }
      shown.forEach(function (j) { list.appendChild(jobCard(j)); });
    }

    async function load() {
      jobs = await KM.api('GET', '/jobs/mine');
      drawSeg();
      drawList();
    }

    await load();
    KM.poll(function () { load().catch(function () { /* keep what we show */ }); }, 15000);
  };

  // ====================================================================
  // One job, live
  // ====================================================================
  function stepsBar(status) {
    var index = KM.STEPS.indexOf(status);
    var ol = h('ol', { class: 'steps' });
    KM.STEPS.forEach(function (s, i) {
      ol.appendChild(h('li', { class: i < index ? 'done' : (i === index ? 'current' : '') }, KM.STEP_LABEL[i]));
    });
    return ol;
  }

  // A row of five stars the person can tap.
  function starPicker(initial) {
    var value = initial || 5;
    var wrap = h('div', { class: 'star-picker', role: 'radiogroup' });
    function draw() {
      KM.clear(wrap);
      for (var i = 1; i <= 5; i++) {
        (function (n) {
          var b = h('button', { type: 'button', class: 'star-btn' + (n <= value ? ' on' : ''), role: 'radio', 'aria-checked': n === value ? 'true' : 'false', 'aria-label': n + ' out of 5' });
          var s = icon('star', 28);
          if (n <= value) { s.setAttribute('class', 'icon filled'); }
          b.appendChild(s);
          b.addEventListener('click', function () { value = n; draw(); });
          wrap.appendChild(b);
        })(i);
      }
    }
    draw();
    return { el: wrap, value: function () { return value; } };
  }

  function ratingForm(jobId) {
    var punctuality = starPicker(5);
    var quality = starPicker(5);
    var communication = starPicker(5);
    var comment = h('textarea', { maxlength: '500', placeholder: 'Anything to add? (optional)' });
    var submit = h('button', { type: 'submit', class: 'btn block' }, 'Send my rating');
    var form = h('form', null,
      h('div', { class: 'rate-row' }, h('span', null, 'On time'), punctuality.el),
      h('div', { class: 'rate-row' }, h('span', null, 'Quality of work'), quality.el),
      h('div', { class: 'rate-row' }, h('span', null, 'Communication'), communication.el),
      KM.field('Comment', comment),
      submit);
    var card = h('div', { class: 'card' }, h('h3', null, 'Rate your worker'), h('p', { class: 'muted' }, 'Your rating helps keep KasiMarket safe for everyone.'), form);

    form.addEventListener('submit', KM.wrap(submit, async function () {
      try {
        await KM.api('POST', '/jobs/' + jobId + '/rating', {
          punctuality: punctuality.value(), quality: quality.value(), communication: communication.value(),
          comment: comment.value.trim() ? comment.value.trim() : null
        });
        KM.toast('Thank you for rating.');
      } catch (e) {
        if (e.status !== 409) { throw e; }
        KM.toast('You have already rated this job.');
      }
      KM.clear(card);
      card.appendChild(h('p', null, 'Thank you. Your rating helps keep KasiMarket safe.'));
    }));
    return card;
  }

  KM.views.clientJob = async function (view, idText) {
    var id = Number(idText);
    view.appendChild(h('a', { class: 'back', href: '#/client/jobs' }, icon('arrowLeft', 16), 'All my jobs'));

    var statusBox = h('div', { class: 'card status-card' });
    var codeBox = h('div');
    var workerBox = h('div');
    var actions = h('div');
    var head = h('div', { class: 'card' });
    var safety = h('div');
    [statusBox, codeBox, workerBox, actions, head, safety].forEach(function (x) { view.appendChild(x); });

    var lastStatus = null;
    var photoUrl = null;
    KM.onLeave(function () { if (photoUrl) { URL.revokeObjectURL(photoUrl); } });

    async function refresh() {
      var jobs = await KM.api('GET', '/jobs/mine');
      var job = jobs.filter(function (j) { return j.id === id; })[0];
      if (!job) {
        KM.clear(statusBox);
        statusBox.appendChild(h('p', null, 'We could not find this job.'));
        return true;
      }
      var t = await KM.api('GET', '/jobs/' + id + '/tracking');
      var finished = KM.TERMINAL.indexOf(job.status) !== -1;

      // ----- the big status card
      KM.clear(statusBox);
      statusBox.className = 'card status-card s-' + job.status.toLowerCase();
      statusBox.appendChild(h('div', { class: 'status-top' },
        h('span', { class: 'status-icon' }, icon(statusIcon(job.status), 24)),
        KM.statusBadge(job.status)));
      statusBox.appendChild(h('div', { class: 'statusmsg' }, t.message));
      if (t.etaMinutes) { statusBox.appendChild(h('span', { class: 'eta' }, icon('clock', 16), 'About ' + t.etaMinutes + ' min away')); }
      if (job.status !== 'CANCELLED' && job.status !== 'EXPIRED') { statusBox.appendChild(stepsBar(job.status)); }

      // ----- the start code
      KM.clear(codeBox);
      if (t.startCode) {
        var digits = h('div', { class: 'code-digits', 'aria-label': 'Start code ' + t.startCode.split('').join(' ') });
        t.startCode.split('').forEach(function (d) { digits.appendChild(h('span', null, d)); });
        codeBox.appendChild(h('div', { class: 'card code-card' },
          h('div', { class: 'row' }, h('strong', null, 'Your start code'), icon('key', 20)),
          digits,
          h('p', { class: 'muted' }, 'Give this code to the worker only when they arrive and you have checked their name. They need it to start the work.')));
      }

      // ----- the worker
      KM.clear(workerBox);
      if (t.workerName) {
        workerBox.appendChild(h('div', { class: 'card worker-card' },
          KM.avatar(t.workerName, 'lg'),
          h('div', { class: 'worker-info' },
            h('strong', null, t.workerName),
            h('div', { class: 'chips' }, t.workerTier ? KM.tierBadge(t.workerTier) : null, KM.badge('Approved by KasiMarket', 'good')),
            h('div', { class: 'rating-line' }, KM.stars(t.workerRating),
              h('span', { class: 'muted small' }, t.workerRating ? t.workerRating + ' - ' + t.workerJobsCompleted + ' job(s)' : 'New worker')))));
      }

      // ----- the job details
      KM.clear(head);
      head.appendChild(h('div', { class: 'row' }, h('h3', null, job.taskName), h('strong', null, money(job.totalPrice))));
      head.appendChild(h('p', { class: 'muted' }, icon('pin', 14), ' ' + job.address));
      head.appendChild(h('p', { class: 'muted small' }, job.hours + ' hour(s)' + (job.urgent ? ' - urgent' : '') + ' - booked ' + KM.fmtDate(job.createdAt)));

      if (job.status !== lastStatus) {
        lastStatus = job.status;
        buildActions(job);
        KM.clear(safety);
        safety.appendChild(KM.safetyPanel(id, !!t.workerName && !finished));
      }
      return finished;
    }

    function statusIcon(status) {
      return { REQUESTED: 'search', ACCEPTED: 'check', EN_ROUTE: 'pin', ARRIVED: 'key', IN_PROGRESS: 'zap', COMPLETED: 'camera', PAID: 'coin', CANCELLED: 'x', EXPIRED: 'clock' }[status] || 'info';
    }

    function buildActions(job) {
      KM.clear(actions);
      var s = job.status;

      if (s === 'REQUESTED' || s === 'ACCEPTED' || s === 'EN_ROUTE' || s === 'ARRIVED') {
        actions.appendChild(h('div', { class: 'actions' },
          KM.button('Cancel job', 'secondary', async function () {
            var sure = await KM.confirmDialog({ title: 'Cancel this job?', message: 'You can cancel for free before the work starts.', confirmText: 'Cancel job', cancelText: 'Keep it', danger: true });
            if (!sure) { return; }
            await KM.api('POST', '/jobs/' + id + '/cancel');
            KM.toast('Job cancelled.');
            await refresh();
          })));
      }

      if (s === 'EXPIRED') {
        actions.appendChild(h('div', { class: 'card notice' },
          h('p', null, 'No worker was free in time. Please try again.'),
          h('a', { class: 'btn', href: '#/client/book' }, 'Book again')));
      }

      if (s === 'COMPLETED') {
        var photoBox = h('div', null, KM.skeleton(1));
        actions.appendChild(h('div', { class: 'card notice' },
          h('h3', null, 'The work is finished'),
          h('p', null, 'Please check the photo. Paying releases the money to your worker.'),
          photoBox,
          KM.button('Confirm and pay ' + money(job.totalPrice), 'block', async function () {
            await KM.api('POST', '/jobs/' + id + '/confirm');
            KM.toast('Thank you. The job is paid.');
            await refresh();
          }, 'check')));
        KM.api('GET', '/jobs/' + id + '/photo', null, { blob: true }).then(function (blob) {
          photoUrl = URL.createObjectURL(blob);
          KM.clear(photoBox);
          photoBox.appendChild(h('img', { class: 'proof', src: photoUrl, alt: 'Photo of the finished work' }));
        }).catch(function (e) {
          KM.clear(photoBox);
          photoBox.appendChild(h('p', null, e.message));
        });
      }

      if (s === 'PAID') { actions.appendChild(ratingForm(id)); }
    }

    var done = await refresh();
    if (!done) {
      var timer = KM.poll(function () {
        refresh().then(function (isDone) { if (isDone) { clearInterval(timer); } }).catch(function () { /* try again soon */ });
      }, 5000);
    }
  };
})();
