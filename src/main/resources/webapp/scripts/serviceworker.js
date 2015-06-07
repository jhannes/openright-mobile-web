self.addEventListener('push', function(event) {
  "use strict";
  console.log("received a message", event);

  var notification = {
    body: "A message was received",
    icon: "../images/app-icon-192px.png",
    tag: "web-push-message"
  };

  return event.waitUntil(
      self.registration.showNotification("Title", notification));
});

self.addEventListener('notificationclick', function(event) {
  console.log("On notification click", event);
  event.notification.close();
  clients.openWindow("play-sound.html");
});
