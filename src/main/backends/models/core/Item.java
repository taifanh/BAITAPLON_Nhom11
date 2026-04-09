package models.core;

public abstract class Item extends Entity {
    protected String name;
    protected double prices;
    protected  String info;
    public  Item(String name,double prices,String info){
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
}
