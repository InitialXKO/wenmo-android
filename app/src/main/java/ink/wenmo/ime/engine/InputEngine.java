package ink.wenmo.ime.engine;

import java.util.List;

public interface InputEngine {
    void type(char value);
    void backspace();
    void clear();
    String composition();
    List<String> candidates();
    void setTraditional(boolean traditional);
    boolean isTraditional();

    default DecodeResult[] decodeSentence(String input, int extra) {
        return new DecodeResult[0];
    }

    default DecodeResult[] decodeNumSentence(String prefixLetters, String digits, int extra) {
        return new DecodeResult[0];
    }

    default DecodeResult[] nextTokens(int[] contextIds, int limit, boolean enOnly) {
        return new DecodeResult[0];
    }

    default DecodeResult[] getTokens(String prefix, int limit, boolean enOnly) {
        return new DecodeResult[0];
    }

    default String[] t9PinyinSyllables(String digits, int limit) {
        return new String[0];
    }

    default void learnUserSentence(int[] context, int[] tokens) {}

    default void flushUserSentence() {}

    default void resetCaches() {}
}
