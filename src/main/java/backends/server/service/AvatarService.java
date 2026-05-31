package backends.server.service;

import backends.common.messages.Common.AvatarPayload;
import backends.common.messages.Common.Message;
import backends.server.database.AvatarDAO;
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

public class AvatarService {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final Gson gson = new Gson();

    public static String saveAvatar(ClientHandler handler, JsonNode node) throws Exception {
        String payloadJson = node.path("payloadJson").asText("");
        AvatarPayload payload = mapper.readValue(payloadJson, AvatarPayload.class);
        AvatarDAO avatarDAO = new AvatarDAO();
        // Client gửi Base64, server decode ra bytes rồi ghi file vào data/avatars.
        byte[] avatarBytes = decodeAvatarBytes(payload.imageBase64);
        String extension = extractExtension(payload.avatarFileName);
        String storedFileName = buildStoredFileName(payload.userId, extension);
        Path storedPath = avatarDAO.getAvatarDirectory().resolve(storedFileName).normalize();
        // File ảnh thật được lưu tập trung trên server.
        Files.write(storedPath, avatarBytes);

        // DB chỉ giữ path tương đối để map user -> avatar server-side.
        String relativePath = Path.of("avatars").resolve(storedFileName).toString().replace("\\", "/");
        avatarDAO.saveAvatar(payload.userId, relativePath);

        AvatarPayload responsePayload = new AvatarPayload(
                payload.userId,
                relativePath,
                storedFileName,
                payload.imageBase64
        );

        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "AVATAR_SAVE_OK");
        responseNode.put("payloadJson", gson.toJson(responsePayload));
        return responseNode.toString();
    }

    public static String getAvatar(ClientHandler handler, JsonNode node) throws Exception {
        Message msg = mapper.treeToValue(node, Message.class);
        AvatarDAO avatarDAO = new AvatarDAO();
        Optional<String> avatarPath = avatarDAO.getAvatarPath(msg.Id_user);

        String storedPath = avatarPath.orElse("");
        String imageBase64 = "";
        if (!storedPath.isBlank()) {
            // Server đọc file từ data/avatars rồi encode lại Base64 để trả cho client.
            Path resolvedPath = resolveStoredAvatarPath(storedPath);
            if (Files.exists(resolvedPath)) {
                imageBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(resolvedPath));
            }
        }

        AvatarPayload payload = new AvatarPayload(msg.Id_user, storedPath, null, imageBase64);
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("type", "AVATAR_OK");
        responseNode.put("payloadJson", gson.toJson(payload));
        return responseNode.toString();
    }

    private static byte[] decodeAvatarBytes(String imageBase64) {
        // Giải mã Base64 từ client thành byte[] ảnh thật.
        if (imageBase64 == null || imageBase64.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(imageBase64);
    }

    private static String buildStoredFileName(String userId, String extension) {
        // Tạo tên file duy nhất trên server để tránh ghi đè giữa các lần upload.
        String safeExtension = extension.isBlank() ? ".png" : extension;
        return userId + "_" + System.currentTimeMillis() + safeExtension;
    }

    private static String extractExtension(String fileName) {
        // Giữ extension gốc để file lưu ra có định dạng đúng.
        if (fileName == null || fileName.isBlank()) {
            return ".png";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".png";
        }
        return fileName.substring(dotIndex);
    }

    private static Path resolveStoredAvatarPath(String storedPath) {
        // Path trong DB là path tương đối; convert về path vật lý trên server.
        Path path = Path.of(storedPath);
        if (path.isAbsolute()) {
            return path;
        }
        return Path.of("data").resolve(path);
    }
}
