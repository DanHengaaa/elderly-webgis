package com.hhu.elderly.institution.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/institution-console/upload")
public class InstitutionUploadController {

    private static final String BASE_DIR = "D:/Elderly/uploads/institution";

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024L;

    /**
     * 机构展示图片上传。
     * 用于封面图片、机构相册。
     */
    @PostMapping("/image")
    public Map<String, Object> uploadImage(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return upload(
                file,
                "images",
                "\\.(jpg|jpeg|png|webp|gif)$",
                MAX_IMAGE_SIZE,
                "仅支持 jpg、jpeg、png、webp、gif 图片"
        );
    }

    /**
     * 机构资质材料上传。
     * 用于营业执照、备案证明、其他材料。
     * 支持主流图片、PDF、Word、Excel、PPT、TXT、RTF 文件。
     */
    @PostMapping("/file")
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return upload(
                file,
                "materials",
                "\\.(jpg|jpeg|png|webp|gif|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf)$",
                MAX_FILE_SIZE,
                "仅支持图片、PDF、Word、Excel、PPT、TXT、RTF 等常见文件类型"
        );
    }

    private Map<String, Object> upload(
            MultipartFile file,
            String subFolder,
            String suffixRegex,
            long maxSize,
            String typeErrorMessage
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "文件为空，请重新选择文件"
            );
        }

        if (file.getSize() > maxSize) {
            return Map.of(
                    "success", false,
                    "message", "文件过大，请重新选择文件"
            );
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename
                    .substring(originalFilename.lastIndexOf("."))
                    .toLowerCase();
        }

        if (!suffix.matches(suffixRegex)) {
            return Map.of(
                    "success", false,
                    "message", typeErrorMessage
            );
        }

        LocalDate today = LocalDate.now();

        Path dir = Path.of(
                BASE_DIR,
                subFolder,
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue())
        );

        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + suffix;
        Path targetPath = dir.resolve(filename);
        file.transferTo(targetPath.toFile());

        String url = "/uploads/institution/"
                + subFolder
                + "/"
                + today.getYear()
                + "/"
                + String.format("%02d", today.getMonthValue())
                + "/"
                + filename;

        return Map.of(
                "success", true,
                "url", url,
                "originalFilename", originalFilename == null ? filename : originalFilename
        );
    }
}
