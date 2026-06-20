const AuthStore = {
  // current state
  user: null,
  token: null,
  isLoggedIn: false,

  /**
   * Try to restore a previous session from localStorage.
   * Call this once when the app loads.
   */
  init() {
    const saved = localStorage.getItem('voiceassess_auth');
    if (!saved) return;

    try {
      const parsed = JSON.parse(saved);
      if (parsed.user && parsed.token) {
        this.user = parsed.user;
        this.token = parsed.token;
        this.isLoggedIn = true;
      }
    } catch (e) {
      // corrupted data — clear it
      localStorage.removeItem('voiceassess_auth');
    }
  },

  /**
   * Authenticate against mock user list.
   * In production, this would POST to /api/auth/login.
   */
  login(email, password) {
    const users = window.mockData ? window.mockData.users : [];
    const found = users.find(u =>
      u.email.toLowerCase() === email.toLowerCase() && u.password === password
    );

    if (!found) {
      return Promise.reject(new Error('Invalid email or password.'));
    }

    if (!found.isActive) {
      return Promise.reject(new Error('This account has been deactivated. Contact your school administrator.'));
    }

    // build the auth payload
    const user = {
      id: found.id,
      fullName: found.fullName,
      email: found.email,
      role: found.role
    };

    // fake token for development
    const token = 'dev-token-' + found.role.toLowerCase() + '-' + Date.now();

    this.user = user;
    this.token = token;
    this.isLoggedIn = true;

    // persist to localStorage
    localStorage.setItem('voiceassess_auth', JSON.stringify({ user, token }));

    return Promise.resolve(user);
  },

  /**
   * Clear session.
   */
  logout() {
    this.user = null;
    this.token = null;
    this.isLoggedIn = false;
    localStorage.removeItem('voiceassess_auth');
  },

  /**
   * Returns headers needed for authenticated API calls.
   */
  getAuthHeaders() {
    if (!this.token) return {};
    return { 'Authorization': 'Bearer ' + this.token };
  },

  /**
   * Check if the current user is authorized for a given role.
   * Admin can access everything. Other roles are restricted.
   */
  isAuthorized(requiredRole) {
    if (!this.isLoggedIn || !this.user) return false;
    // admin can see everything
    if (this.user.role === 'ADMIN') return true;
    // otherwise, must match the required role
    return this.user.role === requiredRole;
  },

  /**
   * Get the dashboard route for the current user's role.
   */
  getDashboardRoute() {
    if (!this.user) return '#login';
    const map = {
      'ADMIN': '#admin',
      'TEACHER': '#teacher',
      'PARENT': '#parent',
      'STUDENT': '#student'
    };
    return map[this.user.role] || '#login';
  }
};

// Initialize from localStorage immediately
AuthStore.init();

window.AuthStore = AuthStore;
