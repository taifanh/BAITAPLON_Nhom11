package com.bidding_system.backends.client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ViewLoader {
    private static final String[] RESOURCE_PREFIXES = {
            "/views/",
            "/views/user/",
            "/views/admin/"
    };
    private static final Path[] FALLBACK_VIEW_DIRECTORIES = {
            Path.of("src", "main", "java", "com", "Bidding System", "src/main/resources", "views"),
            Path.of("src", "main", "java", "com", "Bidding System", "src/main/resources", "views", "user"),
            Path.of("src", "main", "java", "com", "Bidding System", "src/main/resources", "views", "admin")
    };

    private ViewLoader() {
    }

    public static FXMLLoader loader(String viewFileName) throws IOException {
        for (String prefix : RESOURCE_PREFIXES) {
            URL resourceUrl = ViewLoader.class.getResource(prefix + viewFileName);
            if (resourceUrl != null) {
                return new FXMLLoader(resourceUrl);
            }
        }

        for (Path directory : FALLBACK_VIEW_DIRECTORIES) {
            Path fallbackPath = directory.resolve(viewFileName);
            if (Files.exists(fallbackPath)) {
                return new FXMLLoader(fallbackPath.toUri().toURL());
            }
        }

        throw new IOException("Khong tim thay view: " + viewFileName);
    }

    public static Parent load(String viewFileName) throws IOException {
        return loader(viewFileName).load();
    }
}
