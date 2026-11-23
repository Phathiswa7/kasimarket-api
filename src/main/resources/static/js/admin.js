/* KasiMarket front end: the staff (admin) screens.
   Dashboard, Workers (approve, documents, tiers), Incidents, Tasks (switch tasks on and off),
   and the Controls register (what we have built against each risk). */

(function () {
  'use strict';

  var KM = window.KM;
  var h = KM.h;
  var icon = KM.icon;
  var money = KM.money;

  // ====================================================================
  // Dashboard
  // ====================================================================
  KM.views.adminHome = async function (view) {
    var session = KM.getSession();
    view.appendChild(h('div', { class: 'hero staff' },
      h('p', { class: 'hero-eyebrow' }, 'Staff dashboard'),
      h('h1', null, 'KasiMarket control room'),
      h('p', null, 'Signed in as ' + session.email)));
    var body = h('div');
    view.appendChild(body);
    body.appendChild(KM.skeleton(3));

    var results = await Promise.all([
      KM.api('GET', '/admin/stats'),
      KM.api('GET', '/admin/workers?status=SUBMITTED'),
      KM.api('GET', '/admin/incidents?status=OPEN')
    ]);
    var s = results[0];
    var waiting = results[1];
    var openIncidents = results[2];
    KM.clear(body);

    // An SOS is the most important thing on the screen.
    if (s.sosOpen > 0) {
      body.appendChild(h('a', { class: 'card sos alert-banner', href: '#/admin/incidents' },
        icon('alert', 24),
        h('div', null, h('strong', null, s.sosOpen + ' SOS alert' + (s.sosOpen > 1 ? 's' : '') + ' need attention'),
          h('p', { class: 'muted' }, 'Open the incidents list. The most serious are at the top.')),
        icon('arrowRight', 18)));
    }

    var stats = h('div', { class: 'stats' });
    stats.appendChild(KM.statCard('Workers waiting', s.workersSubmitted, { icon: 'users', href: '#/admin/workers', kind: s.workersSubmitted > 0 ? 'warn' : '' }));
    stats.appendChild(KM.statCard('Open incidents', s.incidentsOpen, { icon: 'alert', href: '#/admin/incidents', kind: s.incidentsOpen > 0 ? 'bad' : '' }));
    stats.appendChild(KM.statCard('Jobs live', s.jobsLive, { icon: 'zap' }));
    stats.appendChild(KM.statCard('Paid so far', money(s.paidTotal), { icon: 'coin', kind: 'good' }));
    stats.appendChild(KM.statCard('KasiMarket fees', money(s.platformFeeTotal), { icon: 'chart' }));
    stats.appendChild(KM.statCard('Approved workers', s.workersApproved, { icon: 'check' }));
    body.appendChild(stats);

    // ----- jobs by stage
    body.appendChild(KM.sectionTitle('Jobs by stage'));
    var rows = [['Waiting for a worker', s.jobsOpen], ['In progress or on the way', s.jobsLive], ['Finished, waiting to be paid', s.jobsCompleted],
                ['Paid', s.jobsPaid], ['Cancelled', s.jobsCancelled], ['Expired', s.jobsExpired]];
    var max = Math.max.apply(null, rows.map(function (r) { return r[1]; }).concat([1]));
    var funnel = h('div', { class: 'card funnel' });
    rows.forEach(function (r) {
      funnel.appendChild(h('div', { class: 'funnel-row' },
        h('span', { class: 'funnel-label' }, r[0]),
        KM.progress(r[1], max, r[0]),
        h('strong', null, String(r[1]))));
    });
    body.appendChild(funnel);

    // ----- what needs attention
    body.appendChild(KM.sectionTitle('Needs attention', h('a', { href: '#/admin/workers' }, 'Workers')));
    if (!waiting.length && !openIncidents.length) {
      body.appendChild(KM.emptyState({ icon: 'check', title: 'All clear', text: 'No workers waiting and no open incidents.' }));
    }
    waiting.slice(0, 5).forEach(function (w) {
      body.appendChild(h('a', { class: 'card row-link', href: '#/admin/workers' },
        KM.avatar(w.fullName), h('div', { class: 'grow' }, h('strong', null, w.fullName), h('p', { class: 'muted small' }, w.email)),
        KM.statusBadge(w.vettingStatus)));
    });
    openIncidents.slice(0, 5).forEach(function (i) {
      body.appendChild(h('a', { class: 'card row-link' + (i.sos ? ' sos' : ''), href: '#/admin/incidents' },
        h('span', { class: 'point-icon' }, icon('alert', 20)),
        h('div', { class: 'grow' }, h('strong', null, (i.sos ? 'SOS - ' : '') + 'Job #' + i.jobId), h('p', { class: 'muted small' }, i.description)),
        KM.severityBadge(i.severity)));
    });

    body.appendChild(h('a', { class: 'card tip', href: '#/admin/controls' },
      h('span', { class: 'point-icon' }, icon('flag', 22)),
      h('div', null, h('strong', null, 'Risk and controls register'),
        h('p', { class: 'muted' }, 'What we have built against each risk, and what is still to do.')),
      icon('arrowRight', 18)));
  };

  // ====================================================================
  // Workers
  // ====================================================================
  KM.views.adminWorkers = async function (view) {
    view.appendChild(h('h1', null, 'Workers'));
    var filter = h('select', null,
      KM.option('', 'All workers'), KM.option('SUBMITTED', 'Waiting for approval', true),
      KM.option('APPROVED', 'Approved'), KM.option('REJECTED', 'Rejected'), KM.option('SUSPENDED', 'Suspended'));
    var search = h('input', { type: 'search', placeholder: 'Search by name or email' });
    var list = h('div');
    view.appendChild(h('div', { class: 'toolbar' }, KM.field('Show', filter), KM.field('Search', search)));
    view.appendChild(list);
    var workers = [];

    function draw() {
      KM.clear(list);
      var q = search.value.trim().toLowerCase();
      var shown = workers.filter(function (w) { return !q || (w.fullName + ' ' + w.email).toLowerCase().indexOf(q) !== -1; });
      if (!shown.length) {
        list.appendChild(KM.emptyState({ icon: 'users', title: 'No workers in this list', text: 'Try another filter.' }));
        return;
      }
      shown.forEach(function (w) { list.appendChild(workerCard(w, load)); });
    }

    async function load() {
      var query = filter.value ? '?status=' + encodeURIComponent(filter.value) : '';
      workers = await KM.api('GET', '/admin/workers' + query);
      draw();
    }

    filter.addEventListener('change', function () { load().catch(function (e) { KM.toast(e.message, true); }); });
    search.addEventListener('input', draw);
    await load();
  };

  function workerCard(w, reload) {
    function decide(status, question) {
      return async function () {
        if (question) {
          var sure = await KM.confirmDialog({ title: question, confirmText: 'Yes, continue', danger: status !== 'APPROVED' });
          if (!sure) { return; }
        }
        await KM.api('PUT', '/admin/workers/' + w.id + '/decision', { status: status });
        KM.toast('Worker updated.');
        await reload();
      };
    }

    var card = h('div', { class: 'card worker-admin' },
      h('div', { class: 'row-head' },
        KM.avatar(w.fullName),
        h('div', { class: 'grow' }, h('strong', null, w.fullName), h('p', { class: 'muted small' }, w.email + (w.area ? ' - ' + w.area : ''))),
        KM.statusBadge(w.vettingStatus)),
      h('div', { class: 'chips' }, KM.tierBadge(w.tier)));

    var actions = h('div', { class: 'actions' });
    if (w.vettingStatus !== 'APPROVED') { actions.appendChild(KM.button('Approve', 'small', decide('APPROVED'), 'check')); }
    if (w.vettingStatus !== 'REJECTED') { actions.appendChild(KM.button('Reject', 'secondary small', decide('REJECTED', 'Reject ' + w.fullName + '?'))); }
    if (w.vettingStatus !== 'SUSPENDED') { actions.appendChild(KM.button('Suspend', 'danger small', decide('SUSPENDED', 'Suspend ' + w.fullName + '?'))); }
    card.appendChild(actions);

    // The worker's documents are loaded only when staff open this box.
    var docsBox = h('div');
    var docsDetails = h('details', null, h('summary', null, icon('file', 16), 'Documents'), docsBox);
    var loaded = false;
    docsDetails.addEventListener('toggle', function () {
      if (docsDetails.open && !loaded) { loaded = true; loadWorkerDocs(w.id, docsBox); }
    });
    card.appendChild(docsDetails);

    if (w.vettingStatus === 'APPROVED') { card.appendChild(tierBox(w, reload)); }
    return card;
  }

  async function loadWorkerDocs(profileId, box) {
    KM.clear(box);
    box.appendChild(KM.skeleton(1));
    try {
      var docs = await KM.api('GET', '/admin/vetting/workers/' + profileId + '/documents');
      KM.clear(box);
      if (!docs.length) { box.appendChild(h('p', { class: 'muted' }, 'No documents uploaded yet.')); return; }
      docs.forEach(function (d) { box.appendChild(adminDocCard(d, function () { loadWorkerDocs(profileId, box); })); });
    } catch (e) {
      KM.clear(box);
      box.appendChild(h('p', null, e.message));
    }
  }

  function extensionFor(contentType) {
    if (contentType === 'application/pdf') { return '.pdf'; }
    if (contentType === 'image/png') { return '.png'; }
    return '.jpg';
  }

  function adminDocCard(d, reload) {
    var card = h('div', { class: 'card inner' },
      h('div', { class: 'row' }, h('strong', null, KM.DOC_LABEL[d.type] || d.type), KM.statusBadge(d.reviewStatus)),
      d.validUntil ? h('p', { class: 'muted small' }, 'Valid until ' + d.validUntil) : null,
      d.reviewNote ? h('p', { class: 'small' }, 'Note: ' + d.reviewNote) : null);

    var actions = h('div', { class: 'actions' });
    actions.appendChild(KM.button('Download', 'secondary small', async function () {
      var blob = await KM.api('GET', '/admin/vetting/documents/' + d.id + '/file', null, { blob: true });
      var url = URL.createObjectURL(blob);
      var link = h('a', { href: url, download: 'document-' + d.id + extensionFor(d.contentType) });
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
    }, 'download'));

    if (d.reviewStatus === 'PENDING') {
      actions.appendChild(KM.button('Accept', 'small', async function () {
        await KM.api('PUT', '/admin/vetting/documents/' + d.id + '/review', { status: 'ACCEPTED' });
        KM.toast('Document accepted.');
        reload();
      }, 'check'));
      actions.appendChild(KM.button('Reject', 'danger small', async function () {
        var note = await KM.askText({ title: 'Reject this document', message: 'The worker will read your reason, so be clear and kind.', label: 'Reason', multiline: true, confirmText: 'Reject document' });
        if (!note) { return; }
        await KM.api('PUT', '/admin/vetting/documents/' + d.id + '/review', { status: 'REJECTED', note: note });
        KM.toast('Document rejected.');
        reload();
      }));
    }
    card.appendChild(actions);
    return card;
  }

  function tierBox(w, reload) {
    var tier = h('select', null, KM.option('3', 'Tier 3 (higher-trust tasks)'), KM.option('2', 'Tier 2 (back to automatic)'));
    var refs = h('input', { type: 'checkbox', id: 'refs' + w.id });
    var checks = h('input', { type: 'checkbox', id: 'checks' + w.id });
    var submit = h('button', { type: 'submit', class: 'btn small' }, 'Change tier');
    var form = h('form', null,
      KM.field('New tier', tier),
      h('div', { class: 'check' }, refs, h('label', { for: 'refs' + w.id }, 'I phoned and verified the references myself')),
      h('div', { class: 'check' }, checks, h('label', { for: 'checks' + w.id }, 'The enhanced background check is complete')),
      submit);

    form.addEventListener('submit', KM.wrap(submit, async function () {
      await KM.api('PUT', '/admin/workers/' + w.id + '/tier', {
        tier: Number(tier.value), referencesVerified: refs.checked, enhancedCheckDone: checks.checked
      });
      KM.toast('Tier changed.');
      await reload();
    }));
    return h('details', null, h('summary', null, icon('star', 16), 'Change tier'),
      h('p', { class: 'muted small' }, 'Tier 3 also needs a strong track record. The system checks this for you.'), form);
  }

  // ====================================================================
  // Incidents
  // ====================================================================
  KM.views.adminIncidents = async function (view) {
    view.appendChild(h('h1', null, 'Incidents'));
    var filter = h('select', null, KM.option('OPEN', 'Open', true), KM.option('INVESTIGATING', 'Being investigated'), KM.option('RESOLVED', 'Resolved'), KM.option('', 'All'));
    var list = h('div');
    view.appendChild(KM.field('Show', filter));
    view.appendChild(list);

    async function load() {
      var query = filter.value ? '?status=' + encodeURIComponent(filter.value) : '';
      var incidents = await KM.api('GET', '/admin/incidents' + query);
      KM.clear(list);
      if (!incidents.length) {
        list.appendChild(KM.emptyState({ icon: 'check', title: 'Nothing here', text: 'Good news.' }));
        return;
      }
      incidents.forEach(function (i) { list.appendChild(incidentCard(i, load)); });
    }

    filter.addEventListener('change', function () { load().catch(function (e) { KM.toast(e.message, true); }); });
    await load();
    KM.poll(function () {
      if (document.querySelector('.modal-backdrop')) { return; }
      load().catch(function () { /* keep what we show */ });
    }, 10000);
  };

  function incidentCard(i, reload) {
    var card = h('div', { class: 'card' + (i.sos ? ' sos' : (i.severity === 'CRITICAL' ? ' alert' : '')) },
      h('div', { class: 'row' },
        h('strong', null, (i.sos ? 'SOS - ' : '') + 'Job #' + i.jobId),
        h('span', { class: 'chips' }, KM.severityBadge(i.severity), KM.statusBadge(i.status))),
      h('p', null, i.description),
      h('p', { class: 'muted small' }, 'Reported by ' + i.reportedBy + ' (' + i.reporterRole + ')' + (i.otherParty ? ' about ' + i.otherParty : '')),
      h('p', { class: 'muted small' }, KM.fmtDate(i.createdAt)));

    if (i.latitude !== null && i.latitude !== undefined && i.longitude !== null && i.longitude !== undefined) {
      var lat = Number(i.latitude);
      var lng = Number(i.longitude);
      card.appendChild(h('p', null, h('a', { target: '_blank', rel: 'noopener noreferrer',
        href: 'https://www.openstreetmap.org/?mlat=' + lat + '&mlon=' + lng + '#map=16/' + lat + '/' + lng }, icon('pin', 14), ' Open the position on a map')));
    }
    if (i.resolution) {
      card.appendChild(h('div', { class: 'answer' }, h('strong', null, 'Resolution'), h('p', null, i.resolution)));
    }

    var actions = h('div', { class: 'actions' });
    if (i.status === 'OPEN') {
      actions.appendChild(KM.button('Start investigating', 'secondary small', async function () {
        await KM.api('PUT', '/admin/incidents/' + i.id, { status: 'INVESTIGATING' });
        await reload();
      }));
    }
    if (i.status !== 'RESOLVED') {
      actions.appendChild(KM.button('Resolve', 'small', async function () {
        var text = await KM.askText({
          title: 'Resolve this incident', message: 'This is kept on record, and the person who reported it can read it.',
          label: 'How was it resolved?', multiline: true, maxlength: 1000, confirmText: 'Resolve'
        });
        if (!text) { return; }
        await KM.api('PUT', '/admin/incidents/' + i.id, { status: 'RESOLVED', resolution: text });
        KM.toast('Incident resolved.');
        await reload();
      }, 'check'));
    }
    card.appendChild(actions);
    return card;
  }

  // ====================================================================
  // Tasks (the pilot scope)
  // ====================================================================
  KM.views.adminTasks = async function (view) {
    view.appendChild(h('h1', null, 'Tasks'));
    view.appendChild(h('div', { class: 'card notice' },
      h('strong', null, 'Pilot scope'),
      h('p', { class: 'muted' }, 'Keep higher-risk tasks (childcare, house watching) switched off during the pilot. Switch a task on only when you have the data, the insurance and the legal advice. Changing a price never changes jobs that are already booked.')));
    var list = h('div');
    view.appendChild(list);

    async function load() {
      var tasks = await KM.api('GET', '/admin/task-types');
      KM.clear(list);
      tasks.forEach(function (t) { list.appendChild(taskCard(t, load)); });
    }
    await load();
  };

  function taskCard(t, reload) {
    var rate = h('input', { type: 'number', min: '10', max: '2000', value: String(t.baseRatePerHour), 'aria-label': 'Rand per hour' });
    var card = h('div', { class: 'card task-admin' + (t.active ? '' : ' off') },
      h('div', { class: 'row-head' },
        h('span', { class: 'tile-icon' }, icon(KM.CATEGORY_ICON[t.category] || 'sparkle', 22)),
        h('div', { class: 'grow' }, h('strong', null, t.name),
          h('p', { class: 'muted small' }, 'Minimum ' + t.minHours + ' hour(s)')),
        KM.tierBadge(t.minTier)),
      h('div', { class: 'row' },
        KM.switchControl(t.active, t.active ? 'Open to clients' : 'Switched off', async function (on, input) {
          input.disabled = true;
          try {
            await KM.api('PUT', '/admin/task-types/' + t.id, { active: on });
            KM.toast(on ? t.name + ' is open to clients.' : t.name + ' is switched off.');
          } catch (e) {
            input.checked = !on;
            KM.toast(e.message, true);
          }
          await reload();
        })),
      h('div', { class: 'rate-edit' },
        h('label', null, 'Rand per hour'), rate,
        KM.button('Save price', 'secondary small', async function () {
          await KM.api('PUT', '/admin/task-types/' + t.id, { baseRatePerHour: Number(rate.value) });
          KM.toast('Price saved.');
          await reload();
        })));
    return card;
  }

  // ====================================================================
  // Controls register
  // ====================================================================
  var STATUS_TEXT = { done: 'Done', partial: 'Partly built', planned: 'Planned' };
  var STATUS_KIND = { done: 'good', partial: 'warn', planned: 'bad' };

  KM.views.adminControls = function (view) {
    var data = KM.CONTROLS;
    var all = data.items;
    var count = function (list, s) { return list.filter(function (x) { return x.status === s; }).length; };

    view.appendChild(h('h1', null, 'Risks and controls'));
    view.appendChild(h('p', { class: 'muted' }, 'Built from the risk and counter register and the investor action plan. The statuses are honest: "Planned" means it is not built yet.'));

    // ----- launch readiness
    var blockers = data.blockers;
    var weight = { done: 1, partial: 0.5, planned: 0 };
    var score = blockers.reduce(function (t, b) { return t + weight[b.status]; }, 0);
    var readiness = h('div', { class: 'card' },
      h('div', { class: 'row' }, h('strong', null, 'Launch readiness'), h('span', { class: 'big' }, Math.round(score / blockers.length * 100) + '%')),
      KM.progress(score, blockers.length, 'Launch readiness'),
      h('p', { class: 'muted small' }, 'Each launch blocker counts fully when done and half when partly built. Real people should not use the app until all ten are done.'));
    var blockerList = h('ul', { class: 'checklist' });
    blockers.forEach(function (b) {
      blockerList.appendChild(h('li', { class: 'check-row' },
        h('span', { class: 'check-icon ' + (b.status === 'done' ? 'ok' : '') }, icon(b.status === 'done' ? 'check' : 'clock', 16)),
        h('div', { class: 'grow' }, h('span', null, b.text), h('p', { class: 'muted small' }, b.note)),
        KM.badge(STATUS_TEXT[b.status], STATUS_KIND[b.status])));
    });
    readiness.appendChild(blockerList);
    view.appendChild(readiness);

    // ----- counters
    view.appendChild(h('div', { class: 'stats' },
      KM.statCard('Done', count(all, 'done'), { icon: 'check', kind: 'good' }),
      KM.statCard('Partly built', count(all, 'partial'), { icon: 'clock', kind: 'warn' }),
      KM.statCard('Planned', count(all, 'planned'), { icon: 'flag', kind: 'bad' })));

    // ----- the register
    var statusFilter = h('select', null, KM.option('', 'All statuses'), KM.option('planned', 'Planned'), KM.option('partial', 'Partly built'), KM.option('done', 'Done'));
    var priorityFilter = h('select', null, KM.option('', 'All priorities'), KM.option('Critical', 'Critical'), KM.option('High', 'High'), KM.option('Medium', 'Medium'), KM.option('Low', 'Low'));
    var search = h('input', { type: 'search', placeholder: 'Search risks' });
    var list = h('div');
    view.appendChild(KM.sectionTitle('The register'));
    view.appendChild(h('div', { class: 'toolbar three' }, KM.field('Status', statusFilter), KM.field('Priority', priorityFilter), KM.field('Search', search)));
    view.appendChild(list);

    function draw() {
      KM.clear(list);
      var q = search.value.trim().toLowerCase();
      var shown = all.filter(function (x) {
        return (!statusFilter.value || x.status === statusFilter.value)
          && (!priorityFilter.value || x.priority === priorityFilter.value)
          && (!q || (x.risk + ' ' + x.have + ' ' + x.next + ' ' + x.cat).toLowerCase().indexOf(q) !== -1);
      });
      if (!shown.length) { list.appendChild(KM.emptyState({ icon: 'search', title: 'Nothing matches', text: 'Clear a filter.' })); return; }
      var lastCat = '';
      shown.forEach(function (x) {
        if (x.cat !== lastCat) { lastCat = x.cat; list.appendChild(h('h3', { class: 'cat' }, x.cat)); }
        list.appendChild(h('div', { class: 'card control' },
          h('div', { class: 'row' },
            h('strong', null, x.risk),
            h('span', { class: 'chips' }, KM.badge(x.priority, x.priority === 'Critical' ? 'bad' : (x.priority === 'High' ? 'warn' : 'info')), KM.badge(STATUS_TEXT[x.status], STATUS_KIND[x.status]))),
          h('p', null, h('span', { class: 'muted' }, 'Today: '), x.have),
          x.next && x.next !== '-' ? h('p', null, h('span', { class: 'muted' }, 'Next: '), x.next) : null));
      });
    }
    [statusFilter, priorityFilter].forEach(function (el) { el.addEventListener('change', draw); });
    search.addEventListener('input', draw);
    draw();
  };
})();
