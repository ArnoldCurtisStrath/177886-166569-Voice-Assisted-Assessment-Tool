const Router = {
  // route definitions: hash -> config
  routes: {
    'login':        { title: 'VoiceAssess',     role: null,     file: 'pages/login.html' },
    'admin':        { title: 'Admin Dashboard', role: 'ADMIN',  file: 'pages/admin/dashboard.html' },
    'admin/users':  { title: 'User Management', role: 'ADMIN',  file: 'pages/admin/users.html' },
    'teacher':      { title: 'Dashboard',       role: 'TEACHER',file: 'pages/teacher/dashboard.html' },
    'teacher/curate': { title: 'New Assessment',role: 'TEACHER',file: 'pages/teacher/curation.html' },
    'teacher/record':{ title: 'Assessment Audio',role:'TEACHER',file: 'pages/teacher/recording.html' },
    'teacher/review':{ title: 'Pending Reviews',role: 'TEACHER',file: 'pages/teacher/review.html' },
    'parent':       { title: 'My Children',     role: 'PARENT', file: 'pages/parent/dashboard.html' },
    'parent/child': { title: 'Student Progress',role: 'PARENT', file: 'pages/parent/child-progress.html' },
    'student':      { title: 'My Progress',     role: 'STUDENT',file: 'pages/student/dashboard.html' }
  },

  currentHash: '',

  /**
   * Start the router — listen for hash changes and render the current route.
   */
  init() {
    window.addEventListener('hashchange', () => this._handleRoute());
    // also handle initial load (or if no hash, redirect to login)
    if (!window.location.hash || window.location.hash === '#') {
      this.navigate('login');
    } else {
      this._handleRoute();
    }
  },

  /**
   * Navigate to a hash route.
   */
  navigate(hash) {
    // strip leading # if present
    const clean = hash.replace(/^#/, '');
    window.location.hash = clean;
  },

  /**
   * Get the current hash (without the #).
   */
  _getHash() {
    return window.location.hash.replace(/^#/, '') || 'login';
  },

  /**
   * Handle a route change — check auth, load template, render.
   * The order of these checks matters: unknown route first, then
   * role auth, then login redirect, then not-logged-in guard.
   */
  async _handleRoute() {
    const hash = this._getHash();
    const route = this.routes[hash];

    // unknown route — go to login
    if (!route) {
      this.navigate('login');
      return;
    }

    // check auth for role-restricted pages
    if (route.role && !AuthStore.isAuthorized(route.role)) {
      // user not authorized for this page — redirect to their dashboard
      this.navigate(AuthStore.getDashboardRoute());
      return;
    }

    // if logged in but trying to access login page, redirect to dashboard
    if (hash === 'login' && AuthStore.isLoggedIn) {
      this.navigate(AuthStore.getDashboardRoute());
      return;
    }

    // if not logged in and trying to access a restricted page, redirect to login
    if (route.role && !AuthStore.isLoggedIn) {
      this.navigate('login');
      return;
    }

    this.currentHash = hash;
    document.title = route.title + ' — VoiceAssess';

    // load and render the page template
    const container = document.getElementById('page-content');
    if (!container) return;

    try {
      const html = await this._fetchTemplate(route.file);
      container.innerHTML = html;

      // innerHTML doesn't execute <script> tags — eval each one manually
      var scripts = container.querySelectorAll('script');
      scripts.forEach(function(s) {
        if (s.textContent) {
          try { eval(s.textContent); } catch (e) { console.error('Page script error:', e); }
        }
      });

      // fade in the new content
      container.classList.add('animate-fade-in');
      setTimeout(() => container.classList.remove('animate-fade-in'), 300);

      // update navigation active states
      this._updateNav(hash);
      this._updateLayout(route);
    } catch (err) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">!</div>
          <h4>Page failed to load</h4>
          <p>Could not load ${route.file}. Check that the file exists.</p>
        </div>`;
    }
  },

  /**
   * Fetch an HTML template file and return its content as a string.
   */
  async _fetchTemplate(filePath) {
    const resp = await fetch(filePath);
    if (!resp.ok) throw new Error('Failed to load template: ' + filePath);
    return resp.text();
  },

  /**
   * Update sidebar and bottom-nav active states based on current hash.
   */
  _updateNav(hash) {
    // sidebar links
    document.querySelectorAll('.sidebar-link').forEach(link => {
      const linkHash = link.getAttribute('data-route');
      link.classList.toggle('active', linkHash === hash);
    });

    // bottom nav items
    document.querySelectorAll('.bottom-nav-item').forEach(item => {
      const itemHash = item.getAttribute('data-route');
      item.classList.toggle('active', itemHash === hash);
    });
  },

  /**
   * Show/hide layout elements based on the route.
   * Login page hides all navigation chrome.
   */
  _updateLayout(route) {
    const topbar = document.getElementById('topbar');
    const sidebarEl = document.getElementById('sidebar');
    const bottomNav = document.getElementById('bottom-nav');

    if (!route.role) {
      // login page — hide all navigation
      if (topbar) topbar.style.display = 'none';
      if (sidebarEl) sidebarEl.style.display = 'none';
      if (bottomNav) bottomNav.style.display = 'none';
    } else {
      // authenticated page — show navigation
      if (topbar) topbar.style.display = '';
      if (sidebarEl) sidebarEl.style.display = '';
      if (bottomNav) bottomNav.style.display = '';

      // render role-specific sidebar links
      this._renderSidebarNav();
      // render role-specific bottom nav
      this._renderBottomNav();
    }
  },

  /**
   * Populate sidebar with role-appropriate navigation links.
   */
  _renderSidebarNav() {
    const navContainer = document.getElementById('sidebar-nav');
    if (!navContainer) return;

    const role = AuthStore.user ? AuthStore.user.role : 'TEACHER';

    // each role gets its own set of sidebar links — icons are plain chars
    // since we're not using an icon library (just text symbols for now)
    const navConfig = {
      // 8 admin pages — full system management
      ADMIN: [
        { route: 'admin', label: 'Dashboard', icon: '=' },
        { route: 'admin/users', label: 'Users', icon: '#' },
        { route: 'admin/classes', label: 'Classes', icon: '[' },
        { route: 'admin/subjects', label: 'Subjects', icon: 'S' },
        { route: 'admin/rubrics', label: 'Rubrics', icon: 'R' },
        { route: 'admin/terms', label: 'Terms', icon: 'T' },
        { route: 'admin/compliance', label: 'Compliance', icon: '%' },
        { route: 'admin/logs', label: 'Error Logs', icon: '!' }
      ],

      // 5 teacher pages — assessment workflow
      TEACHER: [
        { route: 'teacher', label: 'Dashboard', icon: '=' },
        { route: 'teacher/curate', label: 'New Assessment', icon: '+' },
        { route: 'teacher/review', label: 'Pending Reviews', icon: '?' },
        { route: 'teacher/class', label: 'Class Grid', icon: '[' },
        { route: 'teacher/appeals', label: 'Appeals', icon: '!' }
      ],

      // 2 parent pages — child progress tracking
      PARENT: [
        { route: 'parent', label: 'My Children', icon: '#' },
        { route: 'parent/child', label: 'Progress', icon: '%' }
      ],

      // 2 student pages — own progress + appeals
      STUDENT: [
        { route: 'student', label: 'My Progress', icon: '%' },
        { route: 'student/appeal', label: 'Appeal', icon: '!' }
      ]
    };

    // build sidebar links from config — mark current hash as active
    const links = navConfig[role] || [];
    navContainer.innerHTML = links.map(l => `
      <a class="sidebar-link ${l.route === this.currentHash ? 'active' : ''}"
         data-route="${l.route}"
         href="#${l.route}">
        <span class="sidebar-link-icon">${l.icon}</span>
        ${l.label}
      </a>
    `).join('');

    // also update the sidebar user info section (name, role, avatar initials)
    const userNameEl = document.getElementById('sidebar-user-name');
    const userRoleEl = document.getElementById('sidebar-user-role');
    const userAvatarEl = document.getElementById('sidebar-avatar');
    if (userNameEl && AuthStore.user) {
      userNameEl.textContent = AuthStore.user.fullName;
    }
    if (userRoleEl && AuthStore.user) {
      userRoleEl.textContent = AuthStore.user.role;
    }
    if (userAvatarEl && AuthStore.user) {
      const names = AuthStore.user.fullName.split(' ');
      const initials = names.map(n => n.charAt(0)).join('').slice(0, 2).toUpperCase();
      userAvatarEl.textContent = initials;
    }
  },

  /**
   * Populate bottom nav with 3-4 role-specific tabs.
   */
  _renderBottomNav() {
    const navContainer = document.getElementById('bottom-nav');
    if (!navContainer) return;

    // same structure as sidebar nav but fewer items — mobile gets 3-4 shortcuts
    const role = AuthStore.user ? AuthStore.user.role : 'TEACHER';

    const navConfig = {
      // admin bottom nav — 4 most-used pages
      ADMIN: [
        { route: 'admin', icon: '=', label: 'Home' },
        { route: 'admin/users', icon: '#', label: 'Users' },
        { route: 'admin/classes', icon: '[', label: 'Classes' },
        { route: 'admin/subjects', icon: 'S', label: 'Subjects' }
      ],
      TEACHER: [
        { route: 'teacher', icon: '=', label: 'Home' },
        { route: 'teacher/curate', icon: '+', label: 'Assess' },
        { route: 'teacher/review', icon: '?', label: 'Review' },
        { route: 'teacher/class', icon: '[', label: 'Grid' }
      ],
      PARENT: [
        { route: 'parent', icon: '#', label: 'Children' },
        { route: 'parent/child', icon: '%', label: 'Progress' }
      ],
      STUDENT: [
        { route: 'student', icon: '%', label: 'Progress' },
        { route: 'student/appeal', icon: '!', label: 'Appeal' }
      ]
    };

    const tabs = navConfig[role] || [];
    navContainer.innerHTML = tabs.map(t => `
      <a class="bottom-nav-item ${t.route === this.currentHash ? 'active' : ''}"
         data-route="${t.route}"
         href="#${t.route}">
        <span class="bottom-nav-icon">${t.icon}</span>
        ${t.label}
      </a>
    `).join('');
  }
};

window.Router = Router;
