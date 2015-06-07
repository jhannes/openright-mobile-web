package net.openright.mobile.server;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

public class SubscriberServlet extends HttpServlet {

    private static final String ENDPOINT_PREFIX = "https://android.googleapis.com/gcm/send/";
    private Set<String> registrations;

    public SubscriberServlet(Set<String> registrations) {
        this.registrations = registrations;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (BufferedReader reader = req.getReader()) {
            addRegistration(fromJSON(new JSONObject(new JSONTokener(reader))));
        }
        resp.setStatus(200);
    }

    private void addRegistration(String endpoint) {
        if (endpoint.startsWith(ENDPOINT_PREFIX)) {
            this.registrations.add(endpoint.substring(ENDPOINT_PREFIX.length()));
        } else {
            this.registrations.add(endpoint);
        }
    }

    private String fromJSON(JSONObject jsonObject) {
        return jsonObject.getString("endpoint");
    }
}
