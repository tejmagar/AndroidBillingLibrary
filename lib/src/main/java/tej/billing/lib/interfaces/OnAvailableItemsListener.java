package tej.billing.lib.interfaces;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public interface OnAvailableItemsListener {
    void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList);
    void onFailed();
}
