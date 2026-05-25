package us.achik.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://app.achik.us";

    private WebView webView;
    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private View offlineView;
    private AdsManager ads;

    private boolean pageHadError = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable interstitialTick = new Runnable() {
        @Override public void run() {
            ads.maybeShowInterstitial();
            handler.postDelayed(this, 30_000L);
        }
    };
    private final Runnable rewardedTick = new Runnable() {
        @Override public void run() {
            // Rewarded shown opportunistically; here we just keep it preloaded.
            // Apps would typically call ads.maybeShowRewarded() from a user-triggered action.
            handler.postDelayed(this, 60_000L);
        }
    };

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Achik);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        swipe = findViewById(R.id.swipe);
        progress = findViewById(R.id.progress);
        offlineView = findViewById(R.id.offlineView);
        findViewById(R.id.retryBtn).setOnClickListener(v -> retry());

        ads = new AdsManager(this);

        // Keep splash visible until first commit.
        final boolean[] ready = {false};
        splash.setKeepOnScreenCondition(() -> !ready[0]);
        handler.postDelayed(() -> ready[0] = true, 600);

        configureWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            loadHome();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                    ads.markUserInteracted();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        handler.postDelayed(interstitialTick, 90_000L);
        handler.postDelayed(rewardedTick, 30_000L);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDatabaseEnabled(true);
        s.setUserAgentString(s.getUserAgentString() + " AchikApp/1.0");

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int nightMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            WebSettingsCompat.setForceDark(s,
                    nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                            ? WebSettingsCompat.FORCE_DARK_ON
                            : WebSettingsCompat.FORCE_DARK_OFF);
        }

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals("app.achik.us") || host.endsWith(".achik.us"))) {
                    return false; // load in-app
                }
                // Open external links in browser
                try {
                    startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                pageHadError = false;
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                swipe.setRefreshing(false);
                if (!pageHadError) {
                    offlineView.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }
                ads.markUserInteracted(); // navigation counts as interaction
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    pageHadError = true;
                    showOffline();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                if (newProgress >= 100) progress.setVisibility(View.GONE);
            }
        });

        webView.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) ads.markUserInteracted();
            return false;
        });

        swipe.setOnRefreshListener(() -> {
            ads.markUserInteracted();
            if (isOnline()) {
                webView.reload();
            } else {
                swipe.setRefreshing(false);
                showOffline();
            }
        });
    }

    private void loadHome() {
        if (isOnline()) {
            offlineView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(HOME_URL);
        } else {
            showOffline();
        }
    }

    private void retry() {
        ads.markUserInteracted();
        if (isOnline()) {
            offlineView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            if (webView.getUrl() == null) webView.loadUrl(HOME_URL);
            else webView.reload();
        }
    }

    private void showOffline() {
        progress.setVisibility(View.GONE);
        swipe.setRefreshing(false);
        webView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() { webView.onPause(); super.onPause(); }

    @Override
    protected void onResume() { super.onResume(); webView.onResume(); }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
