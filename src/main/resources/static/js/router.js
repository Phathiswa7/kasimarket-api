const Router = {
    init() {
        this.renderNav();
        const session = Core.getSession();
        if (!session) {
            this.navigate('login');
        } else if (session.role === 'ADMIN') {
            this.navigate('admin-dashboard');
        } else if (session.role === 'WORKER') {
            this.navigate('worker-dashboard');
        } else {
            this.navigate('client-dashboard');
        }
    },

    renderNav() {
        const nav = document.getElementById('main-nav');
        const session = Core.getSession();
        if (!session) {
            nav.innerHTML = `<button class="btn-secondary" onclick="Router.navigate('login')">Sign In</button>`;
            return;
        }

        let links = '';
        if (session.role === 'ADMIN') {
            links = `
        <button onclick="Router.navigate('admin-dashboard')">Vetting Queue</button>
        <button onclick="Router.navigate('admin-controls')">Risk & Controls Register</button>
      `;
        } else if (session.role === 'WORKER') {
            links = `
        <button onclick="Router.navigate('worker-dashboard')">My Jobs & Trust Score</button>
        <button onclick="Router.navigate('worker-psychometric')">Psychometric Test</button>
        <button onclick="Router.navigate('worker-academy')">20-Min Exam & AI Coach</button>
        <button class="btn-sos-sm" onclick="Router.navigate('safety')">SOS Safety</button>
      `;
        } else {
            links = `
        <button onclick="Router.navigate('client-dashboard')">Book a Task</button>
        <button class="btn-sos-sm" onclick="Router.navigate('safety')">SOS Safety</button>
      `;
        }

        nav.innerHTML = `
      <span class="user-pill">${session.fullName} (${session.role})</span>
      ${links}
      <button class="btn-secondary" onclick="Core.logout()">Logout</button>
    `;
    },

    navigate(route) {
        const root = document.getElementById('app-root');
        const session = Core.getSession();

        switch (route) {
            case 'login':
                this.renderLogin(root);
                break;
            case 'admin-dashboard':
                AdminModule.renderDashboard(root);
                break;
            case 'admin-controls':
                this.renderAdminControlsRegister(root);
                break;
            case 'worker-dashboard':
                this.renderWorkerDashboard(root, session);
                break;
            case 'worker-psychometric':
                this.renderPsychometricTest(root, session);
                break;
            case 'worker-academy':
                WorkerAcademy.renderHub(root, session.workerId || 1);
                break;
            case 'client-dashboard':
                this.renderClientDashboard(root, session);
                break;
            case 'safety':
                Core.renderSafetyCenter(root);
                break;
            default:

                if (!session) {
                    this.renderLogin(root);
                } else if (session.role === 'ADMIN') {
                    this.navigate('admin-dashboard');
                } else if (session.role === 'WORKER') {
                    this.navigate('worker-dashboard');
                } else {
                    this.navigate('client-dashboard');
                }
        }
    },

    renderLogin(root) {
        root.innerHTML = `
      <div class="card login-card">
        <h2>Welcome to KasiMarket</h2>
        <p>Select a role to enter the platform or run a live demo:</p>

        <label>Role</label>
        <select id="login-role">
          <option value="CLIENT">Client (Book Verified Help)</option>
          <option value="WORKER">Worker (Earn, Learn & Build Trust)</option>
          <option value="ADMIN">Admin (Vetting, Safety & Risk Controls)</option>
        </select>

        <label>Full Name</label>
        <input type="text" id="login-name" value="Sipho Dlamini" />

        <label>Worker Profile ID (for Worker testing)</label>
        <input type="number" id="login-worker-id" value="1" />

        <button class="btn-primary" onclick="Router.handleLogin()">Enter Platform</button>
      </div>
    `;
    },

    handleLogin() {
        const role = document.getElementById('login-role').value;
        const fullName = document.getElementById('login-name').value || 'Demo User';
        const workerId = parseInt(document.getElementById('login-worker-id').value, 10) || 1;

        Core.setSession({ role, fullName, workerId });
        this.init();
    },

    renderWorkerDashboard(root, session) {
        root.innerHTML = `
      <div class="card">
        <h2>👷 Worker Hub — ${session.fullName}</h2>
        <p>Complete your onboarding assessments and Academy courses to unlock Tier 2 & Tier 3 high-paying bookings.</p>
        <div class="grid-3">
          <div class="mini-box">
            <h4>Step 1: Psychometric</h4>
            <p>Behavioral & judgment assessment.</p>
            <button onclick="Router.navigate('worker-psychometric')">Take Assessment</button>
          </div>
          <div class="mini-box">
            <h4>Step 2: 20-Min Skill Exam</h4>
            <p>Prove practical skill in your category.</p>
            <button onclick="Router.navigate('worker-academy')">Start 20-Min Exam</button>
          </div>
          <div class="mini-box">
            <h4>Step 3: Study & AI Coach</h4>
            <p>Earn points, Trust Score & tips.</p>
            <button onclick="Router.navigate('worker-academy')">Open Academy & Bot</button>
          </div>
        </div>
      </div>
    `;
    },

    renderPsychometricTest(root, session) {
        const workerId = session.workerId || 1;
        root.innerHTML = `
      <div class="card">
        <h2>🧠 Behavioral & Psychometric Screening</h2>
        <p>Evaluates decision-making, honesty, patience, and emotional stability before platform activation.</p>
        
        <div class="question-block">
          <p><strong>1. Integrity & Honesty:</strong> While cleaning a client's living room, you find R500 cash under a sofa cushion. What do you do?</p>
          <label><input type="radio" name="pq1" value="35"> Place it visibly on the table, take a photo in the app, and notify the client immediately.</label><br>
          <label><input type="radio" name="pq1" value="0"> Keep it because the client probably forgot about it.</label>
        </div>

        <div class="question-block">
          <p><strong>2. Stress & Patience:</strong> A client's dog pulls hard on the leash and barks at another dog during a walk. How do you respond?</p>
          <label><input type="radio" name="pq2" value="35"> Stay calm, shorten the leash firmly, cross to a quiet path, and redirect the dog's attention.</label><br>
          <label><input type="radio" name="pq2" value="0"> Yell loudly and jerk the leash aggressively.</label>
        </div>

        <div class="question-block">
          <p><strong>3. Accountability:</strong> You realize traffic will make you 10 minutes late to a booking. What is the right action?</p>
          <label><input type="radio" name="pq3" value="30"> Message the client through KasiMarket immediately with your updated ETA.</label><br>
          <label><input type="radio" name="pq3" value="0"> Say nothing and hope they don't notice.</label>
        </div>

        <button class="btn-primary" onclick="Router.submitPsychometric(${workerId})">Submit Psychometric Assessment</button>
      </div>
    `;
    },

    async submitPsychometric(workerId) {
        const q1 = parseInt(document.querySelector('input[name="pq1"]:checked')?.value || '0', 10);
        const q2 = parseInt(document.querySelector('input[name="pq2"]:checked')?.value || '0', 10);
        const q3 = parseInt(document.querySelector('input[name="pq3"]:checked')?.value || '0', 10);
        const score = q1 + q2 + q3;

        try {
            const updated = await Core.api(`/workers/${workerId}/assessments/psychometric`, {
                method: 'POST',
                body: JSON.stringify({ score })
            });
            alert(`Psychometric Score: ${score}% (${updated.psychometricStatus}). Next step: Take your 20-Minute Category Exam!`);
            this.navigate('worker-academy');
        } catch (err) {
            alert('Saved locally (start Spring Boot on port 8081 to persist): Score ' + score + '%');
        }
    },

    renderClientDashboard(root, session) {
        root.innerHTML = `
      <div class="card">
        <h2>🏠 Book a Verified Local Specialist</h2>
        <p>All workers pass psychometric screening, timed 20-minute practical exams, and ID/address verification.</p>
        <div class="grid-2">
          <div class="mini-box">
            <strong>Car Wash (Tier 1 Outdoor) — R150</strong>
            <p>Vetted local detailer comes to your driveway. 4-digit start PIN included.</p>
            <button class="btn-primary" onclick="alert('Booking created in Escrow! 4-Digit Start PIN: 4829')">Book Car Wash</button>
          </div>
          <div class="mini-box">
            <strong>Dog Walking (Tier 3 High-Trust + Live GPS) — R200</strong>
            <p>Restricted to Tier 3 specialists with 25+ completed jobs, GPS breadcrumb tracking & geofence alerts.</p>
            <button class="btn-primary" onclick="alert('Tier 3 Specialist matched! Live GPS tracking & Geofence armed.')">Book Dog Walk</button>
          </div>
        </div>
      </div>
    `;
    },

    renderAdminControlsRegister(root) {
        root.innerHTML = `
      <div class="card">
        <h2>📋 KasiMarket Risk & Counter-Controls Register</h2>
        <table class="vetting-table">
          <thead>
            <tr>
              <th>Risk Scenario</th>
              <th>System Control</th>
              <th>Enforcement Layer</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>High-value pet or home theft (e.g. R15,000 dog on R200 walk)</td>
              <td>Tier 1–3 progression gates + live GPS breadcrumbs + neighborhood geofence alerts + SA ID/address lock</td>
              <td>Backend Tier Gate & GPS Telemetry</td>
              <td><span class="badge-trust">ACTIVE</span></td>
            </tr>
            <tr>
              <td>Unskilled worker damaging car paint, clothes, or appliances</td>
              <td>Mandatory 20-minute category competency exam (80% pass mark) + Academy SOP courses</td>
              <td>Assessment Engine</td>
              <td><span class="badge-trust">ACTIVE</span></td>
            </tr>
            <tr>
              <td>Erratic behavior or poor judgment in sensitive roles (babysitting)</td>
              <td>Psychometric & Situational Judgment Test (SJT) + Admin review queue</td>
              <td>Onboarding Gate</td>
              <td><span class="badge-trust">ACTIVE</span></td>
            </tr>
            <tr>
              <td>Client non-payment or unsafe fake address</td>
              <td>Upfront Escrow payment lock + verified client ID + one-tap SOS button</td>
              <td>Escrow & Safety Center</td>
              <td><span class="badge-trust">ACTIVE</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    `;
    }
};

window.addEventListener('DOMContentLoaded', () => Router.init());