package models.core;

public abstract class Entity {
    protected String id;
    public Entity(String id){
        this.id=id;
    }
    public String getId(){
        return id;
    }
//    public abstract Entity newObject();
}
