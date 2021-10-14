package tej.billing.lib.interfaces;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;

import java.util.List;

public interface OnQueryPurchases {
    void onSuccess(BillingResult billingResult, List<Purchase> purchases);
    void onFailed();
}
