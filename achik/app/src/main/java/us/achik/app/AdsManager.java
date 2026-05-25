package us.achik.app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Manages Rewarded and Interstitial ads.
 * - Rewarded: shown only after >=30s elapsed AND user has interacted.
 * - Interstitial: shown only after >=90s elapsed AND user has interacted (nav/click).
 * Ads are throttled to avoid overloading UX.
 */
public class AdsManager {

    private static final String TAG = "AdsManager";

    private static final String REWARDED_UNIT_ID    = "ca-app-pub-5654505690672290/3056439561";
    private static final String INTERSTITIAL_UNIT_ID = "ca-app-pub-5654505690672290/6367507923";

    private static final long REWARDED_MIN_ELAPSED_MS     = 30_000L;
    private static final long INTERSTITIAL_MIN_ELAPSED_MS = 90_000L;

    // Spacing between successive ad displays to respect UX best practices.
    private static final long MIN_SPACING_BETWEEN_ADS_MS = 60_000L;

    private final Activity activity;
    private final long sessionStartMs;
    private final Handler main = new Handler(Looper.getMainLooper());

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    private boolean loadingInterstitial = false;
    private boolean loadingRewarded = false;

    private boolean userInteracted = false;
    private long lastAdShownMs = 0L;

    public AdsManager(Activity activity) {
        this.activity = activity;
        this.sessionStartMs = System.currentTimeMillis();
        // Preload both ad types asynchronously.
        main.postDelayed(this::preloadAll, 1500);
    }

    /** Call whenever user interacts (touch, scroll, navigation). */
    public void markUserInteracted() {
        userInteracted = true;
    }

    public void preloadAll() {
        loadInterstitial();
        loadRewarded();
    }

    private boolean canShowAdNow() {
        return userInteracted
                && (System.currentTimeMillis() - lastAdShownMs) >= MIN_SPACING_BETWEEN_ADS_MS;
    }

    // ---------------- Interstitial ----------------

    private void loadInterstitial() {
        if (interstitialAd != null || loadingInterstitial) return;
        loadingInterstitial = true;
        AdRequest req = new AdRequest.Builder().build();
        InterstitialAd.load(activity, INTERSTITIAL_UNIT_ID, req,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loadingInterstitial = false;
                        interstitialAd = ad;
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;
                                lastAdShownMs = System.currentTimeMillis();
                                loadInterstitial();
                            }
                            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                                interstitialAd = null;
                                loadInterstitial();
                            }
                        });
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError e) {
                        loadingInterstitial = false;
                        interstitialAd = null;
                        Log.w(TAG, "Interstitial load failed: " + e.getMessage());
                    }
                });
    }

    /** Try to show interstitial if all conditions met. Returns true if shown. */
    public boolean maybeShowInterstitial() {
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        if (elapsed < INTERSTITIAL_MIN_ELAPSED_MS) return false;
        if (!canShowAdNow()) return false;
        if (interstitialAd == null) { loadInterstitial(); return false; }
        interstitialAd.show(activity);
        return true;
    }

    // ---------------- Rewarded ----------------

    private void loadRewarded() {
        if (rewardedAd != null || loadingRewarded) return;
        loadingRewarded = true;
        AdRequest req = new AdRequest.Builder().build();
        RewardedAd.load(activity, REWARDED_UNIT_ID, req,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        loadingRewarded = false;
                        rewardedAd = ad;
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override public void onAdDismissedFullScreenContent() {
                                rewardedAd = null;
                                lastAdShownMs = System.currentTimeMillis();
                                loadRewarded();
                            }
                            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                                rewardedAd = null;
                                loadRewarded();
                            }
                        });
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError e) {
                        loadingRewarded = false;
                        rewardedAd = null;
                        Log.w(TAG, "Rewarded load failed: " + e.getMessage());
                    }
                });
    }

    /** Try to show rewarded if all conditions met. Returns true if shown. */
    public boolean maybeShowRewarded() {
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        if (elapsed < REWARDED_MIN_ELAPSED_MS) return false;
        if (!canShowAdNow()) return false;
        if (rewardedAd == null) { loadRewarded(); return false; }
        rewardedAd.show(activity, rewardItem -> { /* no-op: reward is implicit */ });
        return true;
    }
}
