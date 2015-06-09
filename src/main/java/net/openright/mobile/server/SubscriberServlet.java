package net.openright.mobile.server;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

public class SubscriberServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubscriberServlet.class);

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
        this.registrations.add(endpoint);
    }

    private String fromJSON(JSONObject jsonObject) {
        log.debug("{}", jsonObject);
        String endpoint = jsonObject.getString("endpoint");
        if (endpoint.startsWith(ENDPOINT_PREFIX)) {
            return endpoint.substring(ENDPOINT_PREFIX.length());
        } else {
            return jsonObject.getString("subscriptionId");
        }
    }
}
