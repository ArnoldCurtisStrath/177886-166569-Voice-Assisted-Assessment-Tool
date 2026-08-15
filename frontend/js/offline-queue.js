/**
 * Simple IndexedDB-backed offline queue for audio uploads.
 * If the network drops during upload, the blob gets saved here
 * and retried when the browser comes back online.
 */
var OfflineQueue = {
  DB_NAME: 'VoiceAssessOffline',
  DB_VERSION: 1,
  STORE: 'uploads',
  _db: null,

  /**
   * Open (or create) the database and object store.
   * Call once on app startup. Returns a promise.
   */
  init: function() {
    var self = this;
    if (!window.indexedDB) {
      console.warn('IndexedDB not available — offline queue disabled');
      return Promise.resolve();
    }

    return new Promise(function(resolve, reject) {
      var req = indexedDB.open(self.DB_NAME, self.DB_VERSION);
      req.onupgradeneeded = function(e) {
        var db = e.target.result;
        if (!db.objectStoreNames.contains(self.STORE)) {
          var store = db.createObjectStore(self.STORE, { keyPath: 'id', autoIncrement: true });
          store.createIndex('status', 'status', { unique: false });
          store.createIndex('createdAt', 'createdAt', { unique: false });
        }
      };
      req.onsuccess = function(e) {
        self._db = e.target.result;
        resolve();
      };
      req.onerror = function(e) {
        console.warn('IndexedDB open failed:', e.target.error);
        reject(e.target.error);
      };
    });
  },

  /**
   * Save an audio blob to the queue for later upload.
   * Returns the queue item id.
   */
  enqueue: function(audioBlob, assessmentId, fileName) {
    var self = this;
    if (!this._db) return Promise.reject(new Error('DB not initialized'));

    return new Promise(function(resolve, reject) {
      // read blob as ArrayBuffer so IndexedDB can store it
      var reader = new FileReader();
      reader.onload = function() {
        var item = {
          assessmentId: assessmentId,
          fileName: fileName || 'recording.webm',
          blobData: reader.result,
          mimeType: audioBlob.type || 'audio/webm',
          status: 'queued',
          createdAt: Date.now(),
          retryCount: 0
        };

        var txn = self._db.transaction([self.STORE], 'readwrite');
        var store = txn.objectStore(self.STORE);
        var addReq = store.add(item);
        addReq.onsuccess = function() {
          resolve(addReq.result);
        };
        addReq.onerror = function(e) {
          reject(e.target.error);
        };
      };
      reader.onerror = function() {
        reject(new Error('Failed to read audio blob'));
      };
      reader.readAsArrayBuffer(audioBlob);
    });
  },

  /**
   * Returns all items that still need attention:
   * queued, failed, or stuck in 'uploading' for over 10 minutes
   * (a page that died mid-upload leaves its item in that state).
   */
  getPending: function() {
    var self = this;
    if (!this._db) return Promise.resolve([]);

    return new Promise(function(resolve, reject) {
      var txn = self._db.transaction([self.STORE], 'readonly');
      var store = txn.objectStore(self.STORE);
      var results = [];
      var staleAfter = Date.now() - 10 * 60 * 1000;
      var cursorReq = store.openCursor();
      cursorReq.onsuccess = function(e) {
        var cursor = e.target.result;
        if (cursor) {
          var item = cursor.value;
          if (item.status === 'queued' || item.status === 'failed' ||
              (item.status === 'uploading' && item.createdAt < staleAfter)) {
            results.push(item);
          }
          cursor.continue();
        } else {
          resolve(results);
        }
      };
      cursorReq.onerror = function(e) {
        reject(e.target.error);
      };
    });
  },

  /**
   * Remove an item entirely (e.g. the user cancels an upload).
   */
  remove: function(id) {
    var self = this;
    if (!this._db) return Promise.reject(new Error('DB not initialized'));

    return new Promise(function(resolve, reject) {
      var txn = self._db.transaction([self.STORE], 'readwrite');
      var store = txn.objectStore(self.STORE);
      var req = store.delete(id);
      req.onsuccess = function() { resolve(); };
      req.onerror = function(e) { reject(e.target.error); };
    });
  },

  /**
   * Update the status of a queue item.
   */
  updateStatus: function(id, status) {
    var self = this;
    if (!this._db) return Promise.reject(new Error('DB not initialized'));

    return new Promise(function(resolve, reject) {
      var txn = self._db.transaction([self.STORE], 'readwrite');
      var store = txn.objectStore(self.STORE);
      var getReq = store.get(id);
      getReq.onsuccess = function() {
        var item = getReq.result;
        if (!item) { reject(new Error('Item not found')); return; }
        item.status = status;
        var putReq = store.put(item);
        putReq.onsuccess = function() { resolve(); };
        putReq.onerror = function(e) { reject(e.target.error); };
      };
      getReq.onerror = function(e) { reject(e.target.error); };
    });
  },

  /**
   * Increment the retry counter for a queue item.
   */
  incrementRetry: function(id) {
    var self = this;
    if (!this._db) return Promise.reject(new Error('DB not initialized'));

    return new Promise(function(resolve, reject) {
      var txn = self._db.transaction([self.STORE], 'readwrite');
      var store = txn.objectStore(self.STORE);
      var getReq = store.get(id);
      getReq.onsuccess = function() {
        var item = getReq.result;
        if (!item) { reject(new Error('Item not found')); return; }
        item.retryCount = (item.retryCount || 0) + 1;
        var putReq = store.put(item);
        putReq.onsuccess = function() { resolve(); };
        putReq.onerror = function(e) { reject(e.target.error); };
      };
      getReq.onerror = function(e) { reject(e.target.error); };
    });
  },

  /**
   * Delete all completed items to free up space.
   */
  removeCompleted: function() {
    var self = this;
    if (!this._db) return Promise.resolve();

    return new Promise(function(resolve, reject) {
      var txn = self._db.transaction([self.STORE], 'readwrite');
      var store = txn.objectStore(self.STORE);
      var cursorReq = store.openCursor();
      cursorReq.onsuccess = function(e) {
        var cursor = e.target.result;
        if (cursor) {
          if (cursor.value.status === 'completed') {
            cursor.delete();
          }
          cursor.continue();
        } else {
          resolve();
        }
      };
      cursorReq.onerror = function(e) { reject(e.target.error); };
    });
  },

  /**
   * Get count of pending items for badge display.
   */
  pendingCount: function() {
    return this.getPending().then(function(items) { return items.length; });
  }
};

window.OfflineQueue = OfflineQueue;
