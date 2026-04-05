package models.items;

import models.Extra.IdGenerator;
import models.core.Item;

public class Electronics extends Item {
    public  Electronics(String id,String name,double prices,String info){
        super(id, name, prices, info);
    }
    public static String addId(){
        return "E"+makeItemId(IdGenerator.nextId(ItemType.Electronics));
    }
}
