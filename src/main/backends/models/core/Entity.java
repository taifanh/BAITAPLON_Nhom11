package models.core;

public abstract class Entity {
    protected String id;

    public Entity(String phonenumber   ){
        StringBuffer  sb = new StringBuffer("USER");// cần thêm nhận phân biệt admin***

        for ( int i =1 ; i< phonenumber.length() ; i++){

            String tempt = String.valueOf(Integer.valueOf(phonenumber.charAt(i)-1));

            sb.append( tempt );
        }// mã hóa số điện thoại thành dãy ID

        this.id = sb.toString();
    }

    public String getId(){
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
