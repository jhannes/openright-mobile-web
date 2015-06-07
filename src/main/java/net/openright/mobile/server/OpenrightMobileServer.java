package net.openright.mobile.server;

import net.openright.mobile.messages.MessageServlet;
import net.openright.infrastructure.util.IOUtil;
import net.openright.infrastructure.util.LogUtil;
import net.openright.mobile.texts.TextMessageServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class OpenrightMobileServer {

    private static final Logger log = LoggerFactory.getLogger(OpenrightMobileServer.class);

    private Server server;
    private OpenrightMobileConfig config;

    public OpenrightMobileServer(OpenrightMobileConfig config) {
        this.config = config;
        this.server = new Server(config.getHttpPort());
    }

    protected HandlerList createHandlers() {
        HandlerList handlers = new HandlerList();
        handlers.addHandler(new ShutdownHandler("dsgsdglsdgsdgnk", false, true));
        handlers.addHandler(createWebApi());
        handlers.addHandler(createWebApp());
        handlers.addHandler(new MovedContextHandler(null, "/", "/mobile"));
        return handlers;
    }

    private ServletContextHandler createWebApi() {
        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/mobile/api");
        Set<String> registrations = new HashSet<>();
        handler.addServlet(new ServletHolder(new MessageServlet(config, registrations)), "/messages");
        handler.addServlet(new ServletHolder(new SubscriberServlet(registrations)), "/subscribers");
        handler.addServlet(new ServletHolder(new TextMessageServlet()), "/text/messages");
        return handler;
    }

    protected WebAppContext createWebApp() {
        WebAppContext webAppContext = new WebAppContext(null, "/mobile");
        webAppContext.setBaseResource(Resource.newClassPathResource("webapp"));
        webAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        return webAppContext;
    }

    public static void main(String[] args) throws Exception {
        LogUtil.setupLogging("logging-openright-mobile.xml");
        OpenrightMobileConfig config = new OpenrightMobileConfigFile(IOUtil.extractResourceFile("openright-mobile.properties"));

        new OpenrightMobileServer(config).start();
    }

    public void start() throws Exception {
        server.setHandler(createHandlers());
        this.server.start();
        log.info("Started {}", getURI());
    }

    public URI getURI() {
        return server.getURI().resolve("/");
    }
}
