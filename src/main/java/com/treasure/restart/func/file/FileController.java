package com.treasure.restart.func.file;

import com.treasure.restart.base.BaseResponse;
import com.treasure.restart.helper.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final String accessUrl;
    public FileController(
            @Value("${file.upload-path}") String uploadPath,
            @Value("${file.access-url}") String accessUrl
    ){
        this.uploadPath = uploadPath;
        this.accessUrl = accessUrl;
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

                wholeUrls.add(accessUrl + fileName);

            } catch (IOException e) {
                throw new BusinessException(500, "文件上传失败");
            }
        }

        return BaseResponse.success("上传成功", wholeUrls);
    }
}
