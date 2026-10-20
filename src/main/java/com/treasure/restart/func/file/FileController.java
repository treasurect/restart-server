package com.treasure.restart.func.file;

import com.treasure.restart.base.BaseResponse;
import com.treasure.restart.entity.HistoryItem;
import com.treasure.restart.helper.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final String uploadPath;
    private final String uploadDir;
    private final ObjectMapper objectMapper;

    public FileController(
            @Value("${file.upload-path}") String uploadPath,
            @Value("${file.upload-dir}") String uploadDir,
            ObjectMapper objectMapper
    ){
        this.uploadPath = uploadPath;
        this.uploadDir = uploadDir;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/upload")
    public BaseResponse<List<String>> upload(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        List<String> wholeUrls = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                continue;
            }

            try {
                Path path = Paths.get(uploadPath);

                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }

                String originalFilename = file.getOriginalFilename();

                String suffix = "";

                if (originalFilename != null) {
                    int index = originalFilename.lastIndexOf(".");
                    if (index != -1) {
                        suffix = originalFilename.substring(index);
                    }
                }

                String fileName = UUID.randomUUID() + suffix;

                Path target = path.resolve(fileName);

                file.transferTo(target);

                wholeUrls.add(uploadDir + fileName);

            } catch (IOException e) {
                throw new BusinessException(500, "文件上传失败");
            }
        }

        return BaseResponse.success("上传成功", wholeUrls);
    }

    @GetMapping("/history/{region}")
    public BaseResponse<List<HistoryItem>> getHistoryList(@PathVariable String region) {
        String historyPath = switch (region) {
            case "china" ->  "/opt/restart-server/data/history_china.json";
            case "europe" ->  "/opt/restart-server/data/history_europe.json";
            case "usa" ->  "/opt/restart-server/data/history_usa.json";
            case "russia" ->  "/opt/restart-server/data/history_russia.json";
            default -> throw new BusinessException(404,"不支持的数据类型");
        };
        try {
            Path path = Paths.get(historyPath);
            if (!path.toFile().exists()) throw  new BusinessException(404,"历史数据文件不存在");
            List<HistoryItem> historyItems = objectMapper.readValue(path.toFile(), new TypeReference<List<HistoryItem>>() {
            });
            return BaseResponse.success("获取历史数据成功",historyItems);
        } catch (Exception e) {
            throw new BusinessException(500,"获取历史数据失败");
        }
    }
}
