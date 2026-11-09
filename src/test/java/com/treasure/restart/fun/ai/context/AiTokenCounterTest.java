package com.treasure.restart.fun.ai.context;

import com.treasure.restart.func.ai.context.AiTokenCounter;
import com.treasure.restart.func.ai.context.AiTokenCounterProd;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class AiTokenCounterTest {

    private static AiTokenCounterProd tokenCounter;

    @BeforeAll
    static void setUp() {
        tokenCounter = new AiTokenCounterProd("./tokenizers/v41/tokenizer.json");
    }

    @AfterAll
    static void tearDown() {
        if (tokenCounter != null) {
            tokenCounter.close();
        }
    }

    @Test
    void emptyTextShouldHaveZeroTokens() {
        assertEquals(0, tokenCounter.count(""));
        assertEquals(0, tokenCounter.count(null));
    }

    @Test
    void englishTextShouldHaveTokens() {
        int count = tokenCounter.count("Hello, world!");

        assertTrue(count > 0);
        System.out.println("英文 Token 数量：" + count);
    }

    @Test
    void chineseTextShouldHaveTokens() {
        int count = tokenCounter.count("你好，我正在学习 AI Agent 开发。");

        assertTrue(count > 0);
        System.out.println("中文 Token 数量：" + count);
    }

    @Test
    void sameTextShouldReturnSameCount() {
        String text = "Spring Boot + DeepSeek";

        assertEquals(
                tokenCounter.count(text),
                tokenCounter.count(text)
        );
    }
}