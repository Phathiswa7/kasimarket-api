/* KasiMarket front end: the worker's screens.
   Home (profile, earnings, online switch, jobs near you), My work, and Documents. */

(function () {
  'use strict';

  var KM = window.KM;
  var h = KM.h;
  var icon = KM.icon;
  var money = KM.money;

  // ====================================================================
  // Home: profile, earnings, online switch and jobs near you
  // ====================================================================
  var NOT_APPROVED = {
    SUBMITTED: 'Your application is waiting for approval. Upload your ID and police clearance so staff can check them.',
    UNDER_REVIEW: 'Staff are looking at your application. We will let you know here.',
    REJECTED: 'Your application was not approved. If you think this is a mistake, please contact KasiMarket support.',
    SUSPENDED: 'Your account is paused while we look into something. Please contact KasiMarket support.'
  };

  function sum(list, status) {
    return list.filter(function (j) { return j.status === status; })
      .reduce(function (total, j) { return total + j.workerEarning; }, 0);
  }

  KM.views.workerHome = async function (view) {
    var me = await KM.api('GET', '/workers/me');

    view.appendChild(h('div', { class: 'hero' },
      h('p', { class: 'hero-eyebrow' }, 'Welcome back'),
      h('h1', null, 'Hi, ' + KM.firstName(me.fullName)),
      h('p', null, me.vettingStatus === 'APPROVED' ? 'Go online to see jobs near you.' : 'Let us get you approved.')));

    // ----- profile card
    var profile = h('div', { class: 'card profile' },
      KM.avatar(me.fullName, 'lg'),
      h('div', { class: 'worker-info' },
        h('strong', null, me.fullName),
        h('div', { class: 'chips' }, KM.statusBadge(me.vettingStatus), KM.tierBadge(me.tier)),
        h('div', { class: 'rating-line' }, KM.stars(me.avgRating),
          h('span', { class: 'muted small' }, me.avgRating ? me.avgRating + ' - ' + me.jobsCompleted + ' job(s)' : 'No ratings yet'))));
    view.appendChild(profile);

    if (me.vettingStatus !== 'APPROVED') {
      view.appendChild(h('div', { class: 'card notice' },
        h('h3', null, 'Not approved yet'),
        h('p', null, NOT_APPROVED[me.vettingStatus] || 'Your account is not active yet.'),
        h('a', { class: 'btn', href: '#/worker/docs' }, icon('file', 18), 'My documents')));
      return;
    }

    // ----- the trust ladder
    if (me.tier === 1) {
      var done = Math.min(me.jobsCompleted, me.tier2MinJobs);
      view.appendChild(h('div', { class: 'card' },
        h('div', { class: 'row' }, h('strong', null, 'Next: Tier 2 - Trusted'), h('span', { class: 'muted small' }, done + ' of ' + me.tier2MinJobs + ' paid jobs')),
        KM.progress(done, me.tier2MinJobs, 'Paid jobs towards Tier 2'),
        h('p', { class: 'muted small' }, 'You also need an average rating of ' + me.tier2MinRating + ' or more' + (me.avgRating ? ' (yours is ' + me.avgRating + ').' : '.') + ' Higher tiers unlock better-paid jobs.')));
    } else if (me.tier === 2) {
      view.appendChild(h('div', { class: 'card' },
        h('strong', null, 'You are Tier 2 - Trusted'),
        h('p', { class: 'muted small' }, 'Tier 3 jobs need extra checks. KasiMarket staff grant them after a review.')));
    }

    // ----- earnings
    var earnings = h('div', { class: 'stats' });
    view.appendChild(earnings);
    KM.api('GET', '/jobs/my-work').then(function (work) {
      KM.clear(earnings);
      earnings.appendChild(KM.statCard('Earned', money(sum(work, 'PAID')), { icon: 'coin', kind: 'good' }));
      earnings.appendChild(KM.statCard('Waiting for payment', money(sum(work, 'COMPLETED')), { icon: 'clock' }));
      earnings.appendChild(KM.statCard('Jobs on now', work.filter(function (j) { return ['ACCEPTED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS'].indexOf(j.status) !== -1; }).length, { icon: 'zap', href: '#/worker/work' }));
    }).catch(function () { /* the earnings are a nice extra, not essential */ });

    // ----- online switch
    var onlineCard = h('div', { class: 'card' });
    var list = h('div');
    view.appendChild(onlineCard);
    view.appendChild(KM.sectionTitle('Jobs near you'));
    view.appendChild(list);

    function drawOnline() {
      KM.clear(onlineCard);
      var online = KM.isOnline();
      onlineCard.appendChild(h('div', { class: 'row' },
        h('div', null, h('strong', null, online ? 'You are online' : 'You are offline'),
          h('p', { class: 'muted small' }, online ? 'Your position is shared so nearby clients can find you. It is erased when you go offline.' : 'Go online to see jobs close to you.')),
        KM.switchControl(online, online ? 'Online' : 'Offline', async function (on, input) {
          input.disabled = true;
          try {
            if (on) { await KM.goOnline('gps'); KM.toast('You are online.'); } else { await KM.goOffline(); KM.toast('You are offline. Your position was erased.'); }
          } catch (e) {
            input.checked = !on;
            KM.toast(e.message, true);
          }
          drawOnline();
          loadJobs().catch(function () { /* shown inside the list */ });
        })));
      if (!online) {
        onlineCard.appendChild(h('div', { class: 'actions' },
          KM.button('Go online with the demo location', 'secondary small', async function () {
            await KM.goOnline('demo');
            drawOnline();
            await loadJobs();
          })));
      }
    }

    async function loadJobs() {
      KM.clear(list);
      if (!KM.isOnline()) {
        list.appendChild(KM.emptyState({ icon: 'power', title: 'You are offline', text: 'Switch to online to see jobs.' }));
        return;
      }
      var jobs;
      try {
        jobs = await KM.api('GET', '/jobs/available');
      } catch (e) {
        if (e.status === 409) {
          sessionStorage.removeItem('km_online');
          KM.stopLocationTimer();
          drawOnline();
          list.appendChild(KM.emptyState({ icon: 'pin', title: 'Share your location first', text: 'Switch online so we know where you are.' }));
          return;
        }
        throw e;
      }
      if (!jobs.length) {
        list.appendChild(KM.emptyState({ icon: 'search', title: 'No jobs near you right now', text: 'This page checks again by itself.' }));
        return;
      }
      jobs.forEach(function (j) {
        list.appendChild(h('div', { class: 'card job-offer' },
          h('div', { class: 'row' },
            h('strong', null, j.taskName),
            j.urgent ? KM.badge('Urgent', 'warn') : null),
          h('div', { class: 'row' },
            h('span', { class: 'muted' }, icon('pin', 14), ' About ' + j.distanceKm + ' km away - ' + j.hours + ' hour(s)'),
            h('span', { class: 'big good-text' }, money(j.workerEarning))),
          h('p', { class: 'muted small' }, 'You earn ' + money(j.workerEarning) + '. The address is shown after you accept.'),
          KM.button('Accept this job', 'block', async function () {
            await KM.api('POST', '/jobs/' + j.id + '/accept');
            KM.toast('Job accepted.');
            KM.go('#/worker/work');
          }, 'check')));
      });
    }

    drawOnline();
    try { await loadJobs(); } catch (e) { list.appendChild(h('p', null, e.message)); }
    KM.poll(function () { if (KM.isOnline()) { loadJobs().catch(function () { /* keep what we show */ }); } }, 10000);
  };

  // ====================================================================
  // My work
  // ====================================================================
  var NEXT_STEP = {
    ACCEPTED: ['EN_ROUTE', 'I am on my way'],
    EN_ROUTE: ['ARRIVED', 'I have arrived'],
    ARRIVED: ['IN_PROGRESS', 'Start the work']
  };

  KM.views.workerWork = async function (view) {
    view.appendChild(h('h1', null, 'My work'));
    var list = h('div');
    view.appendChild(list);

    async function load() {
      var jobs = await KM.api('GET', '/jobs/my-work');
      KM.clear(list);
      if (!jobs.length) {
        list.appendChild(KM.emptyState({ icon: 'list', title: 'No jobs yet', text: 'Accept a job near you and it will show here.', action: h('a', { class: 'btn', href: '#/worker/jobs' }, 'Find a job') }));
        return;
      }
      jobs.forEach(function (j) { list.appendChild(workCard(j, load)); });
    }

    await load();
    KM.poll(function () {
      // Do not redraw while the worker is typing, choosing a photo or has a dialog open.
      if (list.querySelector('details[open]') || document.querySelector('.modal-backdrop') || list.querySelector('input[type="file"]:focus')) { return; }
      load().catch(function () { /* keep what we show */ });
    }, 15000);
  };

  function workCard(j, reload) {
    var card = h('div', { class: 'card work-card s-' + j.status.toLowerCase() },
      h('div', { class: 'row' }, h('strong', null, j.taskName), KM.statusBadge(j.status)),
      h('p', null, icon('pin', 14), ' ', h('strong', null, j.address)),
      j.notes ? h('p', null, h('strong', null, 'Notes: '), j.notes) : null,
      h('p', { class: 'muted small' }, j.hours + ' hour(s)' + (j.urgent ? ' - urgent' : '') + ' - you earn ' + money(j.workerEarning)),
      h('a', { class: 'btn secondary small', target: '_blank', rel: 'noopener noreferrer',
               href: 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(j.address) }, icon('pin', 16), 'Open in Maps'));

    var actions = h('div', { class: 'actions' });
    var next = NEXT_STEP[j.status];

    if (next) {
      actions.appendChild(KM.button(next[1], '', async function () {
        var body = { status: next[0] };

        // Starting the work needs the client's 4-digit code: it proves the right person is at the door.
        if (next[0] === 'IN_PROGRESS') {
          var config = await KM.getConfig();
          var required = config.startCodeRequired !== false;
          var code = await KM.askText({
            title: 'Enter the start code',
            message: required ? 'Ask the client for their 4-digit start code.' : 'Ask the client for their 4-digit start code. In demo mode you may leave it empty.',
            label: 'Start code', numeric: true, maxlength: 6, required: required, confirmText: 'Start'
          });
          if (code === null) { return; }
          if (code) { body.code = code; }
        }

        await KM.api('POST', '/jobs/' + j.id + '/status', body);
        // While travelling, the client sees how far away the worker is, so send a fresh position.
        if (next[0] === 'EN_ROUTE' && KM.isOnline()) { KM.sendWorkerLocation().catch(function () { /* ignore */ }); }
        KM.toast('Updated.');
        await reload();
      }));
      actions.appendChild(KM.button('Drop this job', 'secondary', async function () {
        var sure = await KM.confirmDialog({ title: 'Give this job back?', message: 'It goes back to the list for other workers.', confirmText: 'Drop job', danger: true });
        if (!sure) { return; }
        await KM.api('POST', '/jobs/' + j.id + '/drop');
        KM.toast('The job went back to the list.');
        await reload();
      }));
    }

    if (j.status === 'IN_PROGRESS') {
      var file = h('input', { type: 'file', accept: 'image/*', capture: 'environment' });
      var finish = h('button', { type: 'button', class: 'btn block' }, icon('camera', 18), 'Finish the job with this photo');
      finish.addEventListener('click', KM.wrap(finish, async function () {
        if (!file.files || !file.files[0]) { throw new Error('Take or choose a photo of the finished work first.'); }
        var form = new FormData();
        form.append('file', file.files[0]);
        await KM.api('POST', '/jobs/' + j.id + '/complete', form);
        KM.toast('Job finished. Now we wait for the client to confirm.');
        await reload();
      }));
      card.appendChild(KM.field('Photo of the finished work', file, 'A JPEG or PNG picture.'));
      card.appendChild(finish);
    }

    if (j.status === 'COMPLETED') { card.appendChild(h('p', { class: 'muted' }, 'Finished. Waiting for the client to confirm and pay.')); }
    if (j.status === 'PAID') { card.appendChild(h('p', { class: 'good-text' }, 'Paid. You earned ' + money(j.workerEarning) + '.')); }
    if (j.status === 'CANCELLED') { card.appendChild(h('p', { class: 'muted' }, 'The client cancelled this job.')); }
    if (actions.childNodes.length) { card.appendChild(actions); }

    if (['ACCEPTED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED'].indexOf(j.status) !== -1) {
      card.appendChild(KM.safetyPanel(j.id, true));
    }
    return card;
  }

  // ====================================================================
  // Documents
  // ====================================================================
  var DOC_LABEL = { ID_DOCUMENT: 'ID document', POLICE_CLEARANCE: 'Police clearance', REFERENCE_LETTER: 'Reference letter' };
  KM.DOC_LABEL = DOC_LABEL;

  // The best state a worker has for one kind of document.
  function bestState(docs, type) {
    var mine = docs.filter(function (d) { return d.type === type; });
    if (mine.some(function (d) { return d.reviewStatus === 'ACCEPTED'; })) { return 'ACCEPTED'; }
    if (mine.some(function (d) { return d.reviewStatus === 'PENDING'; })) { return 'PENDING'; }
    if (mine.length) { return 'REJECTED'; }
    return null;
  }

  KM.views.workerDocs = async function (view) {
    view.appendChild(h('h1', null, 'My documents'));
    var checklist = h('div', { class: 'card' });
    var list = h('div');

    var type = h('select', null,
      KM.option('ID_DOCUMENT', 'ID document'),
      KM.option('POLICE_CLEARANCE', 'Police clearance certificate'),
      KM.option('REFERENCE_LETTER', 'Reference letter'));
    var file = h('input', { type: 'file', accept: '.pdf,.jpg,.jpeg,.png,image/*,application/pdf' });
    var validUntil = h('input', { type: 'date' });
    var validField = KM.field('Valid until', validUntil, 'Write the date shown on the certificate.');
    var submit = h('button', { type: 'submit', class: 'btn block' }, icon('file', 18), 'Upload');
    var form = h('form', null, KM.field('Which document?', type), KM.field('The file (PDF, JPEG or PNG)', file), validField, submit);

    function syncValid() { validField.hidden = type.value !== 'POLICE_CLEARANCE'; }
    type.addEventListener('change', syncValid);
    syncValid();

    form.addEventListener('submit', KM.wrap(submit, async function () {
      if (!file.files || !file.files[0]) { throw new Error('Choose a file first.'); }
      var data = new FormData();
      data.append('type', type.value);
      if (type.value === 'POLICE_CLEARANCE' && validUntil.value) { data.append('validUntil', validUntil.value); }
      data.append('file', file.files[0]);
      await KM.api('POST', '/workers/me/documents', data);
      KM.toast('Uploaded. An admin will check it.');
      form.reset();
      syncValid();
      await load();
    }));

    function stateRow(label, state) {
      var kind = state === 'ACCEPTED' ? 'good' : (state === 'REJECTED' ? 'bad' : 'warn');
      return h('li', { class: 'check-row' },
        h('span', { class: 'check-icon ' + (state === 'ACCEPTED' ? 'ok' : '') }, icon(state === 'ACCEPTED' ? 'check' : 'file', 16)),
        h('span', null, label),
        state ? KM.statusBadge(state) : KM.badge('Not uploaded', kind));
    }

    async function load() {
      var docs = await KM.api('GET', '/workers/me/documents');
      KM.clear(checklist);
      checklist.appendChild(h('h3', null, 'What you need'));
      checklist.appendChild(h('ul', { class: 'checklist' },
        stateRow('ID document', bestState(docs, 'ID_DOCUMENT')),
        stateRow('Police clearance (still valid)', bestState(docs, 'POLICE_CLEARANCE'))));
      checklist.appendChild(h('p', { class: 'muted small' }, 'A KasiMarket admin checks every document. Only admins can open them.'));

      KM.clear(list);
      if (!docs.length) {
        list.appendChild(KM.emptyState({ icon: 'file', title: 'Nothing uploaded yet', text: 'Upload your ID first.' }));
        return;
      }
      docs.forEach(function (d) {
        list.appendChild(h('div', { class: 'card' },
          h('div', { class: 'row' }, h('strong', null, DOC_LABEL[d.type] || d.type), KM.statusBadge(d.reviewStatus)),
          d.validUntil ? h('p', { class: 'muted' }, 'Valid until ' + d.validUntil) : null,
          d.reviewNote ? h('div', { class: 'answer' }, h('strong', null, 'Admin note'), h('p', null, d.reviewNote)) : null,
          h('p', { class: 'muted small' }, 'Uploaded ' + KM.fmtDate(d.uploadedAt))));
      });
    }

    view.appendChild(checklist);
    view.appendChild(h('div', { class: 'card' }, h('h3', null, 'Upload a document'), form));
    view.appendChild(KM.sectionTitle('Uploaded documents'));
    view.appendChild(list);
    await load();
  };
})();
