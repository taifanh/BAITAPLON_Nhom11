package models.items;

import models.items.ItemType;
import models.Extra.IdGenerator;
import models.core.Item;

public class Art extends Item {
    public Art(String id,String name,double prices,String info){
        super(id, name, prices, info);
    }
}
