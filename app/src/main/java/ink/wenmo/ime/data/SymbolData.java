package ink.wenmo.ime.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SymbolData {
    public static class SymbolCategory {
        private final String name;
        private final String id;
        private final List<String> symbols;

        public SymbolCategory(String name, String id, List<String> symbols) {
            this.name = name;
            this.id = id;
            this.symbols = symbols;
        }

        public String getName() { return name; }
        public String getId() { return id; }
        public List<String> getSymbols() { return symbols; }
    }

    private static final List<SymbolCategory> CATEGORIES = new ArrayList<>();

    static {
        CATEGORIES.add(new SymbolCategory("中", "cnPunctuation", Arrays.asList(
            "。", "，", "、", "：", "；", "‘", "’", "“", "”",
            "？", "！", "～", "—", "｜", "‖", "﹏", "（", "）",
            "【", "】", "《", "》", "「", "」", "『", "』", "〔", "〕"
        )));

        CATEGORIES.add(new SymbolCategory("英", "enPunctuation", Arrays.asList(
            ".", ",", ";", ":", "!", "?", "'", "\"",
            "@", "#", "$", "%", "^", "&", "*", "-",
            "_", "=", "+", "~", "`", "|", "\\", "/",
            "(", ")", "[", "]", "{", "}", "<", ">"
        )));

        CATEGORIES.add(new SymbolCategory("数", "mathSymbols", Arrays.asList(
            "+", "-", "×", "÷", "=", "≠", "≈", "±",
            "<", ">", "≤", "≥", "∞", "√", "π", "∫",
            "∑", "∈", "⊂", "⊃", "∪", "∩", "⊥", "∠", "∴", "∵"
        )));

        CATEGORIES.add(new SymbolCategory("①", "numberedSymbols", Arrays.asList(
            "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
            "❶", "❷", "❸", "❹", "❺", "❻", "❼", "❽", "❾", "❿",
            "㈠", "㈡", "㈢", "㈣", "㈤", "㈥", "㈦", "㈧", "㈨", "㈩",
            "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ"
        )));

        CATEGORIES.add(new SymbolCategory("Xⁿ", "scripts", Arrays.asList(
            "⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹", "⁺", "⁻", "⁼", "⁽", "⁾",
            "₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉", "₊", "₋", "₌", "₍", "₎"
        )));

        CATEGORIES.add(new SymbolCategory("◓", "graphical", Arrays.asList(
            "★", "☆", "✦", "✧", "♥", "❤", "❥", "ღ", "☑", "✓", "✔", "✘", "✕",
            "☀", "☁", "☂", "☃", "♠", "♣", "♦", "●", "○", "■", "□", "▲", "▼"
        )));

        CATEGORIES.add(new SymbolCategory("⇌", "arrows", Arrays.asList(
            "←", "↑", "→", "↓", "↖", "↗", "↘", "↙",
            "↔", "↕", "⇄", "⇅", "⇐", "⇑", "⇒", "⇓", "⇔"
        )));

        CATEGORIES.add(new SymbolCategory("¥", "currency", Arrays.asList(
            "￥", "$", "€", "£", "HK$", "NT$", "₩", "₽", "฿", "¢", "₭"
        )));

        CATEGORIES.add(new SymbolCategory("δ", "greek", Arrays.asList(
            "α", "β", "γ", "δ", "ε", "ζ", "η", "θ", "ι", "κ", "λ", "μ", "ν", "ξ", "ο", "π", "ρ", "σ", "τ", "υ", "φ", "χ", "ψ", "ω",
            "Α", "Β", "Γ", "Δ", "Ε", "Ζ", "Η", "Θ", "Ι", "Κ", "Λ", "Μ", "Ν", "Ξ", "Ο", "Π", "Ρ", "Σ", "Τ", "Υ", "Φ", "Χ", "Ψ", "Ω"
        )));

        CATEGORIES.add(new SymbolCategory("ㄎ", "pinyinZhuyin", Arrays.asList(
            "ā", "á", "ǎ", "à", "ō", "ó", "ǒ", "ò", "ē", "é", "ě", "è",
            "ī", "í", "ǐ", "ì", "ū", "ú", "ǔ", "ù", "ǖ", "ǘ", "ǚ", "ǜ",
            "ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ"
        )));
    }

    public static List<SymbolCategory> getCategories() {
        return Collections.unmodifiableList(CATEGORIES);
    }
}
