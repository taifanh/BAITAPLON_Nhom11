package backends.common.messages.MsgData;

import java.util.List;

public class BuyerItemResponse {
    public String type = "BUY_ITEM_RESPONSE";
    public List<ItemRecordDto> itemlist;

    public BuyerItemResponse(){}
}
