package tej.billing.lib;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;

import tej.billing.lib.interfaces.OnAvailableItemsListener;
import tej.billing.lib.interfaces.OnPurchaseChangeListener;
import tej.billing.lib.interfaces.OnQueryItems;
import tej.billing.lib.interfaces.OnQueryPurchases;

public class Billing {

    private static BillingClient billingClient;
    private static PurchasesUpdatedListener purchasesUpdatedListener;
    private static OnPurchaseChangeListener purchaseChangeListener;
    
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context context) {
        setPurchasesUpdatedListener(context);
        billingClient = buildBillingClient(context);
    }

    private static BillingClient buildBillingClient(Context context) {
        return BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
    }

    private static void setPurchasesChangeListener(OnPurchaseChangeListener onPurchaseChangeListener) {
        purchaseChangeListener = onPurchaseChangeListener;
    }

    private static void setPurchasesUpdatedListener(Context context) {
        purchasesUpdatedListener = (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                notifyPurchaseCancelled();
            } else {
                notifyPurchaseFailed();
            }
        };
    }

    // Sending callbacks to UI Thread

    private static void notifyPurchaseSuccess(ArrayList<String> skus) {
        handler.post(() -> purchaseChangeListener.onPurchaseSuccess(skus));
    }

    private static void notifyPurchaseCancelled() {
        handler.post(() -> purchaseChangeListener.onPurchaseCanceled());
    }

    private static void notifyPurchaseFailed() {
        handler.post(() -> purchaseChangeListener.onPurchaseFailed());
    }

    /***
     * Acknowledges purchase if it's not already acknowledged
     * @param purchase purchase
     */
    public static void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams,
                        billingResult -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                notifyPurchaseSuccess(purchase.getSkus());
                            } else {
                                notifyPurchaseFailed();
                            }
                        });
            }
        }
    }

    /***
     * Provides all the active InApp Products of the app.
     * @param skuList list of Skus
     * @param onAvailableItemsListener onAvailableItemsListener
     */

    public static void getAvailableInAppPurchaseItems(List<String> skuList,
                                                      OnAvailableItemsListener onAvailableItemsListener) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                querySkuItemsAvailable(skuList, BillingClient.SkuType.INAPP,
                        onAvailableItemsListener);
            }

            @Override
            public void onBillingServiceDisconnected() {
                handler.post(onAvailableItemsListener::onFailed);
            }
        });
    }

    /***
     * Provides all the active Subscription Products of the app.
     * @param skuList list of Skus
     * @param onAvailableItemsListener onAvailableItemsListener
     */

    public static void getAvailableInAppSubscriptionItems(List<String> skuList,
                                                          OnAvailableItemsListener onAvailableItemsListener) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                querySkuItemsAvailable(skuList, BillingClient.SkuType.SUBS,
                        onAvailableItemsListener);
            }

            @Override
            public void onBillingServiceDisconnected() {
                handler.post(onAvailableItemsListener::onFailed);
            }
        });
    }

    /***
     * Queries Sku Items available for a given sku List
     * @param skuList Sku list of the products
     * @param skuType Sku Type
     * @param onAvailableItemsListener onAvailableItemsListener
     */

    private static void querySkuItemsAvailable(List<String> skuList,
                                               String skuType,
                                               OnAvailableItemsListener onAvailableItemsListener) {
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(skuType);
        billingClient.querySkuDetailsAsync(params.build(), (billingResult, list) ->
                handler.post(() -> onAvailableItemsListener.onSkuDetailsResponse(billingResult, list)));
    }


    /***
     * Provides user's all owned InApp Purchase items
     * @param onQueryItems onQueryItems
     */
    public static void getUserInAppPurchasedItems(OnQueryItems onQueryItems) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                queryUserOwnedItems(onQueryItems, BillingClient.SkuType.INAPP);
            }

            @Override
            public void onBillingServiceDisconnected() {
                handler.post(onQueryItems::onFailed);
            }
        });
    }

    /***
     * Provides user's all owned Subscribed items
     * @param onQueryItems onQueryItems
     */
    public static void getUserSubscribedItems(OnQueryItems onQueryItems) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                queryUserOwnedItems(onQueryItems, BillingClient.SkuType.SUBS);
            }

            @Override
            public void onBillingServiceDisconnected() {
                handler.post(onQueryItems::onFailed);
            }
        });
    }

    /***
     * Queries user owned items
     * @param onQueryItems onQueryItems
     * @param skuType sku type
     */

    public static void queryUserOwnedItems(OnQueryItems onQueryItems, String skuType) {
        billingClient.queryPurchasesAsync(skuType, (result, purchases) ->
                handler.post(() -> onQueryItems.onSuccess(result, purchases)));
    }

    /***
     * Purchase product
     * @param context context
     * @param skuDetails skuDetails
     * @param onPurchaseChangeListener onPurchaseChangeListener
     */
    public static void purchase(Context context, SkuDetails skuDetails,
                                OnPurchaseChangeListener onPurchaseChangeListener) {
        setPurchasesChangeListener(onPurchaseChangeListener);

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        int responseCode = billingClient.launchBillingFlow((Activity) context, billingFlowParams)
                .getResponseCode();

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            handler.post(onPurchaseChangeListener::onPurchaseFailed);
        }
    }
}
