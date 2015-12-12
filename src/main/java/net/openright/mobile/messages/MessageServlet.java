package net.openright.mobile.messages;

import net.openright.infrastructure.util.IOUtil;
import net.openright.infrastructure.util.JSONUtil;
import net.openright.mobile.server.OpenrightMobileConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(MessageServlet.class);

    private final TreeMap<Instant, Message> messages = new TreeMap<>();
    private final Set<String> registrations;
    private final OpenrightMobileConfig config;
    private final Set<AsyncContext> eventSources = new HashSet<>();
    private ExecutorService eventSourceNotifier;

    public MessageServlet(OpenrightMobileConfig config, Set<String> registrations) {
        this.config = config;
        this.registrations = registrations;
    }

    @Override
    public void init() throws ServletException {
        eventSourceNotifier = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("{} {} {}", req.getMethod(), req.getRequestURI(), req.getQueryString());
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isEventSourceRequest(req)) {
            doGetEventSource(req, resp);
        } else {
            doGetJson(req, resp);
        }
    }

    private boolean isEventSourceRequest(HttpServletRequest req) {
        return Collections.list(req.getHeaders("Accept")).indexOf("text/event-stream") != -1;
    }

    private void doGetJson(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONArray messages = new JSONArray(streamMessages(req.getParameter("lastSeen"), req.getParameter("lastBeforeStream"))
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

    private Stream<Message> streamMessages(String lastSeen, String lastBeforeStream) {
        return streamMessages(parseInstant(lastSeen), parseInstant(lastBeforeStream));
    }

    private Stream<Message> streamMessages(Instant lastSeen, Instant lastBeforeStream) {
        if (Objects.equals(lastSeen, lastBeforeStream)) {
            return Stream.empty();
        }
        NavigableMap<Instant, Message> messages = this.messages;
        if (lastSeen != null) messages = messages.tailMap(lastSeen, false);
        if (lastSeen != null) messages = messages.headMap(lastBeforeStream, true);
        return messages.values().stream();
    }

    private Instant parseInstant(String string) {
        return string != null ? Instant.parse(string) : null;
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Message message;
        try (BufferedReader reader = req.getReader()) {
            message = Message.fromJSON(new JSONObject(new JSONTokener(reader)));
        }
        resp.setStatus(200);

        addMessage(message);
        notifySubscribers();
        eventSourceNotifier.submit(() -> notifyEventSources(message));
    }

    private void notifyEventSources(Message message) {
        log.info("Notifying eventSources {}", eventSources);
        for (AsyncContext eventSource : new ArrayList<>(eventSources)) {
            try {
                PrintWriter writer = eventSource.getResponse().getWriter();
                writer.write("id: " + message.getCreatedAt().toString() + "\r\n");
                writer.write("data: " + message.toJSON() + "\r\n\r\n");
                log.debug("Flushing {}", eventSource);
                writer.flush();
            } catch (IllegalStateException e) {
                log.warn("Closing dead connection");
                eventSources.remove(eventSource);
            } catch (IOException e) {
                log.error("Closing dead connection", e);
                eventSources.remove(eventSource);
                eventSource.complete();
            }
        }
    }

    private void doGetEventSource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.addHeader("Connection", "close");

        PrintWriter writer = resp.getWriter();
        if (!messages.isEmpty()) {
            JSONObject initMessage = new JSONObject().put("last_seen", messages.lastKey());
            writer.write("event: streamStarting\n");
            writer.write("data: " + initMessage + "\n\n");
        } else {
            writer.write("event: emptyDatabase\n");
            writer.write("data: {}\n\n");
        }
        resp.flushBuffer();

        final AsyncContext async = req.startAsync();
        async.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                log.debug("onComplete: {}", event);
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                log.debug("onTimeout: {}", event);
                eventSources.remove(async);
                async.complete();
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log.debug("onError: {}", event);
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                log.debug("onStartAsync: {}", event);
            }
        });
        eventSources.add(async);
    }

    private void notifySubscribers() throws IOException {
        JSONObject notification = new JSONObject();
        notification.put("registration_ids", new JSONArray(new ArrayList<>(registrations)));

        URL url = new URL("https://android.googleapis.com/gcm/send");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", "key=" + config.getGoogleApiKey());
        log.info("{}", notification);
        JSONUtil.copy(notification, urlConnection);
        log.info("{}", IOUtil.toString(urlConnection));
    }

    private Message addMessage(Message message) {
        return messages.put(message.getCreatedAt(), message);
    }

}
