package com.treasure.restart.func.ai.context;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.treasure.restart.helper.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AiTokenCounterProd {
    private final HuggingFaceTokenizer tokenizer;

    public AiTokenCounterProd(@Value("${deepseek.tokenizer.path:./tokenizers/v41/tokenizer.json}") String tokenizerPath) {
        Path path = Path.of(tokenizerPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) throw new BusinessException("DeepSeek Tokenizer 文件不存在：" + path);
        try {
            this.tokenizer = HuggingFaceTokenizer.newInstance(path);
        } catch (IOException e) {
            throw new BusinessException("DeepSeek Tokenizer 加载失败：" + path + e);
        }
    }

    public int count(String text) {
        if (text == null || text.isEmpty()) return 0;
        Encoding encode = tokenizer.encode(text);
        return encode.getIds().length;
    }

    @PreDestroy
    public void close() {
        tokenizer.close();
    }
}
