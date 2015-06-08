function startMessageController(messageDb, replaceMessages, appendMessage) {
  function refreshMessages(messageDb) {
    messageDb.list().then(function(list) {
      replaceMessages(list);
    });
  }

  function fetchMissingMessages(lastBeforeStream) {
    messageDb.lastCreatedTimestamp().then(function (lastSeen) {
      var url = "api/messages?lastBeforeStream=" + lastBeforeStream;
      if (lastSeen) url += "&lastSeen=" + lastSeen;
      return fetch(url);
    }).then(function (result) {
      return result.json();
    }).then(function (json) {
      json.messages.forEach(function (message) {
        messageDb.save(message);
      });
      return json.messages.length > 0;
    }).then(function (hasMessages) {
      refreshMessages(messageDb);
    }).catch(function (e) {
      refreshMessages(messageDb);
      console.error("fetch failed", e);
    });
  }

  var messageEvents = new EventSource("api/messages");
  messageEvents.onmessage = function(event) {
    var data = JSON.parse(event.data);
    if (data.picture) {
      console.log("on message", data);
      messageDb.save(data);
      appendMessage(data);
    }
    if (data.last_seen) {
      fetchMissingMessages(data.last_seen);
    }
  };
  messageEvents.onerror = function() {
    refreshMessages(messageDb);
  };
  messageEvents.addEventListener("streamStarting", function(event) {
    fetchMissingMessages(JSON.parse(event.data).last_seen, messageDb);
  });
}

function createDatabase() {
  return new Promise(function(resolve, reject) {
    var openRequest = indexedDB.open("messages", 1);
    openRequest.onupgradeneeded = function(e) {
      var db = openRequest.result;
      if (e.oldVersion < 1) {
        var store = db.createObjectStore("messages", { keyPath: "id", autoIncrement: true });
        store.createIndex("by_createdAt", "createdAt", {unique: true});
      }
    };
    openRequest.onsuccess = function() {
      resolve(openRequest.result);
    };
    openRequest.onerror = reject;
  });
}


function messageDatabase(db) {
  function readonlyStore() {
    return db.transaction(["messages"], "readonly").objectStore("messages").index("by_createdAt");
  }
  function lastCreatedTimestamp() {
    return new Promise(function(resolve, reject) {
      var cursor = readonlyStore().openCursor(null, 'prev');
      cursor.onsuccess = function (e) {
        resolve(e.target.result ? e.target.result.key : null);
      };
      cursor.onerror = reject;
    });
  }
  function list() {
    return new Promise(function(resolve, reject) {
      var cursor = readonlyStore().openCursor();
      var result = [];
      cursor.onsuccess = function(e) {
        if (e.target.result) {
          result.push(e.target.result.value);
          e.target.result.continue();
        } else {
          resolve(result);
        }
      };
      cursor.onerror = reject;
    });
  }
  function save(message) {
    var transaction = db.transaction(["messages"],"readwrite");
    var store = transaction.objectStore("messages");
    store.put(message);
  }

  return {
    lastCreatedTimestamp: lastCreatedTimestamp,
    list: list,
    save: save
  };
}
