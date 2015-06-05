package net.openright.infrastructure.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class IOUtil {
    public static File extractResourceFile(String filename) {
        File file = new File(filename);
        if (file.exists())
            return file;

        try (InputStream input = IOUtil.class.getResourceAsStream("/" + filename)) {
            if (input == null) {
                throw new IllegalArgumentException("Can't find /" + filename + " in classpath");
            }

            copy(input, file);
            return file;
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    public static Optional<String> toString(URI uri) {
        try {
            return toString(uri.toURL());
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    public static Optional<String> toString(URL url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() == 204) {
                return Optional.empty();
            }
            try (Reader content = getReader(conn)) {
                return Optional.of(toString(content));
            }
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    private static Reader getReader(HttpURLConnection conn) throws IOException {
        String contentType = conn.getHeaderField("Content-Type");
        if (contentType != null && contentType.contains("; charset=")) {
            String charset = contentType.substring(contentType.indexOf("; charset=") + "; charset=".length());
            return new InputStreamReader(conn.getInputStream(), charset);
        }
        return new InputStreamReader(conn.getInputStream());
    }

    public static String toString(InputStream content) {
        return toString(new InputStreamReader(content));
    }

    public static String toString(Reader reader) {
        StringWriter writer = new StringWriter();
        copy(reader, writer);
        return writer.toString();
    }

    public static void copy(Reader in, Writer out) {
        try {
            char[] buf = new char[1024];
            int count;
            while ((count = in.read(buf)) >= 0) {
                out.write(buf, 0, count);
            }
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    public static File copy(URL url, File file) {
        if (file.isDirectory()) {
            file = new File(file, new File(url.getPath()).getName());
        }
        try (InputStream content = (InputStream) url.getContent()) {
            copy(content, file);
            return file;
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }

    }

    public static void copy(File source, File target) {
        try (FileInputStream input = new FileInputStream(source)) {
            copy(input, target);
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    public static void copy(InputStream content, File file) {
        try (FileOutputStream output = new FileOutputStream(file)) {
            copy(content, output);
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    public static void copy(InputStream in, OutputStream out) {
        try {
            byte[] buf = new byte[1024];
            int count;
            while ((count = in.read(buf)) >= 0) {
                out.write(buf, 0, count);
            }
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }

    public static File extractZipEntry(File zippedFile, File entry) {
        try (ZipFile zipFile = new ZipFile(zippedFile)) {
            ZipEntry zipEntry = zipFile.getEntry(entry.getName());
            IOUtil.copy(zipFile.getInputStream(zipEntry), entry);
            return entry;
        } catch (IOException e) {
            throw ExceptionUtil.soften(e);
        }
    }
}
