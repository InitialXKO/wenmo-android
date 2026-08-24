package ink.wenmo.ime.data;

import org.junit.Assert;
import org.junit.Test;
import java.util.List;

public class SymbolDataTest {

    @Test
    public void testSymbolCategories() {
        List<SymbolData.SymbolCategory> categories = SymbolData.getCategories();
        Assert.assertNotNull(categories);
        Assert.assertFalse(categories.isEmpty());

        for (SymbolData.SymbolCategory category : categories) {
            Assert.assertNotNull(category.getName());
            Assert.assertNotNull(category.getId());
            Assert.assertNotNull(category.getSymbols());
            Assert.assertFalse(category.getSymbols().isEmpty());
        }
    }
}
