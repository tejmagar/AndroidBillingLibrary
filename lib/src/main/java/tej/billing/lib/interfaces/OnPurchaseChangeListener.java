package tej.billing.lib.interfaces;

import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;

public interface OnPurchaseChangeListener {
    void onPurchaseSuccess(ArrayList<String> skus);
    void onPurchaseCanceled();
    void onPurchaseFailed();
}
