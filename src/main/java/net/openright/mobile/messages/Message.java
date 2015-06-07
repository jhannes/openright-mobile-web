package net.openright.mobile.messages;

import org.json.JSONObject;

import java.time.Instant;

public class Message {
    private Instant createdAt = Instant.now();
    private String pictureDataUrl;
    private boolean hasLocation;
    private Double longitude;
    private Double latitude;
    private String avatarDataUrl;
    private String name;

    public Instant getCreatedAt() {
        return createdAt;
    }

    static Message fromJSON(JSONObject jsonObject) {
        Message message = new Message();
        message.name = jsonObject.optString("name");
        message.avatarDataUrl = jsonObject.optString("avatar");
        message.pictureDataUrl = jsonObject.getString("picture");
        message.hasLocation = jsonObject.getBoolean("location");
        message.longitude = (Double) jsonObject.opt("longitude");
        message.latitude = (Double) jsonObject.opt("latitude");
        return message;
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("name", name)
                .put("avatar", avatarDataUrl)
                .put("longitude", longitude)
                .put("latitude", latitude)
                .put("createdAt", createdAt)
                .put("picture", pictureDataUrl);
    }
}
