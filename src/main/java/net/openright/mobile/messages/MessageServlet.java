package net.openright.mobile.messages;

import net.openright.infrastructure.util.IOUtil;
import net.openright.infrastructure.util.JSONUtil;
import net.openright.mobile.server.OpenrightMobileConfig;
import org.json.JSONArray;
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
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MessageServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MessageServlet.class);

    private TreeMap<Instant, Message> messages = new TreeMap<>();
    private final Set<String> registrations;
    private final OpenrightMobileConfig config;

    public MessageServlet(OpenrightMobileConfig config, Set<String> registrations) {
        this.config = config;
        this.registrations = registrations;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JSONArray messages = new JSONArray(this.messages.descendingMap().values().stream()
                .limit(5)
                .map(Message::toJSON)
                .collect(Collectors.toList()));
        JSONObject result = new JSONObject()
                .put("messages", messages);
        resp.setContentType("text/json");
        try (Writer writer = resp.getWriter()) {
            result.write(writer);
        }
        resp.setStatus(200);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (BufferedReader reader = req.getReader()) {
            addMessage(Message.fromJSON(new JSONObject(new JSONTokener(reader))));
        }
        resp.setStatus(200);

        notifySubscribers();
    }

    private void notifySubscribers() throws IOException {
        JSONObject notification = new JSONObject();
        notification.put("registration_ids", new JSONArray(new ArrayList<>(registrations)));

        URL url = new URL("https://android.googleapis.com/gcm/send");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", "key=" + config.getGoogleApiKey());
        JSONUtil.copy(notification, urlConnection);
        log.info("{}", IOUtil.toString(urlConnection));
    }

    private Message addMessage(Message message) {
        return messages.put(message.getCreatedAt(), message);
    }

}
