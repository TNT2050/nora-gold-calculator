package ir.noragold.calculator;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;
import android.database.Cursor;

public class MainActivity extends Activity {
    private WebView webView;
    private DownloadManager downloadManager;
    private long activeDownloadId = -1L;
    private Uri pendingInstallUri;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (id != activeDownloadId) return;
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor == null || !cursor.moveToFirst()) {
                    showStatus("دریافت فایل ناموفق بود؛ دوباره تلاش کنید");
                    return;
                }
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    pendingInstallUri = downloadManager.getUriForDownloadedFile(id);
                    showStatus("دانلود کامل شد؛ آماده نصب نسخه جدید");
                    beginInstall();
                } else {
                    int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                    showStatus("دانلود ناموفق بود (کد " + reason + ")؛ دوباره تلاش کنید");
                }
            } catch (Exception e) {
                showStatus("خطا در آماده‌سازی فایل نصب؛ دوباره تلاش کنید");
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17, 17, 17));
        getWindow().setNavigationBarColor(Color.rgb(10, 10, 10));
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(downloadReceiver, filter);
        receiverRegistered = true;

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 11, 11));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setDefaultTextEncodingName("UTF-8");
        webView.addJavascriptInterface(new UpdateBridge(), "NoraAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (scheme != null && (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); return true; }
                    catch (Exception e) { return false; }
                }
                return false;
            }
        });
        setContentView(webView);
        if (savedInstanceState == null) webView.loadUrl("file:///android_asset/index.html");
        else webView.restoreState(savedInstanceState);
    }

    private class UpdateBridge {
        @JavascriptInterface public void downloadApk(String url) {
            runOnUiThread(() -> startApkDownload(url));
        }
    }

    private void startApkDownload(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                !"github.com".equalsIgnoreCase(uri.getHost()) ||
                !uri.getPath().contains("/releases/") || !uri.getPath().endsWith(".apk")) {
                showStatus("نشانی فایل به‌روزرسانی معتبر نیست");
                return;
            }
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("به‌روزرسانی ماشین حساب نورا");
            request.setDescription("در حال دریافت نسخه جدید برنامه");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "nora-gold-update.apk");
            activeDownloadId = downloadManager.enqueue(request);
            showStatus("دانلود نسخه جدید آغاز شد؛ وضعیت را در اعلان‌های گوشی ببینید");
        } catch (Exception e) {
            showStatus("شروع دانلود ممکن نشد؛ اتصال اینترنت را بررسی کنید");
        }
    }

    private void beginInstall() {
        if (pendingInstallUri == null) return;
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(settingsIntent, 7102);
            } catch (Exception e) {
                showStatus("اجازه نصب برنامه از این منبع را در تنظیمات فعال کنید");
            }
            return;
        }
        launchInstaller();
    }

    private void launchInstaller() {
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(pendingInstallUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception e) {
            showStatus("باز کردن نصب‌کننده اندروید ممکن نشد؛ فایل دانلودشده در اعلان‌ها موجود است");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7102) {
            if (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls()) launchInstaller();
            else showStatus("برای نصب نسخه جدید، اجازه نصب از این برنامه لازم است");
        }
    }

    private void showStatus(String message) {
        if (webView != null) {
            String safe = message.replace("\\", "\\\\").replace("'", "\\'");
            webView.post(() -> webView.evaluateJavascript(
                "(()=>{const s=document.getElementById('updateStatus');if(s)s.textContent='" + safe + "';})()", null));
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
        if (receiverRegistered) { try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {} }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
