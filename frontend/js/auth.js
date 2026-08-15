var AuthStore = {
  // current state
  user: null,
  token: null,
  isLoggedIn: false,

  /**
   * Try to restore a previous session from localStorage.
   */
  init() {
    var saved = localStorage.getItem('voiceassess_auth');
    if (!saved) return;

    try {
      var parsed = JSON.parse(saved);
      if (parsed.user && parsed.token) {
        // drop expired sessions so the router doesn't let them into dashboards
        if (this._isExpired(parsed.token)) {
          localStorage.removeItem('voiceassess_auth');
          return;
        }
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
   * Check the JWT exp claim without trusting the saved user object.
   */
  _isExpired(token) {
    try {
      var parts = token.split('.');
      if (parts.length < 2) return true;
      // base64url -> base64 (pad it — atob chokes on unpadded strings)
      var b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      while (b64.length % 4 !== 0) b64 += '=';
      var payload = JSON.parse(atob(b64));
      if (!payload.exp) return true;
      return payload.exp * 1000 < Date.now();
    } catch (e) {
      // unreadable token — treat as expired
      return true;
    }
  },

  /**
   * Authenticate against the backend API.
   */
  login(email, password) {
    return API.post('/api/auth/login', {
      email: email,
      password: password
    }).then(function(data) {
      // data = { token, userId, fullName, email, role, schoolId }
      var user = {
        id: data.userId,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        schoolId: data.schoolId || null
      };

      AuthStore.user = user;
      AuthStore.token = data.token;
      AuthStore.isLoggedIn = true;

      // persist to localStorage
      localStorage.setItem('voiceassess_auth', JSON.stringify({ user: user, token: data.token }));

      return user;
    });
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
   * Merge updated fields into the stored session (used after profile edits).
   */
  updateUser(patch) {
    if (!this.user) return;
    this.user = Object.assign({}, this.user, patch);
    if (this.token) {
      localStorage.setItem('voiceassess_auth', JSON.stringify({ user: this.user, token: this.token }));
    }
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
   * Admin can access everything.
   */
  isAuthorized(requiredRole) {
    if (!this.isLoggedIn || !this.user) return false;
    // admin can see everything
    if (this.user.role === 'ADMIN') return true;
    return this.user.role === requiredRole;
  },

  /**
   * Get the dashboard route for the current user's role.
   */
  getDashboardRoute() {
    if (!this.user) return '#login';
    var map = {
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
