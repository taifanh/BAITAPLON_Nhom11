package models.core;

public abstract class Entity {
    protected String id;

    public Entity() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static String makeItemId(Long x) {
        int len = 8;
        String id = String.valueOf(x);
        len -= id.length();
        for (int i = 0; i < len; i++) {
            id = "0" + id;
        }
        return id;
    }
}
