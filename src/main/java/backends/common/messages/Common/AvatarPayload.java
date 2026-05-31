package backends.common.messages.Common;

public class AvatarPayload {
    public String userId;
    // Path lưu trên server, dạng tương đối như "avatars/<file>".
    public String avatarPath;
    // Tên file gốc do client gửi lên để server giữ extension đúng.
    public String avatarFileName;
    // Nội dung ảnh đã encode Base64 để gửi qua socket/JSON.
    public String imageBase64;

    public AvatarPayload() {
    }

    public AvatarPayload(String userId, String avatarPath) {
        this.userId = userId;
        this.avatarPath = avatarPath;
    }

    public AvatarPayload(String userId, String avatarPath, String avatarFileName, String imageBase64) {
        this.userId = userId;
        this.avatarPath = avatarPath;
        this.avatarFileName = avatarFileName;
        this.imageBase64 = imageBase64;
    }
}
