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
    var headers = { 'Content-Type': 'application/json' };

    // attach auth header if we have a token
    if (window.AuthStore && AuthStore.token) {
      headers['Authorization'] = 'Bearer ' + AuthStore.token;
    }

    var opts = {
      method: method,
      headers: headers
    };
    if (body) opts.body = JSON.stringify(body);

    var resp = await fetch(this.BASE + path, opts);

    // try to parse JSON even on error — the backend sends { error: "..." }
    var data;
    try {
      data = await resp.json();
    } catch (e) {
      data = { error: 'Unexpected response from server' };
    }

    if (!resp.ok) {
      var msg = data.error || ('Request failed with status ' + resp.status);
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
   */
  uploadFormData(path, formData, onProgress) {
    var self = this;
    return new Promise(function(resolve, reject) {
      var xhr = new XMLHttpRequest();
      xhr.open('POST', self.BASE + path);
      if (window.AuthStore && AuthStore.token) {
        xhr.setRequestHeader('Authorization', 'Bearer ' + AuthStore.token);
      }

      xhr.upload.onprogress = function(e) {
        if (e.lengthComputable && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
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

      xhr.send(formData);
    });
  }
};

window.API = API;
