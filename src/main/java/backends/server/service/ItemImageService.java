package backends.server.service;

import backends.common.messages.Common.ItemImagePayload;
import backends.common.messages.Common.Message;
import backends.server.database.InventoryDAO;
import backends.server.database.ItemImageDAO;
import backends.server.database.ItemImageDAO.ImageRecord;
import backends.server.handler.ClientHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

public final class ItemImageService {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final Gson gson = new Gson();

    private ItemImageService() {
    }

    public static String saveItemImage(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.path("payloadJson").asText("");
        ItemImagePayload payload = mapper.readValue(payloadJson, ItemImagePayload.class);
        if (payload.requestId == null || payload.requestId.isBlank()) {
            return fail("Missing request id");
        }
        if (payload.imageBase64 == null || payload.imageBase64.isBlank()) {
            return fail("Missing image data");
        }

        ItemImageDAO dao = new ItemImageDAO();
        String resolvedItemId = payload.itemId;
        if (resolvedItemId == null || resolvedItemId.isBlank()) {
            resolvedItemId = new InventoryDAO().getItemIdByRequestId(payload.requestId);
        }
        byte[] imageBytes = Base64.getDecoder().decode(payload.imageBase64);
        String extension = extractExtension(payload.imageFileName);
        String storedFileName = buildStoredFileName(payload.requestId, extension);
        Path storedPath = dao.getItemImageDirectory().resolve(storedFileName).normalize();
        Files.write(storedPath, imageBytes);

        String relativePath = Path.of("itemImage").resolve(storedFileName).toString().replace("\\", "/");
        dao.saveImage(payload.requestId, resolvedItemId, relativePath);

        ItemImagePayload responsePayload = new ItemImagePayload(
                payload.requestId,
                resolvedItemId,
                relativePath,
                storedFileName,
                payload.imageBase64
        );

        ObjectNode response = mapper.createObjectNode();
        response.put("type", "ITEM_IMAGE_SAVE_OK");
        response.put("payloadJson", gson.toJson(responsePayload));
        return response.toString();
    }

    public static String getItemImage(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.path("payloadJson").asText("");
        ItemImagePayload payload = mapper.readValue(payloadJson, ItemImagePayload.class);
        String itemId = payload.itemId;
        if (itemId == null || itemId.isBlank()) {
            return fail("Missing item id");
        }

        ItemImageDAO dao = new ItemImageDAO();
        Optional<ImageRecord> existing = dao.findByItemId(itemId);

        String requestId = "";
        String imagePath = "";
        String imageBase64 = "";
        String imageFileName = "";

        if (existing.isPresent()) {
            ImageRecord record = existing.get();
            requestId = record.requestId();
            imagePath = record.imagePath();
            Path resolvedPath = dao.resolveStoredPath(imagePath);
            if (Files.exists(resolvedPath)) {
                imageBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(resolvedPath));
                imageFileName = resolvedPath.getFileName().toString();
            }
        }

        ItemImagePayload responsePayload = new ItemImagePayload(
                requestId,
                itemId,
                imagePath,
                imageFileName,
                imageBase64
        );

        ObjectNode response = mapper.createObjectNode();
        response.put("type", "ITEM_IMAGE_OK");
        response.put("payloadJson", gson.toJson(responsePayload));
        return response.toString();
    }

    private static String fail(String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "ITEM_IMAGE_FAIL");
        response.put("message", message);
        return response.toString();
    }

    private static String buildStoredFileName(String requestId, String extension) {
        String safeExtension = extension.isBlank() ? ".png" : extension;
        return requestId + "_" + System.currentTimeMillis() + safeExtension;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return ".png";
        }

        String safeName = Path.of(fileName).getFileName().toString();
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == safeName.length() - 1) {
            return ".png";
        }
        return safeName.substring(dotIndex);
    }
}
