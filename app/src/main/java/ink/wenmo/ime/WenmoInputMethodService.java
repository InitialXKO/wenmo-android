package ink.wenmo.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.view.Gravity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ink.wenmo.ime.calculator.CalculatorEngine;
import ink.wenmo.ime.data.SymbolData;
import ink.wenmo.ime.engine.InputEngine;
import ink.wenmo.ime.engine.LocalInputEngine;
import ink.wenmo.ime.engine.RustInputEngine;

public final class WenmoInputMethodService extends InputMethodService {
    private InputEngine engine;
    private ink.wenmo.ime.clipboard.ClipboardManager clipboardHistoryManager;
    private ClipboardManager systemClipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;

    private LinearLayout candidates;
    private LinearLayout keyboardPanel;
    private LinearLayout toolsBar;
    private TextView composition;
    private Button scriptToggle;
    private KeyboardMode keyboardMode = KeyboardMode.ALPHABETIC;

    private StringBuilder numberComposition = new StringBuilder();
    private int selectedSymbolCategoryIndex = 0;
    private int selectedEmojiCategoryIndex = 0;

    private final List<String> quickPhrases = new ArrayList<>(Arrays.asList(
        "好的，没问题！", "收到，谢谢！", "麻烦稍等一下。", "我马上处理。",
        "祝您工作顺利！", "辛苦啦！", "今天天气不错。", "保持联系！"
    ));

    private final List<EmojiCategory> emojiCategories = Arrays.asList(
        new EmojiCategory("😊", Arrays.asList("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "☺️", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠")),
        new EmojiCategory("👍", Arrays.asList("👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦵", "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅", "👄")),
        new EmojiCategory("🐱", Arrays.asList("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌", "🐞", "🐜", "🪰", "🪲", "🪳", "🦟", "🦗", "🕷️", "🕸️", "🦂")),
        new EmojiCategory("🍔", Arrays.asList("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕")),
        new EmojiCategory("❤️", Arrays.asList("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐", "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "🆔", "⚛️", "🉑", "☢️", "☣️", "📴", "📳"))
    );

    public static class EmojiCategory {
        private final String name;
        private final List<String> emojis;
        public EmojiCategory(String name, List<String> emojis) {
            this.name = name;
            this.emojis = emojis;
        }
        public String getName() { return name; }
        public List<String> getEmojis() { return emojis; }
    }

    public enum KeyboardMode { ALPHABETIC, NUMBER, SYMBOL, CLIPBOARD, EDIT, QUICK_PHRASE, EMOJI }

    @Override
    public void onCreate() {
        super.onCreate();
        clipboardHistoryManager = new ink.wenmo.ime.clipboard.ClipboardManager();
        systemClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (systemClipboardManager != null) {
            clipListener = () -> {
                try {
                    ClipData primaryClip = systemClipboardManager.getPrimaryClip();
                    if (primaryClip != null && primaryClip.getItemCount() > 0) {
                        CharSequence text = primaryClip.getItemAt(0).getText();
                        if (text != null && !TextUtils.isEmpty(text.toString().trim())) {
                            clipboardHistoryManager.add(text.toString());
                            if (keyboardMode == KeyboardMode.CLIPBOARD) {
                                refresh();
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            };
            systemClipboardManager.addPrimaryClipChangedListener(clipListener);
        }
    }

    @Override
    public void onDestroy() {
        if (systemClipboardManager != null && clipListener != null) {
            systemClipboardManager.removePrimaryClipChangedListener(clipListener);
        }
        super.onDestroy();
    }

    @Override public View onCreateInputView() {
        if (engine == null) engine = new RustInputEngine(getApplicationContext());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(4), dp(4), dp(4), dp(6));
        root.setBackgroundColor(Color.rgb(231, 230, 226));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else {
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(dp(4), dp(4), dp(4), Math.max(dp(6), bottom + dp(4)));
            return insets;
        });

        LinearLayout toolbar = row();
        composition = new TextView(this);
        composition.setTextSize(16);
        composition.setTextColor(Color.rgb(32, 33, 36));
        composition.setTypeface(Typeface.DEFAULT_BOLD);
        composition.setGravity(Gravity.CENTER_VERTICAL);
        composition.setPadding(dp(8), 0, dp(6), 0);
        toolbar.addView(composition, new LinearLayout.LayoutParams(-2, dp(42)));

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        candidates = row();
        scroller.addView(candidates);
        toolbar.addView(scroller, new LinearLayout.LayoutParams(0, dp(42), 1));

        toolbar.addView(key("⋯", v -> toggleToolsBar()), fixed(38, 42));
        toolbar.addView(key("⌄", v -> requestHideSelf(0)), fixed(38, 42));
        root.addView(toolbar);

        toolsBar = row();
        toolsBar.setVisibility(View.GONE);
        toolsBar.setPadding(dp(2), dp(2), dp(2), dp(2));

        scriptToggle = key("简", v -> toggleScript());
        toolsBar.addView(scriptToggle, fixed(44, 38));
        toolsBar.addView(key("📋 剪贴板", v -> switchKeyboard(keyboardMode == KeyboardMode.CLIPBOARD ? KeyboardMode.ALPHABETIC : KeyboardMode.CLIPBOARD)), weightedKey());
        toolsBar.addView(key("✏️ 编辑", v -> switchKeyboard(keyboardMode == KeyboardMode.EDIT ? KeyboardMode.ALPHABETIC : KeyboardMode.EDIT)), weightedKey());
        toolsBar.addView(key("💬 常用语", v -> switchKeyboard(keyboardMode == KeyboardMode.QUICK_PHRASE ? KeyboardMode.ALPHABETIC : KeyboardMode.QUICK_PHRASE)), weightedKey());
        toolsBar.addView(key("😀 Emoji", v -> switchKeyboard(keyboardMode == KeyboardMode.EMOJI ? KeyboardMode.ALPHABETIC : KeyboardMode.EMOJI)), weightedKey());
        root.addView(toolsBar);

        keyboardPanel = new LinearLayout(this);
        keyboardPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(keyboardPanel, new LinearLayout.LayoutParams(-1, -2));
        showKeyboard(keyboardMode);
        refresh();
        return root;
    }

    @Override public void onStartInput(android.view.inputmethod.EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        if (engine == null) engine = new RustInputEngine(getApplicationContext());
        engine.clear();
        numberComposition.setLength(0);
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        if (inputClass == InputType.TYPE_CLASS_NUMBER
                || inputClass == InputType.TYPE_CLASS_PHONE
                || inputClass == InputType.TYPE_CLASS_DATETIME) {
            showKeyboard(KeyboardMode.NUMBER);
        } else {
            showKeyboard(KeyboardMode.ALPHABETIC);
        }
        refresh();
    }

    private void type(char letter) { engine.type(letter); refresh(); }

    private void select(String value) {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.commitText(value, 1);
        engine.clear();
        refresh();
    }

    private void space() {
        if (keyboardMode == KeyboardMode.NUMBER && numberComposition.length() > 0) {
            String eval = CalculatorEngine.evaluate(numberComposition.toString());
            if (eval != null) commitRaw(eval);
            else commitRaw(numberComposition.toString());
            numberComposition.setLength(0);
            refresh();
            return;
        }
        if (!engine.candidates().isEmpty()) select(engine.candidates().get(0));
        else if (!engine.composition().isEmpty()) select(engine.composition());
        else commitRaw(" ");
    }

    private void backspace() {
        if (!engine.composition().isEmpty()) { engine.backspace(); refresh(); return; }
        if (keyboardMode == KeyboardMode.NUMBER && numberComposition.length() > 0) {
            numberComposition.deleteCharAt(numberComposition.length() - 1);
            refresh();
            return;
        }
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.deleteSurroundingText(1, 0);
    }

    private void enter() {
        if (!engine.composition().isEmpty()) space();
        else if (keyboardMode == KeyboardMode.NUMBER && numberComposition.length() > 0) {
            commitRaw(numberComposition.toString());
            numberComposition.setLength(0);
            refresh();
        }
        else sendDefaultEditorAction(true);
    }

    private void commitRaw(String value) {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.commitText(value, 1);
    }

    private void typeNumberChar(String val) {
        numberComposition.append(val);
        refresh();
    }

    private void sendKeyEvent(int keyCode) {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) {
            connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        }
    }

    private void switchKeyboard(KeyboardMode mode) {
        if (keyboardMode == KeyboardMode.ALPHABETIC && mode != KeyboardMode.ALPHABETIC) {
            if (!engine.candidates().isEmpty()) select(engine.candidates().get(0));
            else if (!engine.composition().isEmpty()) select(engine.composition());
        }
        showKeyboard(mode);
        refresh();
    }

    private void showKeyboard(KeyboardMode mode) {
        keyboardMode = mode;
        if (keyboardPanel == null) return;
        keyboardPanel.removeAllViews();
        switch (mode) {
            case ALPHABETIC -> buildAlphabeticKeyboard();
            case NUMBER -> buildNumberKeyboard();
            case SYMBOL -> buildSymbolCategoryKeyboard();
            case CLIPBOARD -> buildClipboardPanel();
            case EDIT -> buildEditPanel();
            case QUICK_PHRASE -> buildQuickPhrasePanel();
            case EMOJI -> buildEmojiCategoryKeyboard();
        }
    }

    private void buildAlphabeticKeyboard() {
        for (String letters : new String[]{"qwertyuiop", "asdfghjkl", "zxcvbnm"}) {
            LinearLayout keyRow = row();
            for (char letter : letters.toCharArray()) {
                keyRow.addView(key(String.valueOf(letter), v -> type(letter)), weightedKey());
            }
            keyboardPanel.addView(keyRow);
        }
        LinearLayout bottom = row();
        bottom.addView(key("🌐", v -> switchToNextInputMethod(false)), fixed(46, 48));
        bottom.addView(key("123", v -> switchKeyboard(KeyboardMode.NUMBER)), fixed(50, 48));
        bottom.addView(key("符", v -> switchKeyboard(KeyboardMode.SYMBOL)), fixed(46, 48));
        bottom.addView(key("空格", v -> space()), weightedKey());
        bottom.addView(key("⌫", v -> backspace()), fixed(52, 48));
        bottom.addView(key("回车", v -> enter()), fixed(60, 48));
        keyboardPanel.addView(bottom);
    }

    private void buildNumberKeyboard() {
        addNumberKeyRow("1", "2", "3", "+");
        addNumberKeyRow("4", "5", "6", "-");
        addNumberKeyRow("7", "8", "9", "×");
        addNumberKeyRow(".", "0", "=", "÷");

        LinearLayout bottom = row();
        bottom.addView(key("ABC", v -> switchKeyboard(KeyboardMode.ALPHABETIC)), fixed(62, 48));
        bottom.addView(key("符", v -> switchKeyboard(KeyboardMode.SYMBOL)), fixed(58, 48));
        bottom.addView(key("空格", v -> space()), weightedKey());
        bottom.addView(key("⌫", v -> backspace()), fixed(58, 48));
        bottom.addView(key("回车", v -> enter()), fixed(68, 48));
        keyboardPanel.addView(bottom);
    }

    private void addNumberKeyRow(String... labels) {
        LinearLayout keyRow = row();
        for (String label : labels) {
            if (label.equals("=")) {
                keyRow.addView(key(label, v -> {
                    String eval = CalculatorEngine.evaluate(numberComposition.toString());
                    if (eval != null) {
                        commitRaw(eval);
                        numberComposition.setLength(0);
                        refresh();
                    } else {
                        typeNumberChar(label);
                    }
                }), weightedKey());
            } else {
                keyRow.addView(key(label, v -> typeNumberChar(label)), weightedKey());
            }
        }
        keyboardPanel.addView(keyRow);
    }

    private void buildSymbolCategoryKeyboard() {
        HorizontalScrollView tabScroller = new HorizontalScrollView(this);
        tabScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout tabLayout = row();
        var categories = SymbolData.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            final int index = i;
            SymbolData.SymbolCategory category = categories.get(i);
            Button tabBtn = key(category.getName(), v -> {
                selectedSymbolCategoryIndex = index;
                showKeyboard(KeyboardMode.SYMBOL);
            });
            if (i == selectedSymbolCategoryIndex) {
                tabBtn.setTextColor(Color.rgb(26, 115, 232));
                tabBtn.setTypeface(Typeface.DEFAULT_BOLD);
            }
            tabLayout.addView(tabBtn, fixed(54, 40));
        }
        tabScroller.addView(tabLayout);
        keyboardPanel.addView(tabScroller);

        ScrollView symbolScroller = new ScrollView(this);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        var currentSymbols = categories.get(selectedSymbolCategoryIndex).getSymbols();
        int columns = 7;
        for (int i = 0; i < currentSymbols.size(); i += columns) {
            LinearLayout symbolRow = row();
            for (int j = 0; j < columns && (i + j) < currentSymbols.size(); j++) {
                final String sym = currentSymbols.get(i + j);
                symbolRow.addView(key(sym, v -> commitRaw(sym)), weightedKey());
            }
            grid.addView(symbolRow);
        }
        symbolScroller.addView(grid);
        keyboardPanel.addView(symbolScroller, new LinearLayout.LayoutParams(-1, dp(150)));

        LinearLayout bottom = row();
        bottom.addView(key("ABC", v -> switchKeyboard(KeyboardMode.ALPHABETIC)), fixed(62, 44));
        bottom.addView(key("123", v -> switchKeyboard(KeyboardMode.NUMBER)), fixed(58, 44));
        bottom.addView(key("空格", v -> commitRaw(" ")), weightedKey());
        bottom.addView(key("⌫", v -> backspace()), fixed(58, 44));
        bottom.addView(key("回车", v -> enter()), fixed(68, 44));
        keyboardPanel.addView(bottom);
    }

    private void buildEditPanel() {
        LinearLayout row1 = row();
        row1.addView(key("↑", v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP)), weightedKey());
        row1.addView(key("全选", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.performContextMenuAction(android.R.id.selectAll);
        }), weightedKey());
        row1.addView(key("复制", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.performContextMenuAction(android.R.id.copy);
        }), weightedKey());
        keyboardPanel.addView(row1);

        LinearLayout row2 = row();
        row2.addView(key("←", v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)), weightedKey());
        row2.addView(key("↓", v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN)), weightedKey());
        row2.addView(key("→", v -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)), weightedKey());
        keyboardPanel.addView(row2);

        LinearLayout row3 = row();
        row3.addView(key("剪切", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.performContextMenuAction(android.R.id.cut);
        }), weightedKey());
        row3.addView(key("粘贴", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.performContextMenuAction(android.R.id.paste);
        }), weightedKey());
        row3.addView(key("清空", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.performContextMenuAction(android.R.id.selectAll);
                ic.commitText("", 1);
            }
        }), weightedKey());
        keyboardPanel.addView(row3);

        LinearLayout bottom = row();
        bottom.addView(key("返回键盘", v -> switchKeyboard(KeyboardMode.ALPHABETIC)), weightedKey());
        keyboardPanel.addView(bottom);
    }

    private void buildQuickPhrasePanel() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(4), dp(8), dp(4));

        for (String phrase : quickPhrases) {
            Button itemBtn = new Button(this);
            itemBtn.setText(phrase);
            itemBtn.setAllCaps(false);
            itemBtn.setTextSize(14);
            itemBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            itemBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
            itemBtn.setOnClickListener(v -> {
                commitRaw(phrase);
                switchKeyboard(KeyboardMode.ALPHABETIC);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, dp(2), 0, dp(2));
            list.addView(itemBtn, lp);
        }
        scrollView.addView(list);

        LinearLayout bottomControl = row();
        bottomControl.addView(key("返回", v -> switchKeyboard(KeyboardMode.ALPHABETIC)), fixed(80, 44));

        keyboardPanel.addView(scrollView, new LinearLayout.LayoutParams(-1, dp(150)));
        keyboardPanel.addView(bottomControl);
    }

    private void buildEmojiCategoryKeyboard() {
        HorizontalScrollView tabScroller = new HorizontalScrollView(this);
        tabScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout tabLayout = row();
        for (int i = 0; i < emojiCategories.size(); i++) {
            final int index = i;
            EmojiCategory category = emojiCategories.get(i);
            Button tabBtn = key(category.getName(), v -> {
                selectedEmojiCategoryIndex = index;
                showKeyboard(KeyboardMode.EMOJI);
            });
            if (i == selectedEmojiCategoryIndex) {
                tabBtn.setTextColor(Color.rgb(26, 115, 232));
                tabBtn.setTypeface(Typeface.DEFAULT_BOLD);
            }
            tabLayout.addView(tabBtn, fixed(54, 40));
        }
        tabScroller.addView(tabLayout);
        keyboardPanel.addView(tabScroller);

        ScrollView emojiScroller = new ScrollView(this);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        var currentEmojis = emojiCategories.get(selectedEmojiCategoryIndex).getEmojis();
        int columns = 7;
        for (int i = 0; i < currentEmojis.size(); i += columns) {
            LinearLayout emojiRow = row();
            for (int j = 0; j < columns && (i + j) < currentEmojis.size(); j++) {
                final String emoji = currentEmojis.get(i + j);
                emojiRow.addView(key(emoji, v -> commitRaw(emoji)), weightedKey());
            }
            grid.addView(emojiRow);
        }
        emojiScroller.addView(grid);
        keyboardPanel.addView(emojiScroller, new LinearLayout.LayoutParams(-1, dp(150)));

        LinearLayout bottom = row();
        bottom.addView(key("ABC", v -> switchKeyboard(KeyboardMode.ALPHABETIC)), fixed(62, 44));
        bottom.addView(key("123", v -> switchKeyboard(KeyboardMode.NUMBER)), fixed(58, 44));
        bottom.addView(key("空格", v -> commitRaw(" ")), weightedKey());
        bottom.addView(key("⌫", v -> backspace()), fixed(58, 44));
        bottom.addView(key("回车", v -> enter()), fixed(68, 44));
        keyboardPanel.addView(bottom);
    }

    private void buildClipboardPanel() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(4), dp(8), dp(4));

        var history = clipboardHistoryManager.getHistory();
        if (history.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("剪贴板空空如也");
            emptyView.setTextColor(Color.GRAY);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, dp(32), 0, dp(32));
            list.addView(emptyView, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (int i = 0; i < history.size(); i++) {
                final String text = history.get(i).getText();
                Button clipItem = new Button(this);
                clipItem.setText(text);
                clipItem.setAllCaps(false);
                clipItem.setTextSize(14);
                clipItem.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                clipItem.setPadding(dp(12), dp(8), dp(12), dp(8));
                clipItem.setOnClickListener(v -> {
                    commitRaw(text);
                    switchKeyboard(KeyboardMode.ALPHABETIC);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, dp(2), 0, dp(2));
                list.addView(clipItem, lp);
            }
        }
        scrollView.addView(list);

        LinearLayout bottomControl = row();
        bottomControl.addView(key("清空剪贴板", v -> {
            clipboardHistoryManager.clear();
            showKeyboard(KeyboardMode.CLIPBOARD);
        }), new LinearLayout.LayoutParams(0, dp(44), 1));
        bottomControl.addView(key("返回", v -> switchKeyboard(KeyboardMode.ALPHABETIC)), fixed(80, 44));

        keyboardPanel.addView(scrollView, new LinearLayout.LayoutParams(-1, dp(150)));
        keyboardPanel.addView(bottomControl);
    }

    private void toggleToolsBar() {
        if (toolsBar != null) {
            toolsBar.setVisibility(toolsBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }

    private void toggleScript() {
        engine.setTraditional(!engine.isTraditional());
        refresh();
    }

    private void refresh() {
        if (composition == null || candidates == null) return;
        if (keyboardMode == KeyboardMode.ALPHABETIC) composition.setText(engine.composition());
        else if (keyboardMode == KeyboardMode.NUMBER) composition.setText(numberComposition.length() > 0 ? numberComposition.toString() : "数字");
        else if (keyboardMode == KeyboardMode.SYMBOL) composition.setText("符号");
        else if (keyboardMode == KeyboardMode.CLIPBOARD) composition.setText("剪贴板");
        else if (keyboardMode == KeyboardMode.EDIT) composition.setText("编辑");
        else if (keyboardMode == KeyboardMode.QUICK_PHRASE) composition.setText("常用语");
        else if (keyboardMode == KeyboardMode.EMOJI) composition.setText("Emoji");
        scriptToggle.setText(engine.isTraditional() ? "繁" : "简");

        candidates.removeAllViews();
        if (keyboardMode == KeyboardMode.ALPHABETIC) {
            for (String candidate : engine.candidates()) {
                Button btn = key(candidate, v -> select(candidate));
                btn.setPadding(dp(10), 0, dp(10), 0);
                btn.setMinimumWidth(dp(44));
                candidates.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
            }
        } else if (keyboardMode == KeyboardMode.NUMBER && numberComposition.length() > 0) {
            String evalResult = CalculatorEngine.evaluate(numberComposition.toString());
            if (evalResult != null) {
                Button btn = key("= " + evalResult, v -> {
                    commitRaw(evalResult);
                    numberComposition.setLength(0);
                    refresh();
                });
                btn.setPadding(dp(10), 0, dp(10), 0);
                candidates.addView(btn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
            }
        }
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private Button key(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams fixed(int width, int height) {
        return new LinearLayout.LayoutParams(dp(width), dp(height));
    }

    private LinearLayout.LayoutParams weightedKey() {
        return new LinearLayout.LayoutParams(0, dp(48), 1);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
