package x2oracle;

import org.junit.Test;

import static org.junit.Assert.*;

public class RetrievalControllerTest {
    @Test
    public void testTest() {
        assertEquals("Test query succeeded.", RetrievalController.test(), "Test query");
    }
}
