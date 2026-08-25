package ink.wenmo.ime.engine;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimeEngine implements InputEngine {
    private static final String TAG = "SimeEngine";

    private static final int ASSET_VERSION = 1;
    private static final String ASSET_VERSION_MARKER = ".deployed_version";

    private static boolean sLoaded = false;

    static {
        try {
            System.loadLibrary("wenmo_sime");
            sLoaded = true;
        } catch (Throwable e1) {
            try {
                System.loadLibrary("sime_jni");
                sLoaded = true;
            } catch (Throwable e2) {
                sLoaded = false;
            }
        }
    }

    private static void logE(String msg, Throwable t) {
        try {
            Log.e(TAG, msg, t);
        } catch (Throwable ignored) {
        }
    }

    private static void logI(String msg) {
        try {
            Log.i(TAG, msg);
        } catch (Throwable ignored) {
        }
    }

    private static native boolean nativeLoadResources(String triePath, String modelPath);
    private static native boolean nativeIsReady();
    private static native int nativeContextSize();
    private static native void nativeResetCaches();

    private static native int nativeDecodeSentence(String input, int extra);
    private static native int nativeDecodeNumSentence(String prefixLetters, String digits, int extra);
    private static native int nativeNextTokens(int[] contextIds, int limit, boolean enOnly);
    private static native int nativeGetTokens(String prefix, int limit, boolean enOnly);
    private static native String[] nativeT9PinyinSyllables(String digits, int limit);

    private static native String nativeResultText(int index);
    private static native String nativeResultUnits(int index);
    private static native int nativeResultConsumed(int index);
    private static native int[] nativeResultTokenIds(int index);

    private static native boolean nativeLoadUserSentence(String path);
    private static native boolean nativeSaveUserSentence(String path);
    private static native void nativeSetUserSentenceEnabled(boolean enabled);
    private static native void nativeLearnUserSentence(int[] context, int[] tokens);

    private volatile boolean ready = false;
    private volatile String userSentencePath = null;
    private int pendingUserSentenceSaves = 0;
    private static final int USER_SENTENCE_FLUSH_THRESHOLD = 8;
    private static final String USER_SENTENCE_FILENAME = "sentences.txt";

    private final StringBuilder composition = new StringBuilder();
    private boolean traditional = false;
    private final InputEngine fallbackEngine;

    public SimeEngine(Context context) {
        if (context != null) {
            fallbackEngine = new RustInputEngine(context);
            final Context appCtx = context.getApplicationContext();
            start(appCtx);
        } else {
            fallbackEngine = new LocalInputEngine(null);
        }
    }

    public boolean isReady() {
        return ready;
    }

    public int contextSize() {
        return ready ? nativeContextSize() : 2;
    }

    public void resetCaches() {
        if (ready) nativeResetCaches();
    }

    public void start(Context context) {
        final Context appCtx = context.getApplicationContext();
        new Thread(() -> doStart(appCtx), "sime-init").start();
    }

    public void stop() {
        ready = false;
    }

    public DecodeResult[] decodeSentence(String input, int extra) {
        if (!ready) return new DecodeResult[0];
        int count = nativeDecodeSentence(input, extra);
        return readResults(count);
    }

    @Override
    public void type(char value) {
        if (value >= 'a' && value <= 'z') {
            composition.append(value);
        }
        if (fallbackEngine != null) {
            fallbackEngine.type(value);
        }
    }

    @Override
    public void backspace() {
        if (composition.length() > 0) {
            composition.deleteCharAt(composition.length() - 1);
        }
        if (fallbackEngine != null) {
            fallbackEngine.backspace();
        }
    }

    @Override
    public void clear() {
        composition.setLength(0);
        if (fallbackEngine != null) {
            fallbackEngine.clear();
        }
    }

    @Override
    public String composition() {
        return composition.toString();
    }

    @Override
    public List<String> candidates() {
        if (composition.length() == 0) {
            return Collections.emptyList();
        }
        if (ready) {
            DecodeResult[] results = decodeSentence(composition.toString(), 20);
            if (results != null && results.length > 0) {
                List<String> list = new ArrayList<>(results.length);
                for (DecodeResult r : results) {
                    if (r.text != null && !r.text.isEmpty()) {
                        list.add(r.text);
                    }
                }
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }
        if (fallbackEngine != null) {
            return fallbackEngine.candidates();
        }
        return Collections.emptyList();
    }

    @Override
    public void setTraditional(boolean traditional) {
        this.traditional = traditional;
        if (fallbackEngine != null) {
            fallbackEngine.setTraditional(traditional);
        }
    }

    @Override
    public boolean isTraditional() {
        return traditional;
    }

    public void learnUserSentence(int[] context, int[] tokens) {
        if (!ready || tokens == null || tokens.length == 0) return;
        nativeLearnUserSentence(context != null ? context : new int[0], tokens);
        if (++pendingUserSentenceSaves >= USER_SENTENCE_FLUSH_THRESHOLD) {
            flushUserSentence();
        }
    }

    public void flushUserSentence() {
        if (!ready || pendingUserSentenceSaves == 0 || userSentencePath == null) return;
        if (!nativeSaveUserSentence(userSentencePath)) {
            logE("save user sentence failed: " + userSentencePath, null);
        }
        pendingUserSentenceSaves = 0;
    }

    private static DecodeResult[] readResults(int count) {
        DecodeResult[] results = new DecodeResult[count];
        for (int i = 0; i < count; i++) {
            results[i] = new DecodeResult(
                nativeResultText(i),
                nativeResultUnits(i),
                nativeResultConsumed(i),
                nativeResultTokenIds(i)
            );
        }
        return results;
    }

    private void doStart(Context appCtx) {
        if (!sLoaded) {
            logE("native library not loaded", null);
            return;
        }
        try {
            File dataDir = new File(appCtx.getFilesDir(), "sime");
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                logE("cannot create data dir: " + dataDir, null);
                return;
            }
            ensureAssetsFresh(appCtx, dataDir);
            String triePath = new File(dataDir, "sime.dict").getAbsolutePath();
            String modelPath = new File(dataDir, "sime.cnt").getAbsolutePath();
            if (!nativeLoadResources(triePath, modelPath)) {
                logE("nativeLoadResources failed: trie=" + triePath + " model=" + modelPath, null);
                return;
            }
            userSentencePath = new File(dataDir, USER_SENTENCE_FILENAME).getAbsolutePath();
            nativeLoadUserSentence(userSentencePath);
            nativeSetUserSentenceEnabled(true);
            ready = true;
            logI("engine ready");
        } catch (Throwable t) {
            logE("engine start failed", t);
        }
    }

    private void ensureAssetsFresh(Context ctx, File dataDir) {
        File marker = new File(dataDir, ASSET_VERSION_MARKER);
        int deployed = readMarkerVersion(marker);
        if (deployed != ASSET_VERSION) {
            logI("asset version " + deployed + " → " + ASSET_VERSION + ", re-extracting");
            new File(dataDir, "sime.dict").delete();
            new File(dataDir, "sime.cnt").delete();
        }
        boolean trieOk = extractAsset(ctx, "sime/sime.dict", dataDir, "sime.dict");
        boolean cntOk = extractAsset(ctx, "sime/sime.cnt", dataDir, "sime.cnt");
        if (trieOk && cntOk && deployed != ASSET_VERSION) {
            writeMarkerVersion(marker, ASSET_VERSION);
        }
    }

    private static int readMarkerVersion(File marker) {
        if (!marker.exists()) return -1;
        try (FileInputStream fis = new FileInputStream(marker)) {
            byte[] buf = new byte[16];
            int n = fis.read(buf);
            if (n <= 0) return -1;
            return Integer.parseInt(new String(buf, 0, n).trim());
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    private static void writeMarkerVersion(File marker, int version) {
        try (FileOutputStream fos = new FileOutputStream(marker)) {
            fos.write(Integer.toString(version).getBytes());
        } catch (IOException ignored) {
        }
    }

    private static boolean extractAsset(Context context, String assetName, File destDir, String outputName) {
        File dest = new File(destDir, outputName);
        if (dest.exists() && dest.length() > 0) return true;
        try (InputStream is = context.getAssets().open(assetName);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = is.read(buf)) > 0) {
                os.write(buf, 0, n);
            }
            logI("Extracted " + assetName + " (" + dest.length() + " bytes)");
            return true;
        } catch (IOException e) {
            logE("Failed to extract " + assetName, e);
            return false;
        }
    }
}
