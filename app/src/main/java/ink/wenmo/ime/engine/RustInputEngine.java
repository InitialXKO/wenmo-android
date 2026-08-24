package ink.wenmo.ime.engine;

import android.content.Context;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RustInputEngine implements InputEngine {
    private static boolean isLibraryLoaded = false;
    private final LocalInputEngine fallbackEngine;

    static {
        try {
            System.loadLibrary("wenmo_engine");
            isLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e1) {
            try {
                System.loadLibrary("engine");
                isLibraryLoaded = true;
            } catch (UnsatisfiedLinkError e2) {
                isLibraryLoaded = false;
            }
        }
    }

    private long nativeHandle = 0;

    public RustInputEngine(Context context) {
        if (isLibraryLoaded) {
            try {
                nativeHandle = nativeCreate();
            } catch (UnsatisfiedLinkError e) {
                nativeHandle = 0;
            }
        }
        if (nativeHandle == 0) {
            fallbackEngine = new LocalInputEngine(context);
        } else {
            fallbackEngine = null;
        }
    }

    public static boolean isAvailable() {
        return isLibraryLoaded;
    }

    @Override
    public void type(char value) {
        if (nativeHandle != 0) {
            nativeType(nativeHandle, value);
        } else if (fallbackEngine != null) {
            fallbackEngine.type(value);
        }
    }

    @Override
    public void backspace() {
        if (nativeHandle != 0) {
            nativeBackspace(nativeHandle);
        } else if (fallbackEngine != null) {
            fallbackEngine.backspace();
        }
    }

    @Override
    public void clear() {
        if (nativeHandle != 0) {
            nativeClear(nativeHandle);
        } else if (fallbackEngine != null) {
            fallbackEngine.clear();
        }
    }

    @Override
    public String composition() {
        if (nativeHandle != 0) {
            String comp = nativeComposition(nativeHandle);
            return comp != null ? comp : "";
        } else if (fallbackEngine != null) {
            return fallbackEngine.composition();
        }
        return "";
    }

    @Override
    public List<String> candidates() {
        if (nativeHandle != 0) {
            String[] arr = nativeCandidates(nativeHandle);
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
        if (nativeHandle != 0) {
            nativeSetTraditional(nativeHandle, traditional);
        } else if (fallbackEngine != null) {
            fallbackEngine.setTraditional(traditional);
        }
    }

    @Override
    public boolean isTraditional() {
        if (nativeHandle != 0) {
            return nativeIsTraditional(nativeHandle);
        } else if (fallbackEngine != null) {
            return fallbackEngine.isTraditional();
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        if (nativeHandle != 0) {
            try {
                nativeDestroy(nativeHandle);
            } catch (UnsatisfiedLinkError ignored) {
            }
            nativeHandle = 0;
        }
        super.finalize();
    }

    // Native JNI methods
    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeType(long handle, char value);
    private static native void nativeBackspace(long handle);
    private static native void nativeClear(long handle);
    private static native String nativeComposition(long handle);
    private static native String[] nativeCandidates(long handle);
    private static native void nativeSetTraditional(long handle, boolean traditional);
    private static native boolean nativeIsTraditional(long handle);
}
