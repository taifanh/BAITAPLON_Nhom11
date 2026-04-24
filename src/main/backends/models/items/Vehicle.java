package models.items;

import models.Extra.IdGenerator;
import models.core.Item;

public class Vehicle extends Item {
    public Vehicle(String id,String name,double prices,String info){
        super(id, name, prices, info);
    }
}
