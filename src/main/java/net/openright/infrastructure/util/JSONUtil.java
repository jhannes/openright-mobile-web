package net.openright.infrastructure.util;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;

public class JSONUtil {

    public static void copy(JSONObject jsonObject, HttpURLConnection urlConnection) throws IOException {
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-type", "application/json");
        urlConnection.setDoOutput(true);
        try (Writer writer = new OutputStreamWriter(urlConnection.getOutputStream())) {
            jsonObject.write(writer);
        }
    }
}
