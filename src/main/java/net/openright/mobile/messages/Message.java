package net.openright.mobile.messages;

import org.json.JSONObject;

import java.time.Instant;

public class Message {
    private Instant createdAt = Instant.now();
    private String pictureDataUrl;
    private boolean hasLocation;
    private double longitude;
    private double latitude;

    public Instant getCreatedAt() {
        return createdAt;
    }

    static Message fromJSON(JSONObject jsonObject) {
        Message message = new Message();
        message.pictureDataUrl = jsonObject.getString("picture");
        message.hasLocation = jsonObject.getBoolean("location");
        message.longitude = jsonObject.optDouble("longitude");
        message.latitude = jsonObject.optDouble("latitude");
        return message;
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("createdAt", createdAt)
                .put("picture", pictureDataUrl);
    }
}
