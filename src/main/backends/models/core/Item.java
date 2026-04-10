package models.core;

public abstract class Item extends Entity {
    protected String name;
    protected double prices;
    protected  String info;
    public  Item(String id,String name,double prices,String info){
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
}
