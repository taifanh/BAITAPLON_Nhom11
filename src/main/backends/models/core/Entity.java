package models.core;

import controllers.UserJsonStore;

import java.io.IOException;
import java.util.Random;

public abstract class Entity {
    protected String id;
    private static final Random RANDOM = new Random();

    public Entity() {}

    public String generateEntity() {
        UserJsonStore userJsonStore = new UserJsonStore();
        String generatedId;

        do {
            generatedId = "USER" + (100000 + RANDOM.nextInt(899999));
        } while (isExistingId(userJsonStore, generatedId));

        return generatedId;
    }

    private boolean isExistingId(UserJsonStore userJsonStore, String id) {
        try {
            return userJsonStore.idExists(id);
        } catch (IOException e) {
            throw new RuntimeException("Khong the kiem tra ID da ton tai hay chua.", e);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
