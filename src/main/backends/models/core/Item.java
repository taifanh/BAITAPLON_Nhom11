package models.core;

import models.Extra.IdGenerator;
import models.items.Art;
import models.items.Electronics;
import models.items.ItemType;
import models.items.Vehicle;

public abstract class Item extends Entity {
    protected String name;
    protected double prices;
    protected  String info;
    public Item(String id,String name,double prices,String info){
        super(id);
        this.name=name;
        this.prices=prices;
        this.info=info;
    }
    public String getName(){
        return name;
    }
    public Double getPrices(){
        return prices;
    }
    public String getInfo(){
        return info;
    }
    public static String makeItemId(Long x){
        int len=8;
        String ID=String.valueOf(x);
        len-=ID.length();
        for(int i=0;i<len;i++) ID="0"+ID;
        return ID;
    }
    public static String addId(ItemType type) {
        String ID="";
        switch (type) {
            case Electronics:
                ID="E";
                break;
            case Vehicle:
                ID="V";
                break;
            case Art:
                ID="A";
                break;
            default:
                throw new IllegalArgumentException("Invalid item type");
        }
        return ID+makeItemId(IdGenerator.nextId(type));
    }
}
