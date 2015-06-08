self.addEventListener('push', function(event) {
  "use strict";
  console.log("received a message", event);

  var notification = {
    body: "A message was received",
    icon: "../images/app-icon-192px.png",
    tag: "web-push-message"
  };

  return event.waitUntil(
      self.registration.showNotification("Openright Mobile", notification));
});

self.addEventListener('notificationclick', function(event) {
  console.log("On notification click", event);
  event.notification.close();

  event.waitUntil(clients.matchAll({type:"window"}).then(function(clientList) {
    if (clientList.length) {
      clientList[0].url = "/mobile/#chats";
      return clientList[0].focus();
    } else {
      return clients.openWindow("/mobile/#chats");
    }
  }));
});
