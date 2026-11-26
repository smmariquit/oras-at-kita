package dev.stimmie.orasatkita;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;

/** Shows the web page from inside the app, so it opens without internet. */
public class MainActivity extends Activity {
    private static final String HOST = "oras-at-kita.stimmie.dev";
    private static MainActivity shown;
    private WebView web;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.paper));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        web.addJavascriptInterface(new Bridge(), "OrasNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView w, WebResourceRequest r) {
                Uri u = r.getUrl();
                return HOST.equals(u.getHost()) ? asset(u.getPath()) : null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest r) {
                if (HOST.equals(r.getUrl().getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("https://" + HOST + "/");
        Alarms.daily(this);
    }

    private WebResourceResponse asset(String path) {
        String name = path == null || path.equals("/") ? "index.html" : path.substring(1);
        String type = name.endsWith(".html") ? "text/html" : name.endsWith(".svg") ? "image/svg+xml"
                : name.endsWith(".png") ? "image/png" : "application/json";
        try {
            return new WebResourceResponse(type, "utf-8", getAssets().open(name));
        } catch (IOException e) {
            return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        shown = this;
        web.onResume();
        pull();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shown = null;
        web.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        web.destroy();
    }

    private void pull() { web.evaluateJavascript("window.nativeResume && nativeResume()", null); }

    static void timerChanged() { if (shown != null) shown.pull(); }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.nativeBack ? nativeBack() : false", r -> { if (!"true".equals(r)) finish(); });
    }

    private void askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            runOnUiThread(() -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1));
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) Notify.timer(this);
    }

    /** What the page can call. */
    private final class Bridge {
        @JavascriptInterface
        public void setState(boolean consent, String cat, String start) {
            Task t = Task.find(cat);
            if (t != null && Timer.task(MainActivity.this) != t) askNotifications();
            Timer.set(MainActivity.this, consent, t, Long.parseLong(start));
        }

        @JavascriptInterface
        public String pull() { return Timer.pull(MainActivity.this); }

        @JavascriptInterface
        public void ack(int n) { Timer.ack(MainActivity.this, n); }

        @JavascriptInterface
        public void setReminder(String time) {
            if (!time.isEmpty()) askNotifications();
            Timer.prefs(MainActivity.this).edit().putString("remind", time).commit();
            Alarms.daily(MainActivity.this);
        }

        @JavascriptInterface
        public boolean pinWidget() {
            AppWidgetManager m = getSystemService(AppWidgetManager.class);
            return m.isRequestPinAppWidgetSupported() && m.requestPinAppWidget(new ComponentName(MainActivity.this, TimerWidget.class), null, null);
        }

        @JavascriptInterface
        public void share(String url) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url), null));
        }
    }
}
