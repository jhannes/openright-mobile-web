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


function sendMessage() {
  var message = {};
  message.createdAt = new Date().toISOString();
  message.text = document.querySelector("#message__text").value;
  if (!message.text) {
    alert("Please type a message");
    return;
  }

  fetch("../api/text/messages", {
    method: 'post',
    headers: {'Content-type': 'text/json'},
    body: JSON.stringify(message)
  }).then(function () {
    console.log("message stored");
  }).catch(function (error) {
    console.log("error", error);
  });
}

function showMessage(messages, message) {
  var messageDisplay = document.createElement("p");
  messageDisplay.textContent = message.text;
  messages.appendChild(messageDisplay);
}

var openRequest = indexedDB.open("text_messages", 1);
openRequest.onupgradeneeded = function(e) {
  var db = openRequest.result;
  if (e.oldVersion < 1) {
    var store = db.createObjectStore("messages", { keyPath: "id", autoIncrement: true });
    store.createIndex("by_createdAt", "createdAt");
  }
};
openRequest.onsuccess = function() {
  fetchMessages(messageDatabase(openRequest.result));
};

function startEventListener(messageDb, messages) {
  var eventSource = new EventSource("../api/text/messages");
  eventSource.onmessage = function (event) {
    var data = JSON.parse(event.data);
    if (data.text) {
      messageDb.save(data);
      showMessage(messages, data);
    }
  };
}
function fetchMessages(messageDb) {
  var messages = document.querySelector("#messages");
  messages.innerHTML = "";

  messageDb.lastCreatedTimestamp().then(function(timestamp) {
    var url = "../api/text/messages";
    if (timestamp) {
      url += "?since=" + timestamp;
    }
    return fetch(url);
  }).then(function(result) {
    return result.json();
  }).then(function(json) {
    json.messages.forEach(function (message) {
      messageDb.save(message);
    });
  }).then(function() {
    startEventListener(messageDb, messages);
    displayMessages(messageDb, messages);
  }).catch(function(e) {
    startEventListener(messageDb, messages);
    displayMessages(messageDb, messages);
    console.error("fetch failed", e);
  });
}

function displayMessages(messageDb, messages) {
  messages.innerHTML = "";

  messageDb.list().then(function(messageList) {
    messageList.forEach(function(message) {
      showMessage(messages, message);
    })
  });
}
