package ink.wenmo.ime.clipboard;

import org.junit.Assert;
import org.junit.Test;

public class ClipboardManagerTest {

    @Test
    public void testAddAndGetHistory() {
        ClipboardManager manager = new ClipboardManager(10);
        manager.add("Hello");
        manager.add("World");

        Assert.assertEquals(2, manager.size());
        Assert.assertEquals("World", manager.getHistory().get(0).getText());
        Assert.assertEquals("Hello", manager.getHistory().get(1).getText());
    }

    @Test
    public void testDeduplication() {
        ClipboardManager manager = new ClipboardManager(10);
        manager.add("Hello");
        manager.add("World");
        manager.add("Hello");

        Assert.assertEquals(2, manager.size());
        Assert.assertEquals("Hello", manager.getHistory().get(0).getText());
        Assert.assertEquals("World", manager.getHistory().get(1).getText());
    }

    @Test
    public void testMaxCapacity() {
        ClipboardManager manager = new ClipboardManager(3);
        manager.add("A");
        manager.add("B");
        manager.add("C");
        manager.add("D");

        Assert.assertEquals(3, manager.size());
        Assert.assertEquals("D", manager.getHistory().get(0).getText());
        Assert.assertEquals("C", manager.getHistory().get(1).getText());
        Assert.assertEquals("B", manager.getHistory().get(2).getText());
    }

    @Test
    public void testRemoveAndClear() {
        ClipboardManager manager = new ClipboardManager(10);
        manager.add("Item 1");
        manager.add("Item 2");
        manager.remove(0);

        Assert.assertEquals(1, manager.size());
        Assert.assertEquals("Item 1", manager.getHistory().get(0).getText());

        manager.clear();
        Assert.assertEquals(0, manager.size());
    }
}
