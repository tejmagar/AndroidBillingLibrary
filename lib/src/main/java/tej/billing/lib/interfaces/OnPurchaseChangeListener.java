package tej.billing.lib.interfaces;

public interface OnPurchaseChangeListener {
    void onSuccess();
    void onCanceled();
    void onFailed();
}
