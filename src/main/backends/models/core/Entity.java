package models.core;

import controllers.UserJsonStore;

import java.io.IOException;
import java.util.Random;

public abstract class Entity {
    protected String id;

    public Entity() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
