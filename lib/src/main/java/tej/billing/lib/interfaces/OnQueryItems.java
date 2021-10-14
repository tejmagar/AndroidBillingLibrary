package tej.billing.lib.interfaces;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PurchaseHistoryRecord;

import java.util.List;

public interface OnQueryItems {
    void onSuccess(BillingResult billingResult, List<PurchaseHistoryRecord> records);
    void onFailed();
}
