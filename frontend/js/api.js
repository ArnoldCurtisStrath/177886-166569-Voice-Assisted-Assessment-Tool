/**
 * API client — talks to the Spring Boot backend.
 * Uses AuthStore for the Bearer token.
 */
var API = {
  // change this if the backend runs on a different port
  BASE: localStorage.getItem('voiceassess_api_base') || 'http://localhost:8080',

  /**
   * Low-level request — returns parsed JSON or throws an error.
   */
  async request(method, path, body) {
    var headers = {};

    // only set Content-Type when we're sending a body
    if (body) {
      headers['Content-Type'] = 'application/json';
    }

    // attach auth header if we have a token
    if (window.AuthStore && AuthStore.token) {
      headers['Authorization'] = 'Bearer ' + AuthStore.token;
    }

    var opts = {
      method: method,
      headers: headers,
      cache: 'no-store'
    };
    if (body) opts.body = JSON.stringify(body);

    var resp = await fetch(this.BASE + path, opts);

    // try to parse JSON even on error — the backend sends { error: "..." }
    var data;
    var parsed = true;
    try {
      data = await resp.json();
    } catch (e) {
      data = { error: 'Unexpected response from server' };
      parsed = false;
    }

    if (!resp.ok) {
      // expired/missing token: backend returns 401, or 403 with an empty body
      // from the security filter (ownership 403s always carry a JSON error)
      var sessionProblem = resp.status === 401 || (resp.status === 403 && !parsed);
      if (sessionProblem && window.AuthStore && AuthStore.token) {
        AuthStore.logout();
        if (window.Router) Router.navigate('login');
      }
      var msg = sessionProblem
        ? 'Your session has expired. Please log in again.'
        : (data.error || ('Request failed with status ' + resp.status));
      throw new Error(msg);
    }

    return data;
  },

  get(path) {
    return this.request('GET', path);
  },

  post(path, body) {
    return this.request('POST', path, body);
  },

  put(path, body) {
    return this.request('PUT', path, body);
  },

  del(path) {
    return this.request('DELETE', path);
  },

  /**
   * Multipart upload with progress callback.
   * Uses XHR because fetch doesn't support upload progress yet.
   * Optional 4th arg: onCancel(abortFn) lets the caller wire up a cancel button.
   */
  uploadFormData(path, formData, onProgress, onCancel) {
    var self = this;
    return new Promise(function(resolve, reject) {
      var xhr = new XMLHttpRequest();
      xhr.open('POST', self.BASE + path);
      // transcription + AI grading run inside this request — give it room
      xhr.timeout = 180000;
      if (window.AuthStore && AuthStore.token) {
        xhr.setRequestHeader('Authorization', 'Bearer ' + AuthStore.token);
      }

      xhr.upload.onprogress = function(e) {
        if (e.lengthComputable && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      };

      xhr.ontimeout = function() {
        reject(new Error('Upload timed out. Check your connection and try again.'));
      };

      xhr.onload = function() {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            resolve(JSON.parse(xhr.responseText));
          } catch (e) {
            resolve({ message: 'Upload complete' });
          }
        } else {
          var data;
          try { data = JSON.parse(xhr.responseText); } catch (e) { data = {}; }
          reject(new Error(data.error || ('Upload failed with status ' + xhr.status)));
        }
      };

      xhr.onerror = function() {
        reject(new Error('Network error during upload'));
      };

      xhr.onabort = function() {
        reject(new Error('Upload cancelled'));
      };

      if (onCancel) onCancel(function() { xhr.abort(); });

      xhr.send(formData);
    });
  },

  /**
   * Download a file from an authenticated endpoint.
   * GETs the URL with JWT header, then triggers a browser download.
   * Returns the promise so callers can show progress/finish feedback.
   */
  downloadFile(path, filename) {
    var headers = {};
    if (window.AuthStore && AuthStore.token) {
      headers['Authorization'] = 'Bearer ' + AuthStore.token;
    }
    return fetch(this.BASE + path, { headers: headers })
      .then(function(resp) {
        if (!resp.ok) throw new Error('Download failed: ' + resp.status);
        return resp.blob();
      })
      .then(function(blob) {
        var url = URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = filename || 'download';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      });
  }
};

window.API = API;
