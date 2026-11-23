/* KasiMarket front end: screens used by more than one kind of person
   (the safety panel under a job, the SOS button, "My reports" and the Safety centre). */

(function () {
  'use strict';

  var KM = window.KM;
  var h = KM.h;
  var icon = KM.icon;

  // ------------------------------------------------------------------ SOS
  KM.sendSos = async function (jobId) {
    var sure = await KM.confirmDialog({
      title: 'Send an SOS?',
      message: 'KasiMarket staff will see it with your position. If you are in danger, also call the police on 10111, or 112 from a mobile phone.',
      confirmText: 'Send SOS', danger: true
    });
    if (!sure) { return; }
    var body = {};
    try {
      var pos = await KM.getPosition();
      body = { latitude: pos.lat, longitude: pos.lng };
    } catch (e) { /* the SOS must still work without a position */ }
    await KM.api('POST', '/jobs/' + jobId + '/sos', body);
    KM.toast('SOS sent. If you are in danger, call 10111 or 112 now.', true);
  };

  // The "Safety and help" box under a job.
  KM.safetyPanel = function (jobId, allowSos) {
    var severity = h('select', null,
      KM.option('LOW', 'Small problem'), KM.option('MEDIUM', 'Serious problem', true), KM.option('HIGH', 'Unsafe or damage'));
    var description = h('textarea', { maxlength: '500', required: true, placeholder: 'What happened?' });
    var submit = h('button', { type: 'submit', class: 'btn secondary small' }, 'Send report');
    var form = h('form', null,
      KM.field('How serious is it?', severity),
      KM.field('What happened?', description),
      submit);

    form.addEventListener('submit', KM.wrap(submit, async function () {
      await KM.api('POST', '/jobs/' + jobId + '/incidents', { severity: severity.value, description: description.value });
      description.value = '';
      KM.toast('Report sent. You can follow it under Reports.');
    }));

    var panel = h('details', { class: 'card safety' },
      h('summary', null, icon('shield', 18), 'Safety and help'),
      h('h3', null, 'Report a problem'),
      form);

    if (allowSos) {
      panel.appendChild(h('div', { class: 'sos-box' },
        h('h3', null, icon('alert', 18), 'Emergency'),
        h('p', { class: 'muted' }, 'In danger, call 10111 (police), or 112 from a mobile phone. The SOS button tells KasiMarket staff, but staff may not see it straight away.'),
        KM.button('SOS - I need help now', 'danger block', function () { return KM.sendSos(jobId); }, 'alert')));
    }
    return panel;
  };

  // ------------------------------------------------------------------ My reports
  KM.views.reports = async function (view) {
    view.appendChild(h('h1', null, 'My reports'));
    view.appendChild(h('p', { class: 'muted' }, 'Problems you reported, and what we did about them.'));
    var list = await KM.api('GET', '/incidents/mine');
    if (!list.length) {
      view.appendChild(KM.emptyState({ icon: 'shield', title: 'Nothing reported', text: 'You can report a problem from any job. We read every report.' }));
      return;
    }
    list.forEach(function (i) {
      view.appendChild(h('div', { class: 'card' + (i.sos ? ' sos' : '') },
        h('div', { class: 'row' },
          h('strong', null, i.sos ? 'SOS' : 'Job #' + i.jobId),
          h('span', { class: 'chips' }, KM.severityBadge(i.severity), KM.statusBadge(i.status))),
        h('p', null, i.description),
        i.resolution ? h('div', { class: 'answer' }, h('strong', null, 'Answer from KasiMarket'), h('p', null, i.resolution)) : null,
        h('p', { class: 'muted small' }, KM.fmtDate(i.createdAt))));
    });
  };

  // ------------------------------------------------------------------ Safety centre
  function point(iconName, title, text) {
    return h('li', { class: 'point' },
      h('span', { class: 'point-icon' }, icon(iconName, 20)),
      h('div', null, h('strong', null, title), h('p', { class: 'muted' }, text)));
  }

  KM.views.safety = function (view) {
    var role = KM.getSession().role;
    var isClient = role === 'CLIENT';

    view.appendChild(h('div', { class: 'hero small-hero' },
      h('div', { class: 'hero-icon' }, icon('shield', 30)),
      h('h1', null, 'Your safety comes first'),
      h('p', null, 'KasiMarket sends people to each other\'s homes, so safety is built into every step. We are honest about what we do and what we are still building.')));

    view.appendChild(KM.sectionTitle('How we protect you'));
    var list = h('ul', { class: 'points' });
    list.appendChild(point('check', 'Workers are approved first', 'A KasiMarket staff member must approve every worker before they can take a job.'));
    list.appendChild(point('lock', 'Your details stay private', isClient
      ? 'Your address is shown to a worker only after they accept. We never share phone numbers.'
      : 'Client addresses are shown only after you accept. We never share phone numbers.'));
    list.appendChild(point('key', 'The start code', isClient
      ? 'You get a 4-digit code. Give it to the worker only when they arrive and you have checked their name. No code, no start.'
      : 'The client gives you a 4-digit code when you arrive. You need it to start the work, which proves you are at the right door.'));
    list.appendChild(point('pin', 'Tracking only when needed', 'A worker\'s position is shared while they are online or on a job, and erased when they go offline.'));
    list.appendChild(point('camera', 'Photo proof', 'A photo is required when a job is finished, so there is a record.'));
    list.appendChild(point('alert', 'Report or SOS at any time', 'Use "Safety and help" on any active job. Staff read reports, most serious first.'));
    view.appendChild(list);

    view.appendChild(KM.sectionTitle(isClient ? 'Tips for clients' : 'Tips for workers'));
    var tips = isClient
      ? ['Check the worker\'s name and rating on the app before you open the door.',
         'Do not share your start code before you have seen the worker.',
         'Keep valuables out of sight, and keep pets on a lead or inside.',
         'Never pay cash or share bank details. Everything is paid through KasiMarket.',
         'If something feels wrong, cancel the job or press SOS. You do not have to explain.']
      : ['You can say no to a job that feels unsafe. Use "Drop this job" before you start.',
         'Tell someone you trust where you are going.',
         'Do not accept payment or tips outside the app, and do not give out your phone number.',
         'Take the finishing photo before you leave.',
         'If you feel in danger, leave and press SOS when you are safe.'];
    var tipList = h('ul', { class: 'tips' });
    tips.forEach(function (t) { tipList.appendChild(h('li', null, t)); });
    view.appendChild(h('div', { class: 'card' }, tipList));

    view.appendChild(KM.sectionTitle('In an emergency'));
    view.appendChild(h('div', { class: 'card sos' },
      h('p', null, h('strong', null, 'Police: 10111'), '   |   ', h('strong', null, 'Any mobile phone: 112')),
      h('p', { class: 'muted' }, 'The SOS button tells KasiMarket staff, but it is not an emergency service and staff may not see it straight away. In danger, call first.')));

    view.appendChild(KM.sectionTitle('We will never'));
    var never = h('ul', { class: 'tips' });
    ['Ask for your password, bank details or card number.',
     'Ask for your start code. Only the worker at your door needs it.',
     'Tell you a worker is "100 percent safe". No one can promise that. We tell you what is checked.'].forEach(function (t) { never.appendChild(h('li', null, t)); });
    view.appendChild(h('div', { class: 'card' }, never));

    view.appendChild(h('div', { class: 'card notice' },
      h('strong', null, 'We are a pilot'),
      h('p', { class: 'muted' }, 'We are still adding live ID matching, real payments and insurance. We will say so on this page when each one is ready.')));
  };
})();
