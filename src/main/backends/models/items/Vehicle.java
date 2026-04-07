package models.items;

import models.core.Item;

public class Vehicle extends Item {
    public Vehicle(String id,String name,double prices,String info){
        super( name, prices, info);
    }
}
