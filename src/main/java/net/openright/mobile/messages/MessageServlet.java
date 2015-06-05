package net.openright.mobile.messages;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MessageServlet extends HttpServlet {

    private TreeMap<Instant, Message> messages = new TreeMap<>();

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
    }

    private Message addMessage(Message message) {
        return messages.put(message.getCreatedAt(), message);
    }

}
