package ink.wenmo.ime.calculator;

import org.junit.Assert;
import org.junit.Test;

public class CalculatorEngineTest {

    @Test
    public void testBasicArithmetic() {
        Assert.assertEquals("5", CalculatorEngine.evaluate("2+3"));
        Assert.assertEquals("10", CalculatorEngine.evaluate("15-5"));
        Assert.assertEquals("100", CalculatorEngine.evaluate("12.5*8"));
        Assert.assertEquals("2.5", CalculatorEngine.evaluate("10/4"));
    }

    @Test
    public void testUnicodeOperators() {
        Assert.assertEquals("100", CalculatorEngine.evaluate("12.5×8"));
        Assert.assertEquals("2.5", CalculatorEngine.evaluate("10÷4"));
    }

    @Test
    public void testParenthesesAndPrecedence() {
        Assert.assertEquals("14", CalculatorEngine.evaluate("2+3*4"));
        Assert.assertEquals("20", CalculatorEngine.evaluate("(2+3)*4"));
    }

    @Test
    public void testInvalidExpressions() {
        Assert.assertNull(CalculatorEngine.evaluate(""));
        Assert.assertNull(CalculatorEngine.evaluate("hello"));
        Assert.assertNull(CalculatorEngine.evaluate("10/0"));
    }
}
