package tej.billing.lib;

import android.app.Activity;
import android.content.Context;

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

import java.util.List;

import tej.billing.lib.interfaces.OnAvailableItemsListener;
import tej.billing.lib.interfaces.OnPurchaseChangeListener;
import tej.billing.lib.interfaces.OnQueryItems;
import tej.billing.lib.interfaces.OnQueryPurchases;

public class Billing {

    private static BillingClient billingClient;
    private static PurchasesUpdatedListener purchasesUpdatedListener;
    private static OnPurchaseChangeListener purchaseChangeListener;

    public static void init(Context context) {
        setPurchasesUpdatedListener((Activity) context);
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

    private static void setPurchasesUpdatedListener(Activity activity) {
        purchasesUpdatedListener = (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(activity, purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                notifyPurchaseCancelled(activity);
            } else {
                notifyPurchaseFailed(activity);
            }
        };
    }

    // Sending callbacks to UI Thread

    private static void notifyPurchaseSuccess(Activity activity) {
        activity.runOnUiThread(() -> purchaseChangeListener.onSuccess());
    }

    private static void notifyPurchaseCancelled(Activity activity) {
        activity.runOnUiThread(() -> purchaseChangeListener.onCanceled());
    }

    private static void notifyPurchaseFailed(Activity activity) {
        activity.runOnUiThread(() -> purchaseChangeListener.onFailed());
    }

    /***
     * Acknowledges purchase if it's not already acknowledged
     * @param activity activity from where billing flow is launched
     * @param purchase purchase
     */
    public static void handlePurchase(Activity activity, Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams,
                        billingResult -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                notifyPurchaseSuccess(activity);
                            } else {
                                notifyPurchaseFailed(activity);
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
                onAvailableItemsListener.onFailed();
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
                onAvailableItemsListener.onFailed();
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
        billingClient.querySkuDetailsAsync(params.build(),
                onAvailableItemsListener::onSkuDetailsResponse);
    }


    /***
     * Provides user's all owned InApp Purchase items
     * @param onQueryItems onQueryItems
     */
    public static void getOwnedInAppPurchasedItems(OnQueryItems onQueryItems) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                queryUserOwnedItems(onQueryItems, BillingClient.SkuType.SUBS);
            }

            @Override
            public void onBillingServiceDisconnected() {
                onQueryItems.onFailed();
            }
        });
    }

    /***
     * Provides user's all owned Subscribed items
     * @param onQueryItems onQueryItems
     */
    public static void getOwnedSubscribedItems(OnQueryItems onQueryItems) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                queryUserOwnedItems(onQueryItems, BillingClient.SkuType.SUBS);
            }

            @Override
            public void onBillingServiceDisconnected() {
                onQueryItems.onFailed();
            }
        });
    }

    /***
     * Queries user owned items
     * @param onQueryItems onQueryItems
     * @param skuType sku type
     */

    public static void queryUserOwnedItems(OnQueryItems onQueryItems, String skuType) {
        billingClient.queryPurchaseHistoryAsync(skuType, (result, records) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                onQueryItems.onSuccess(result, records);
            } else {
                onQueryItems.onFailed();
            }
        });
    }

    /***
     * Provides user's all InApp Purchases items which may not be acknowledged or pending state.
     * @param onQueryPurchases onQueryPurchases
     */
    public static void queryAppPurchasesItems(OnQueryPurchases onQueryPurchases) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                queryPurchasesItems(BillingClient.SkuType.INAPP, onQueryPurchases);
            }

            @Override
            public void onBillingServiceDisconnected() {
                onQueryPurchases.onFailed();
            }
        });
    }

    /***
     * Provides user's all Subscription items which may not be acknowledged or in pending state.
     * @param onQueryPurchases onQueryPurchases
     */
    public static void querySubscriptionItems(OnQueryPurchases onQueryPurchases) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                queryPurchasesItems(BillingClient.SkuType.SUBS, onQueryPurchases);
            }

            @Override
            public void onBillingServiceDisconnected() {
                onQueryPurchases.onFailed();
            }
        });
    }

    /***
     * Query InApp and Subscription items which may not be acknowledged or in pending state.
     * @param skuType skuType
     * @param onQueryPurchases onQueryPurchases
     */
    private static void queryPurchasesItems(String skuType, OnQueryPurchases onQueryPurchases) {
        billingClient.queryPurchasesAsync(skuType,
                onQueryPurchases::onSuccess);
    }

    /***
     * Purchase product
     * @param activity activity
     * @param skuDetails skuDetails
     * @param onPurchaseChangeListener onPurchaseChangeListener
     */
    public static void purchase(Activity activity, SkuDetails skuDetails,
                                OnPurchaseChangeListener onPurchaseChangeListener) {
        setPurchasesChangeListener(onPurchaseChangeListener);

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams)
                .getResponseCode();

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            onPurchaseChangeListener.onFailed();
        }
    }
}
