package com.openclaw.gateway;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "OpenClawPrefs";
    private static final String KEY_URL = "gateway_url";
    private static final String KEY_TOKEN = "gateway_token";
    private static final String DEFAULT_GATEWAY_URL = "http://localhost:8080";

    private SharedPreferences prefs;
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearProgressIndicator loadingIndicator;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        bindViews();
        configureUi();
        setupWebView();

        if (hasSavedUrl()) {
            loadGateway(getSavedUrl());
        } else {
            showUnconfiguredState();
            showConfigDialog();
        }
    }

    private void bindViews() {
        webView = findViewById(R.id.webView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        emptyState = findViewById(R.id.emptyState);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptyDescription = findViewById(R.id.emptyDescription);
    }

    private void configureUi() {
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.brand_primary),
                ContextCompat.getColor(this, R.color.brand_accent)
        );
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (hasSavedUrl()) {
                webView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                showConfigDialog();
            }
        });

        ImageButton refreshButton = findViewById(R.id.refreshButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        refreshButton.setOnClickListener(view -> {
            if (hasSavedUrl()) {
                loadGateway(getSavedUrl());
            } else {
                showConfigDialog();
            }
        });
        settingsButton.setOnClickListener(view -> showConfigDialog());

        MaterialButton retryButton = findViewById(R.id.retryButton);
        MaterialButton configureButton = findViewById(R.id.configureButton);
        retryButton.setOnClickListener(view -> {
            if (hasSavedUrl()) {
                loadGateway(getSavedUrl());
            } else {
                showConfigDialog();
            }
        });
        configureButton.setOnClickListener(view -> showConfigDialog());
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString(settings.getUserAgentString() + " OpenClawGateway/1.2");

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new GatewayWebViewClient());
        webView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) ->
                swipeRefreshLayout.setEnabled(scrollY == 0)
        );
    }

    private void loadGateway(String rawUrl) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl == null) {
            showToast(getString(R.string.invalid_url_error));
            showConfigDialog();
            return;
        }

        showLoadingState();
        webView.loadUrl(normalizedUrl);
    }

    private void showConfigDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gateway_config, null);
        TextInputLayout urlLayout = dialogView.findViewById(R.id.urlInputLayout);
        TextInputEditText urlInput = dialogView.findViewById(R.id.urlInput);
        TextInputEditText tokenInput = dialogView.findViewById(R.id.tokenInput);

        urlInput.setText(hasSavedUrl() ? getSavedUrl() : DEFAULT_GATEWAY_URL);
        tokenInput.setText(getSavedToken());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(R.string.action_reset, (d, which) -> clearConfiguration())
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            urlLayout.setError(null);

            String normalizedUrl = normalizeUrl(valueOf(urlInput));
            String token = valueOf(tokenInput);

            if (normalizedUrl == null) {
                urlLayout.setError(getString(R.string.invalid_url_error));
                return;
            }

            prefs.edit()
                    .putString(KEY_URL, normalizedUrl)
                    .putString(KEY_TOKEN, token)
                    .apply();

            dialog.dismiss();
            loadGateway(normalizedUrl);
            showToast(getString(R.string.config_saved));
        }));
        dialog.show();
    }

    private void clearConfiguration() {
        prefs.edit()
                .remove(KEY_URL)
                .remove(KEY_TOKEN)
                .apply();
        webView.loadUrl("about:blank");
        showUnconfiguredState();
        showToast(getString(R.string.config_cleared));
    }

    private void showLoadingState() {
        webView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);
    }

    private void showReadyState() {
        webView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showUnconfiguredState() {
        webView.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        emptyTitle.setText(R.string.empty_title);
        emptyDescription.setText(R.string.empty_description);
    }

    private void showErrorState(String message) {
        webView.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        emptyTitle.setText(R.string.error_title);
        emptyDescription.setText(getString(R.string.error_description, message));
    }

    private void injectPageEnhancements(WebView view) {
        String token = getSavedToken();
        String tokenScript = token.isEmpty()
                ? ""
                : "window.localStorage.setItem('openclaw_token', " + JSONObject.quote(token) + ");";

        String script = "(function(){"
                + tokenScript
                + "var stickyId='openclaw-sticky-search';"
                + "var styleId='openclaw-sticky-style';"
                + "function ensureStyle(){"
                + " if(document.getElementById(styleId)) return;"
                + " var style=document.createElement('style');"
                + " style.id=styleId;"
                + " style.textContent='.openclaw-sticky-search{position:sticky !important;top:0 !important;z-index:2147483646 !important;background:rgba(255,255,255,0.96) !important;backdrop-filter:blur(12px) !important;-webkit-backdrop-filter:blur(12px) !important;box-shadow:0 8px 24px rgba(0,0,0,0.08) !important;}.openclaw-sticky-search *{z-index:inherit !important;}';"
                + " document.head.appendChild(style);"
                + "}"
                + "function isSearchNode(node){"
                + " if(!node) return false;"
                + " var text=((node.innerText||'')+' '+(node.getAttribute&&node.getAttribute('placeholder')||'')).toLowerCase();"
                + " return text.indexOf('\\u641c\\u7d22')>=0 || text.indexOf('search')>=0;"
                + "}"
                + "function candidateFromInput(input){"
                + " var el=input;"
                + " for(var i=0;i<4 && el && el.parentElement;i++){"
                + "   if(el.getBoundingClientRect().width > window.innerWidth*0.55){ return el; }"
                + "   el=el.parentElement;"
                + " }"
                + " return input.parentElement || input;"
                + "}"
                + "function findSearchBar(){"
                + " var inputs=[].slice.call(document.querySelectorAll('input,textarea,[role=\"searchbox\"],button,div,section,header'));"
                + " for(var i=0;i<inputs.length;i++){"
                + "   var node=inputs[i];"
                + "   if(isSearchNode(node)){"
                + "     if(node.tagName==='INPUT' || node.tagName==='TEXTAREA'){ return candidateFromInput(node); }"
                + "     return node;"
                + "   }"
                + " }"
                + " return null;"
                + "}"
                + "function pinSearchBar(){"
                + " ensureStyle();"
                + " var bar=findSearchBar();"
                + " if(!bar) return false;"
                + " var parent=bar.parentElement;"
                + " while(parent && parent !== document.body){"
                + "   if(parent.style){ parent.style.overflow='visible'; }"
                + "   parent=parent.parentElement;"
                + " }"
                + " bar.classList.add(stickyId);"
                + " return true;"
                + "}"
                + "pinSearchBar();"
                + "setTimeout(pinSearchBar,400);"
                + "setTimeout(pinSearchBar,1200);"
                + "new MutationObserver(function(){ pinSearchBar(); }).observe(document.documentElement,{childList:true,subtree:true});"
                + "})();";

        view.evaluateJavascript(script, null);
    }

    private String normalizeUrl(String rawUrl) {
        String candidate = rawUrl == null ? "" : rawUrl.trim();
        if (candidate.isEmpty()) {
            candidate = DEFAULT_GATEWAY_URL;
        }

        if (!candidate.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            candidate = (isLocalAddress(candidate) ? "http://" : "https://") + candidate;
        }

        Uri uri = Uri.parse(candidate);
        if (uri.getScheme() == null || uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            return null;
        }
        return candidate;
    }

    private boolean isLocalAddress(String value) {
        String normalized = value.toLowerCase(Locale.US);
        return normalized.startsWith("localhost")
                || normalized.startsWith("127.")
                || normalized.startsWith("10.")
                || normalized.startsWith("192.168.")
                || normalized.startsWith("172.")
                || normalized.matches("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?(/.*)?$");
    }

    private boolean hasSavedUrl() {
        return !getSavedUrl().isEmpty();
    }

    private String getSavedUrl() {
        return prefs.getString(KEY_URL, "").trim();
    }

    private String getSavedToken() {
        return prefs.getString(KEY_TOKEN, "").trim();
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private final class GatewayWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showLoadingState();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            injectPageEnhancements(view);
            showReadyState();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                String description = error == null || error.getDescription() == null
                        ? getString(R.string.error_unknown)
                        : error.getDescription().toString();
                showErrorState(description);
            }
        }
    }
}
