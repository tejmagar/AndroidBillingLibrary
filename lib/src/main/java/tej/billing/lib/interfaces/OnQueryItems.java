package tej.billing.lib.interfaces;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;

import java.util.List;

public interface OnQueryItems {
    void onSuccess(BillingResult billingResult, List<Purchase> purchases);
    void onFailed();
}
