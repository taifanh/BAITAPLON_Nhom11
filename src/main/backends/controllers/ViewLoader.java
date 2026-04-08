package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ViewLoader {
    private static final String VIEW_RESOURCE_PREFIX = "/org/example/views/";
    private static final Path FALLBACK_VIEW_DIRECTORY = Path.of("src", "main", "frontends", "org", "example", "views");

    private ViewLoader() {
    }

    public static FXMLLoader loader(String viewFileName) throws IOException {
        URL resourceUrl = ViewLoader.class.getResource(VIEW_RESOURCE_PREFIX + viewFileName);
        if (resourceUrl != null) {
            return new FXMLLoader(resourceUrl);
        }

        Path fallbackPath = FALLBACK_VIEW_DIRECTORY.resolve(viewFileName);
        if (Files.notExists(fallbackPath)) {
            throw new IOException("Khong tim thay view: " + viewFileName);
        }

        return new FXMLLoader(fallbackPath.toUri().toURL());
    }

    public static Parent load(String viewFileName) throws IOException {
        return loader(viewFileName).load();
    }
}
