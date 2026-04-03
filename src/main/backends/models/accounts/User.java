package models.accounts;

import models.bidding.CanBidding;
import models.core.Account;
import models.selling.CanSelling;

public class User extends Account implements CanBidding, CanSelling {

}
