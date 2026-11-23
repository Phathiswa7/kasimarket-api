/* KasiMarket: the live risk and controls register.
   Built from the "On-Demand Jobs Platform: Risk and Counter Register" and the investor action plan.
   Each line says what the risk is, what KasiMarket does about it today, and what is still to do.
   STATUS is honest on purpose: done (built and tested), partial (some of it built), planned (not built yet). */

(function () {
  'use strict';

  var KM = window.KM;

  KM.CONTROLS = {
    // The launch blockers: things that must be true before real people use the app.
    blockers: [
      { text: 'Identity and phone verification', status: 'partial', note: 'Staff review ID documents. Live selfie match and phone codes are not built yet.' },
      { text: 'Hosted, secure payments (no card data on our servers)', status: 'planned', note: 'Payments are simulated. A certified payment provider is needed.' },
      { text: 'Payout account verification (account in the worker\'s own name)', status: 'planned', note: 'Needs the payment provider.' },
      { text: 'Location-verified check-in and check-out with photo evidence', status: 'partial', note: 'Finishing photo and start code are built. GPS checks at arrival and before photos are planned.' },
      { text: 'Access-control tests on every personal-data address', status: 'done', note: 'Automated tests try to open other people\'s jobs, photos, reports and documents.' },
      { text: 'Contact details hidden until booking', status: 'done', note: 'Phone numbers are never shared. A worker sees the address only after accepting.' },
      { text: 'Panic button, incident reports and suspension', status: 'partial', note: 'SOS and reports reach staff, most serious first. Alerts to a phone are not built yet.' },
      { text: 'Insurance for theft, damage and injury', status: 'planned', note: 'Quotes needed from three brokers.' },
      { text: 'Legal advice: worker status, holding funds, POPIA', status: 'planned', note: 'A South African attorney must confirm.' },
      { text: 'Written dispute and refund policy', status: 'planned', note: 'To be written and shown in the app.' }
    ],

    items: [
      // ---------------------------------------------------------------- Marketplace
      { cat: 'Marketplace', priority: 'Critical', status: 'partial', risk: 'Clients and workers move off the app to avoid fees',
        have: 'No phone numbers or emails are shown. Address is shown only after accepting.', next: 'Insurance and payment guarantee only for in-app jobs. Reduced fee for repeat bookings.' },
      { cat: 'Marketplace', priority: 'Critical', status: 'partial', risk: 'Fake or duplicate worker accounts',
        have: 'Staff approve every worker. ID and clearance documents are reviewed.', next: 'Live selfie match, phone codes, duplicate device checks.' },
      { cat: 'Marketplace', priority: 'High', status: 'partial', risk: 'Fake or traded reviews',
        have: 'Ratings only after a paid job, once per job.', next: 'Pattern detection. Workers rating clients.' },
      { cat: 'Marketplace', priority: 'High', status: 'partial', risk: 'Workers or clients ghosting',
        have: 'A worker can drop a job back to the list. Unaccepted jobs expire after 30 minutes.', next: 'Late-cancel fees and reliability scores.' },
      { cat: 'Marketplace', priority: 'Medium', status: 'partial', risk: 'Gaming the job matching',
        have: 'Workers must share a fresh position to see jobs. Only nearby jobs shown.', next: 'Limits on repeated declines.' },
      { cat: 'Marketplace', priority: 'Medium', status: 'done', risk: 'Price collusion or haggling',
        have: 'The system sets fixed prices. The price is locked at booking.', next: 'Keep the task list under review.' },
      { cat: 'Marketplace', priority: 'High', status: 'partial', risk: 'Unfair matching or discrimination',
        have: 'Matching uses distance and trust tier only.', next: 'Regular outcome checks and a way to challenge a rating.' },

      // ---------------------------------------------------------------- Payments
      { cat: 'Payments', priority: 'Critical', status: 'planned', risk: 'Stolen cards and card testing',
        have: 'Payments are simulated. No card data exists in the app.', next: 'Hosted provider with 3-D Secure. Limits for new clients.' },
      { cat: 'Payments', priority: 'Critical', status: 'partial', risk: 'Chargebacks and false "not done" claims',
        have: 'A job is paid only after the client confirms. Photo and history are kept.', next: 'Held funds with a real provider and an evidence pack.' },
      { cat: 'Payments', priority: 'High', status: 'partial', risk: 'Fake job completion',
        have: 'Finishing photo required. The start code proves a person was at the door.', next: 'GPS check at arrival and finish. Before photos.' },
      { cat: 'Payments', priority: 'Critical', status: 'planned', risk: 'Payout fraud and account swaps',
        have: 'Not built.', next: 'Payouts only to an account in the worker\'s own verified name.' },
      { cat: 'Payments', priority: 'High', status: 'planned', risk: 'Money laundering through fake jobs',
        have: 'Not built.', next: 'Identity checks, limits and monitoring. Attorney advice.' },
      { cat: 'Payments', priority: 'High', status: 'planned', risk: 'Tax and withholding duties',
        have: 'Not built.', next: 'Accountant advice. Earnings statements.' },
      { cat: 'Payments', priority: 'Low', status: 'done', risk: 'Price changes after booking',
        have: 'Price, fee and worker pay are saved on the job when it is booked.', next: '-' },

      // ---------------------------------------------------------------- Security
      { cat: 'Security', priority: 'Critical', status: 'partial', risk: 'Account takeover',
        have: 'Passwords are scrambled (BCrypt). Five wrong passwords lock an email for 15 minutes. Signed login tokens.', next: 'Two-step login for staff and workers.' },
      { cat: 'Security', priority: 'High', status: 'planned', risk: 'SIM swap and code interception',
        have: 'No SMS codes yet.', next: 'Use app-based codes where possible.' },
      { cat: 'Security', priority: 'Critical', status: 'partial', risk: 'Opening other people\'s data by changing a number in the address',
        have: 'Every job, photo, report and document address checks who owns it. Automated tests try to break it.', next: 'Independent penetration test.' },
      { cat: 'Security', priority: 'Medium', status: 'partial', risk: 'Bots, scraping and brute force',
        have: 'Login lock. No public listings.', next: 'Limits per network address.' },
      { cat: 'Security', priority: 'High', status: 'partial', risk: 'Injection and script attacks',
        have: 'Database access is parameterised. Screens show server text as plain text.', next: 'Penetration test and content security policy.' },
      { cat: 'Security', priority: 'High', status: 'planned', risk: 'Vulnerable libraries',
        have: 'Not built.', next: 'Automatic dependency scanning.' },
      { cat: 'Security', priority: 'High', status: 'done', risk: 'Phishing through in-app messages',
        have: 'There is no chat yet, so there are no links to abuse.', next: 'Masked messaging later, with link blocking.' },
      { cat: 'Security', priority: 'High', status: 'partial', risk: 'Malicious or fake uploads',
        have: 'Files are checked by their real content, saved under random names in a private folder, and size-limited.', next: 'Malware scan and live selfie checks.' },
      { cat: 'Security', priority: 'High', status: 'partial', risk: 'Staff misuse of data',
        have: 'Staff addresses are locked to the staff role.', next: 'Audit log of every staff action. Two-step login.' },
      { cat: 'Security', priority: 'High', status: 'done', risk: 'Secrets left in the code',
        have: 'Production reads secrets from environment variables. The app refuses to start with unsafe settings.', next: 'Move to a managed key vault when hosted.' },
      { cat: 'Security', priority: 'High', status: 'planned', risk: 'Outage or data loss',
        have: 'Data lives in PostgreSQL.', next: 'Backups and a tested restore.' },

      // ---------------------------------------------------------------- Privacy
      { cat: 'Privacy', priority: 'Critical', status: 'partial', risk: 'Leak of IDs and clearances',
        have: 'Collected on purpose, minimal, only staff can open them.', next: 'Encrypt at rest. Information Officer. Breach plan.' },
      { cat: 'Privacy', priority: 'High', status: 'partial', risk: 'Misuse of location data',
        have: 'A worker\'s position is kept only while online and erased when they go offline.', next: 'Delete job positions after a set time.' },
      { cat: 'Privacy', priority: 'Medium', status: 'planned', risk: 'Keeping data too long',
        have: 'Not built.', next: 'Retention rules and deletion on request.' },
      { cat: 'Privacy', priority: 'High', status: 'planned', risk: 'Data stored in another country',
        have: 'Runs locally for now.', next: 'Choose hosting with the attorney.' },

      // ---------------------------------------------------------------- Physical safety
      { cat: 'Physical safety', priority: 'Critical', status: 'partial', risk: 'The wrong person arrives (impersonation)',
        have: 'The client reads out a 4-digit start code at the door. No code, no start.', next: 'Worker photo shown on arrival.' },
      { cat: 'Physical safety', priority: 'Critical', status: 'partial', risk: 'Theft or damage',
        have: 'Finishing photo, full job history, reports, quick suspension.', next: 'Before photos, handover code, insurance and a guarantee fund.' },
      { cat: 'Physical safety', priority: 'Critical', status: 'partial', risk: 'Assault or harassment',
        have: 'SOS and reports for both sides. Staff see the most serious first. Workers can drop a job.', next: 'SOS alerts by phone and a response partner.' },
      { cat: 'Physical safety', priority: 'High', status: 'planned', risk: 'Injury at work',
        have: 'Not built.', next: 'Accident cover for workers.' },
      { cat: 'Physical safety', priority: 'Critical', status: 'done', risk: 'Dangerous or unlicensed work',
        have: 'Only low-risk tasks in the pilot. Staff can switch any task off.', next: 'Add task types one at a time.' },
      { cat: 'Physical safety', priority: 'High', status: 'done', risk: 'Children and vulnerable people',
        have: 'Childcare and house watching are off in production. Higher-risk tasks need Tier 3, granted by staff.', next: 'Only return with legal advice and data.' },

      // ---------------------------------------------------------------- Legal
      { cat: 'Legal', priority: 'Critical', status: 'planned', risk: 'Worker classification (employee or contractor)',
        have: 'Not decided.', next: 'Labour attorney opinion before launch.' },
      { cat: 'Legal', priority: 'High', status: 'planned', risk: 'Holding client money',
        have: 'Payments are simulated.', next: 'Licensed payment partner. Attorney confirms.' },
      { cat: 'Legal', priority: 'High', status: 'partial', risk: 'Consumer protection and honest claims',
        have: 'The price is shown before booking. The app never claims 100 percent safety.', next: 'Plain-language terms and refund policy.' },
      { cat: 'Legal', priority: 'High', status: 'planned', risk: 'Human trafficking and exploitation',
        have: 'One account per approved worker. Staff review.', next: 'Own-name payouts, in-person onboarding, partner organisation, written policy.' },

      // ---------------------------------------------------------------- Operations
      { cat: 'Operations', priority: 'High', status: 'partial', risk: 'Too few workers or too few clients',
        have: 'Nearby-job matching. Launch in one suburb.', next: 'Partner demand. Grow workers in small groups.' },
      { cat: 'Operations', priority: 'High', status: 'partial', risk: 'Support overload',
        have: 'Staff incident queue sorted by seriousness.', next: 'Dispute workflow and support hours.' },
      { cat: 'Operations', priority: 'High', status: 'planned', risk: 'A public safety incident',
        have: 'Incident records and suspension.', next: 'Crisis plan, partner endorsements, published safety numbers.' }
    ]
  };
})();
