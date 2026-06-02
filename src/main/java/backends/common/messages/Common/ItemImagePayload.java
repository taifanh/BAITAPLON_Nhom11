package backends.common.messages.Common;

public class ItemImagePayload {
    public String requestId;
    public String itemId;
    public String imagePath;
    public String imageFileName;
    public String imageBase64;

    public ItemImagePayload() {
    }

    public ItemImagePayload(String requestId, String itemId, String imagePath, String imageFileName, String imageBase64) {
        this.requestId = requestId;
        this.itemId = itemId;
        this.imagePath = imagePath;
        this.imageFileName = imageFileName;
        this.imageBase64 = imageBase64;
    }
}
