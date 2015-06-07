package net.openright.mobile.texts;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class TextMessageServlet extends HttpServlet {
    private TreeMap<Instant, JSONObject> messages = new TreeMap<>();

    private List<AsyncContext> eventSources = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isEventSourceRequest(req)) {
            doStartEventSource(req, resp);
            return;
        }

        Instant since = Instant.MIN;
        if (req.getParameter("since") != null) {
            since = Instant.parse(req.getParameter("since"));
        }

        JSONObject result = new JSONObject()
                .put("messages", new JSONArray(new ArrayList<>(messages.tailMap(since, false).values())));
        resp.setContentType("text/json");
        try (Writer writer = resp.getWriter()) {
            result.write(writer);
        }
        resp.setStatus(200);
    }

    private void doStartEventSource(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.addHeader("Connection", "close");
        resp.flushBuffer();

        if (!messages.isEmpty()) {
            JSONObject initMessage = new JSONObject().put("will_send_messages_after", messages.lastKey());
            resp.getWriter().write("data: " + initMessage + "\n\n");
            resp.getWriter().flush();
        }

        eventSources.add(req.startAsync());
    }

    private boolean isEventSourceRequest(HttpServletRequest req) {
        return Collections.list(req.getHeaders("Accept")).indexOf("text/event-stream") != -1;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (BufferedReader reader = req.getReader()) {
            addMessage(new JSONObject(new JSONTokener(reader)));
        }
        resp.setStatus(200);
    }

    private void addMessage(JSONObject message) {
        messages.put(Instant.parse(message.getString("createdAt")), message);

        for (Iterator<AsyncContext> iterator = eventSources.iterator(); iterator.hasNext(); ) {
            AsyncContext eventSource = iterator.next();
            try {
                PrintWriter writer = eventSource.getResponse().getWriter();
                writer.write("data: " + message + "\r\n\r\n");
                writer.flush();
            } catch (IllegalStateException e) {
                System.err.println("Closing dead connection");
                iterator.remove();
            } catch (IOException e) {
                System.err.println("Closing dead connection");
                e.printStackTrace();
                iterator.remove();
                eventSource.complete();
            }
        }

    }
}
