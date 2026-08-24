package ink.wenmo.ime.clipboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 完全离线且私有的剪贴板历史管理器。
 * 仅在内存中安全保存指定数量的历史剪贴文本，不进行网络访问或持久化泄露。
 */
public final class ClipboardManager {
    public static class Entry {
        private final String text;
        private final long timestamp;

        public Entry(String text, long timestamp) {
            this.text = text;
            this.timestamp = timestamp;
        }

        public String getText() { return text; }
        public long getTimestamp() { return timestamp; }
    }

    private static final int DEFAULT_MAX_ITEMS = 50;
    private final int maxItems;
    private final List<Entry> history = new ArrayList<>();

    public ClipboardManager() {
        this(DEFAULT_MAX_ITEMS);
    }

    public ClipboardManager(int maxItems) {
        this.maxItems = maxItems;
    }

    public synchronized void add(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        // 移除非空相同的历史条目，并将新记录插入顶部
        history.removeIf(entry -> entry.getText().equals(text));
        history.add(0, new Entry(text, System.currentTimeMillis()));

        while (history.size() > maxItems) {
            history.remove(history.size() - 1);
        }
    }

    public synchronized List<Entry> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized void remove(int index) {
        if (index >= 0 && index < history.size()) {
            history.remove(index);
        }
    }

    public synchronized void clear() {
        history.clear();
    }

    public synchronized int size() {
        return history.size();
    }
}
