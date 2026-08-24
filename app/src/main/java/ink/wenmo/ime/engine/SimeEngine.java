package ink.wenmo.ime.engine;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SimeEngine implements InputEngine {
    private static boolean isLibraryLoaded = false;
    private final InputEngine fallbackEngine;
    private boolean initialized = false;

    static {
        try {
            System.loadLibrary("wenmo_sime");
            isLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            isLibraryLoaded = false;
        }
    }

    public SimeEngine(Context context) {
        if (isLibraryLoaded && context != null) {
            try {
                File dictFile = copyAssetToFiles(context, "sime/sime.dict", "sime.dict");
                File cntFile = copyAssetToFiles(context, "sime/sime.cnt", "sime.cnt");
                if (dictFile != null && cntFile != null) {
                    initialized = nativeInit(dictFile.getAbsolutePath(), cntFile.getAbsolutePath());
                }
            } catch (Exception e) {
                initialized = false;
            }
        }

        if (!initialized) {
            fallbackEngine = new RustInputEngine(context);
        } else {
            fallbackEngine = null;
        }
    }

    @Override
    public void type(char value) {
        if (initialized) {
            nativeType(value);
        } else if (fallbackEngine != null) {
            fallbackEngine.type(value);
        }
    }

    @Override
    public void backspace() {
        if (initialized) {
            nativeBackspace();
        } else if (fallbackEngine != null) {
            fallbackEngine.backspace();
        }
    }

    @Override
    public void clear() {
        if (initialized) {
            nativeClear();
        } else if (fallbackEngine != null) {
            fallbackEngine.clear();
        }
    }

    @Override
    public String composition() {
        if (initialized) {
            String comp = nativeComposition();
            return comp != null ? comp : "";
        } else if (fallbackEngine != null) {
            return fallbackEngine.composition();
        }
        return "";
    }

    @Override
    public List<String> candidates() {
        if (initialized) {
            String[] arr = nativeCandidates();
            if (arr != null) {
                return Arrays.asList(arr);
            }
        } else if (fallbackEngine != null) {
            return fallbackEngine.candidates();
        }
        return Collections.emptyList();
    }

    @Override
    public void setTraditional(boolean traditional) {
        if (initialized) {
            nativeSetTraditional(traditional);
        } else if (fallbackEngine != null) {
            fallbackEngine.setTraditional(traditional);
        }
    }

    @Override
    public boolean isTraditional() {
        if (initialized) {
            return nativeIsTraditional();
        } else if (fallbackEngine != null) {
            return fallbackEngine.isTraditional();
        }
        return false;
    }

    private File copyAssetToFiles(Context context, String assetPath, String outputName) {
        try {
            File outFile = new File(context.getFilesDir(), outputName);
            if (outFile.exists() && outFile.length() > 0) {
                return outFile;
            }
            try (InputStream in = context.getAssets().open(assetPath);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    // Native JNI methods
    private static native boolean nativeInit(String dictPath, String cntPath);
    private static native void nativeType(char value);
    private static native void nativeBackspace();
    private static native void nativeClear();
    private static native String nativeComposition();
    private static native String[] nativeCandidates();
    private static native void nativeSetTraditional(boolean traditional);
    private static native boolean nativeIsTraditional();
}
