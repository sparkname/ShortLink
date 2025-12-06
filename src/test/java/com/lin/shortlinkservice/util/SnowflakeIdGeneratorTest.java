package com.lin.shortlinkservice.util;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 雪花算法测试
 */
class SnowflakeIdGeneratorTest {
    
    @Test
    void testGenerateId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        long id = generator.nextId();
        assertTrue(id > 0, "ID应该大于0");
    }
    
    @Test
    void testUniqueIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        Set<Long> ids = new HashSet<>();
        
        // 生成10000个ID,检查是否有重复
        for (int i = 0; i < 10000; i++) {
            long id = generator.nextId();
            assertFalse(ids.contains(id), "ID不应该重复: " + id);
            ids.add(id);
        }
        
        assertEquals(10000, ids.size(), "应该生成10000个唯一ID");
    }
    
    @Test
    void testIdIncreasing() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        long prevId = generator.nextId();
        
        // 检查连续生成的ID是递增的
        for (int i = 0; i < 100; i++) {
            long currentId = generator.nextId();
            assertTrue(currentId > prevId, "ID应该递增");
            prevId = currentId;
        }
    }
}
