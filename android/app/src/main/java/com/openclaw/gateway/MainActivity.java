package com.openclaw.gateway;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
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
    private TextView connectionTitle;
    private TextView connectionDetail;
    private TextView connectionBadge;
    private TextView tokenBadge;
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
        connectionTitle = findViewById(R.id.connectionTitle);
        connectionDetail = findViewById(R.id.connectionDetail);
        connectionBadge = findViewById(R.id.connectionBadge);
        tokenBadge = findViewById(R.id.tokenBadge);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptyDescription = findViewById(R.id.emptyDescription);
    }

    private void configureUi() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

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
        settings.setUserAgentString(settings.getUserAgentString() + " OpenClawGateway/1.1");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new GatewayWebViewClient());
    }

    private void loadGateway(String rawUrl) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl == null) {
            showToast(getString(R.string.invalid_url_error));
            showConfigDialog();
            return;
        }

        setLoadingState(normalizedUrl);
        webView.loadUrl(normalizedUrl);
    }

    private void showConfigDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gateway_config, null);
        TextInputLayout urlLayout = dialogView.findViewById(R.id.urlInputLayout);
        TextInputLayout tokenLayout = dialogView.findViewById(R.id.tokenInputLayout);
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
            tokenLayout.setError(null);

            String normalizedUrl = normalizeUrl(valueOf(urlInput));
            String token = valueOf(tokenInput);

            if (normalizedUrl == null) {
                urlLayout.setError(getString(R.string.invalid_url_error));
                return;
            }

            saveConfiguration(normalizedUrl, token);
            dialog.dismiss();
            loadGateway(normalizedUrl);
            showToast(getString(R.string.config_saved));
        }));
        dialog.show();
    }

    private void saveConfiguration(String url, String token) {
        prefs.edit()
                .putString(KEY_URL, url)
                .putString(KEY_TOKEN, token)
                .apply();
        updateGatewaySummary(buildGatewayLabel(url), getString(R.string.status_ready_detail), !token.isEmpty(), false);
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

    private void setLoadingState(String url) {
        webView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);
        updateGatewaySummary(buildGatewayLabel(url), getString(R.string.status_loading_detail), hasToken(), false);
    }

    private void setReadyState(String url) {
        webView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        updateGatewaySummary(buildGatewayLabel(url), getString(R.string.status_ready_detail), hasToken(), true);
    }

    private void showUnconfiguredState() {
        webView.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        emptyTitle.setText(R.string.empty_title);
        emptyDescription.setText(R.string.empty_description);
        updateGatewaySummary(
                getString(R.string.status_unconfigured_title),
                getString(R.string.status_unconfigured_detail),
                hasToken(),
                false
        );
    }

    private void showErrorState(String url, String message) {
        webView.setVisibility(View.INVISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        loadingIndicator.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        emptyTitle.setText(R.string.error_title);
        emptyDescription.setText(getString(R.string.error_description, message));
        updateGatewaySummary(buildGatewayLabel(url), message, hasToken(), false);
        applyBadgeStyle(connectionBadge, getString(R.string.badge_failed), R.color.status_error_bg, R.color.status_error_text);
    }

    private void updateGatewaySummary(String title, String detail, boolean hasToken, boolean connected) {
        connectionTitle.setText(title);
        connectionDetail.setText(detail);
        applyBadgeStyle(
                connectionBadge,
                connected ? getString(R.string.badge_connected) : hasSavedUrl() ? getString(R.string.badge_connecting) : getString(R.string.badge_not_configured),
                connected ? R.color.status_success_bg : hasSavedUrl() ? R.color.status_warning_bg : R.color.status_neutral_bg,
                connected ? R.color.status_success_text : hasSavedUrl() ? R.color.status_warning_text : R.color.status_neutral_text
        );
        applyBadgeStyle(
                tokenBadge,
                hasToken ? getString(R.string.badge_token_ready) : getString(R.string.badge_token_empty),
                hasToken ? R.color.token_ready_bg : R.color.token_empty_bg,
                hasToken ? R.color.token_ready_text : R.color.token_empty_text
        );
    }

    private void applyBadgeStyle(TextView badge, String text, int backgroundColorRes, int textColorRes) {
        badge.setText(text);
        badge.getBackground().mutate().setTint(ContextCompat.getColor(this, backgroundColorRes));
        badge.setTextColor(ContextCompat.getColor(this, textColorRes));
    }

    private void injectToken(WebView view) {
        String token = getSavedToken();
        if (token.isEmpty()) {
            return;
        }

        String script = "window.localStorage.setItem('openclaw_token', " + JSONObject.quote(token) + ");";
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

    private String buildGatewayLabel(String url) {
        if (TextUtils.isEmpty(url)) {
            return getString(R.string.status_unconfigured_title);
        }

        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return url;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme() + "://";
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        return scheme + host + port;
    }

    private boolean hasSavedUrl() {
        return !getSavedUrl().isEmpty();
    }

    private boolean hasToken() {
        return !getSavedToken().isEmpty();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh) {
            if (hasSavedUrl()) {
                loadGateway(getSavedUrl());
            } else {
                showConfigDialog();
            }
            return true;
        }
        if (itemId == R.id.action_settings) {
            showConfigDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            setLoadingState(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            injectToken(view);
            setReadyState(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                String failingUrl = request.getUrl() == null ? getSavedUrl() : request.getUrl().toString();
                String description = error == null || error.getDescription() == null
                        ? getString(R.string.error_unknown)
                        : error.getDescription().toString();
                showErrorState(failingUrl, description);
            }
        }
    }
}
