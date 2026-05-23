package backends.common.messages.Common;

public class Message {
    public Message() {

    }
    public String Id_user;

    public String messageType; //  need type to make it easy for server to sort the message

    public String payloadJson; // string json of content

}
