package com.openclaw.gateway;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private WebView webView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "OpenClawPrefs";
    private static final String KEY_URL = "gateway_url";
    private static final String KEY_TOKEN = "gateway_token";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        webView = findViewById(R.id.webView);
        
        setupWebView();
        
        // 检查是否有保存的配置
        String savedUrl = prefs.getString(KEY_URL, "");
        if (savedUrl.isEmpty()) {
            showConfigDialog();
        } else {
            loadGateway(savedUrl);
        }
    }
    
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " OpenClawApp/1.0");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
    }
    
    private void loadGateway(String url) {
        String token = prefs.getString(KEY_TOKEN, "");
        
        // 如果有token，通过JavaScript注入
        if (!token.isEmpty()) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    // 注入token到localStorage
                    String js = "javascript:localStorage.setItem('openclaw_token', '" + token + "');";
                    view.evaluateJavascript(js, null);
                }
            });
        }
        
        webView.loadUrl(url);
    }
    
    private void showConfigDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        
        EditText urlInput = new EditText(this);
        urlInput.setHint("网关地址 (如: http://192.168.1.100:8080)");
        urlInput.setText(prefs.getString(KEY_URL, ""));
        layout.addView(urlInput);
        
        EditText tokenInput = new EditText(this);
        tokenInput.setHint("Token (可选)");
        tokenInput.setText(prefs.getString(KEY_TOKEN, ""));
        layout.addView(tokenInput);
        
        new AlertDialog.Builder(this)
            .setTitle("配置网关")
            .setView(layout)
            .setPositiveButton("保存", (dialog, which) -> {
                String url = urlInput.getText().toString().trim();
                String token = tokenInput.getText().toString().trim();
                
                if (url.isEmpty()) {
                    url = "http://localhost:8080";
                }
                
                prefs.edit()
                    .putString(KEY_URL, url)
                    .putString(KEY_TOKEN, token)
                    .apply();
                
                loadGateway(url);
            })
            .setCancelable(false)
            .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showConfigDialog();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
