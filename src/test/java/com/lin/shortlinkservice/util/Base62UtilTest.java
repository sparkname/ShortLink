package com.lin.shortlinkservice.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base62工具类测试
 */
class Base62UtilTest {
    
    @Test
    void testEncode() {
        assertEquals("0", Base62Util.encode(0));
        assertEquals("1", Base62Util.encode(1));
        assertEquals("a", Base62Util.encode(10));
        assertEquals("A", Base62Util.encode(36));
        assertEquals("10", Base62Util.encode(62));
        assertEquals("2Bi", Base62Util.encode(10000));
    }
    
    @Test
    void testDecode() {
        assertEquals(0, Base62Util.decode("0"));
        assertEquals(1, Base62Util.decode("1"));
        assertEquals(10, Base62Util.decode("a"));
        assertEquals(36, Base62Util.decode("A"));
        assertEquals(62, Base62Util.decode("10"));
        assertEquals(10000, Base62Util.decode("2Bi"));
    }
    
    @Test
    void testEncodeDecode() {
        long[] testIds = {0, 1, 100, 1000, 10000, 100000, 1000000, Long.MAX_VALUE};
        
        for (long id : testIds) {
            String encoded = Base62Util.encode(id);
            long decoded = Base62Util.decode(encoded);
            assertEquals(id, decoded, "ID: " + id + " should match after encode/decode");
        }
    }
}
