package net.openright.mobile.server;

import net.openright.infrastructure.server.AppConfigFile;

import java.io.File;

public class OpenrightMobileConfigFile extends AppConfigFile implements OpenrightMobileConfig {

    public OpenrightMobileConfigFile(File file) {
        super(file);
    }

    @Override
    public int getHttpPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return Integer.parseInt(getProperty("parental.http.port", "3000"));
    }

}
