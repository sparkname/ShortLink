package com.lin.shortlinkservice.util;

/**
 * Base62 编码工具类
 * 将数字ID转换为短字符串(0-9, a-z, A-Z)
 * 这是面试核心算法!
 */
public class Base62Util {
    
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    
    /**
     * 将数字ID编码为Base62字符串
     * 例如: 10000 -> "2Bi"
     * 
     * @param id 数字ID
     * @return Base62编码字符串
     */
    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }
        
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(BASE62_CHARS.charAt(remainder));
            id = id / BASE;
        }
        
        // 反转字符串,因为我们是从低位到高位计算的
        return sb.reverse().toString();
    }
    
    /**
     * 将Base62字符串解码为数字ID
     * 例如: "2Bi" -> 10000
     * 
     * @param encoded Base62编码字符串
     * @return 数字ID
     */
    public static long decode(String encoded) {
        long id = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int digit = BASE62_CHARS.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            id = id * BASE + digit;
        }
        return id;
    }
}
