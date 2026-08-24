package ink.wenmo.ime.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 离线轻量计算器引擎。
 * 支持在数字输入模式下识别和计算简单算术表达式（加减乘除与小括号），
 * 无外部依赖且完全离线。
 */
public final class CalculatorEngine {

    public static String evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        String cleanExpr = expression.replace("×", "*").replace("÷", "/").replace(" ", "");
        if (!cleanExpr.matches(".*[0-9].*") || !cleanExpr.matches(".*[\\+\\-\\*/].*")) {
            return null;
        }

        try {
            List<String> tokens = tokenize(cleanExpr);
            if (tokens.isEmpty()) return null;
            List<String> rpn = infixToRPN(tokens);
            BigDecimal result = evalRPN(rpn);
            if (result == null) return null;

            // 格式化输出：去除末尾不必要的零
            String resultStr = result.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            return resultStr;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder numberBuffer = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                numberBuffer.append(c);
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')') {
                if (numberBuffer.length() > 0) {
                    tokens.add(numberBuffer.toString());
                    numberBuffer.setLength(0);
                }
                // 处理一元负号/正号
                if ((c == '-' || c == '+') && (tokens.isEmpty() || tokens.get(tokens.size() - 1).equals("("))) {
                    numberBuffer.append(c);
                } else {
                    tokens.add(String.valueOf(c));
                }
            } else {
                // 包含无效字符
                return new ArrayList<>();
            }
        }
        if (numberBuffer.length() > 0) {
            tokens.add(numberBuffer.toString());
        }
        return tokens;
    }

    private static List<String> infixToRPN(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (!stack.isEmpty() && stack.peek().equals("(")) {
                    stack.pop();
                }
            } else if (isOperator(token)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) && precedence(stack.peek()) >= precedence(token)) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }
        while (!stack.isEmpty()) {
            output.add(stack.pop());
        }
        return output;
    }

    private static BigDecimal evalRPN(List<String> rpn) {
        Stack<BigDecimal> stack = new Stack<>();

        for (String token : rpn) {
            if (isNumber(token)) {
                stack.push(new BigDecimal(token));
            } else if (isOperator(token)) {
                if (stack.size() < 2) return null;
                BigDecimal b = stack.pop();
                BigDecimal a = stack.pop();
                switch (token) {
                    case "+": stack.push(a.add(b)); break;
                    case "-": stack.push(a.subtract(b)); break;
                    case "*": stack.push(a.multiply(b)); break;
                    case "/":
                        if (b.compareTo(BigDecimal.ZERO) == 0) return null;
                        stack.push(a.divide(b, 10, RoundingMode.HALF_UP));
                        break;
                }
            }
        }
        return stack.size() == 1 ? stack.pop() : null;
    }

    private static boolean isNumber(String token) {
        try {
            new BigDecimal(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
    }

    private static int precedence(String op) {
        if (op.equals("*") || op.equals("/")) return 2;
        if (op.equals("+") || op.equals("-")) return 1;
        return 0;
    }
}
