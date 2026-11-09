package com.treasure.restart.func.ai.context;

import org.springframework.stereotype.Component;

/**
 * 字符串
 * ↓
 * 估算 Token 数
 */
@Component
public class AiTokenCounter {

    /**
     * 第一版 Token 估算器。
     * <p>
     * 注意：
     * 这不是模型官方 tokenizer。
     * 当前只用于上下文预算控制。
     */
    public int count(String text) {

        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chinese = 0;
        int ascii = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isChinese(c)) {
                chinese++;
            } else {
                ascii++;
            }
        }

        // 中文：先按 1 字符 ≈ 1 Token 估算
        // 英文/数字/符号：先按 4 字符 ≈ 1 Token 估算
        return chinese + (ascii + 3) / 4;
    }

    private boolean isChinese(char c) {
        return c >= '\u4E00' && c <= '\u9FFF';
    }
}
