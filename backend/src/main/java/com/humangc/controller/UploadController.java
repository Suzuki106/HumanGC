package com.humangc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.humangc.dto.UploadResponse;
import com.humangc.entity.Paper;
import com.humangc.entity.User;
import com.humangc.mapper.PaperMapper;
import com.humangc.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class UploadController {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/upload")
    public UploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "school", required = false) String school) {

        log.info("Upload request: filename={}, size={}, anonymousId={}",
                file.getOriginalFilename(), file.getSize(), anonymousId);

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Extract text based on file type
        String text;
        String originalFilename = file.getOriginalFilename();
        String fileType;

        try {
            if (originalFilename == null) {
                throw new RuntimeException("Filename is null");
            }

            String lowerFilename = originalFilename.toLowerCase();
            if (lowerFilename.endsWith(".pdf")) {
                text = extractPdfText(file);
                fileType = "pdf";
            } else if (lowerFilename.endsWith(".docx")) {
                text = extractDocxText(file);
                fileType = "docx";
            } else if (lowerFilename.endsWith(".txt")) {
                text = extractTxtText(file);
                fileType = "txt";
            } else {
                throw new RuntimeException("Unsupported file type: " + originalFilename +
                        ". Supported types: PDF, DOCX, TXT");
            }
        } catch (Exception e) {
            log.error("Error extracting text from file", e);
            throw new RuntimeException("Failed to extract text: " + e.getMessage());
        }

        if (text == null || text.isBlank()) {
            throw new RuntimeException("No text content found in file");
        }

        // Find or create user
        User user = findOrCreateUser(anonymousId, region, school);

        // Save paper
        Paper paper = new Paper();
        paper.setUserId(user.getId());
        paper.setOriginalText(text);
        paper.setFileType(fileType);
        paper.setOriginalFilename(originalFilename);
        paper.setCreatedAt(LocalDateTime.now());
        paperMapper.insert(paper);

        log.info("Paper saved: id={}, userId={}, filename={}, textLength={}",
                paper.getId(), user.getId(), originalFilename, text.length());

        return new UploadResponse(paper.getId(), originalFilename);
    }

    private String extractPdfText(MultipartFile file) throws Exception {
        byte[] bytes = file.getInputStream().readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);
            stripper.setSuppressDuplicateOverlappingText(true);
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("PDF文件中未检测到可提取的文本（可能是扫描版PDF或图片型PDF）");
            }
            log.info("PDF text extracted: {} chars", text.length());
            return text;
        }
    }

    private String extractDocxText(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            // Extract paragraphs
            document.getParagraphs().forEach(p -> {
                String t = p.getText();
                if (t != null && !t.isBlank()) {
                    sb.append(t).append("\n");
                }
            });
            // Also extract tables
            document.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        String t = cell.getText();
                        if (t != null && !t.isBlank()) {
                            sb.append(t).append(" ");
                        }
                    });
                    sb.append("\n");
                });
            });
            String text = sb.toString();
            if (text.isBlank()) {
                throw new RuntimeException("DOCX文件中未检测到文本内容");
            }
            log.info("DOCX text extracted: {} chars", text.length());
            return text;
        }
    }

    private String extractTxtText(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String text = reader.lines().collect(Collectors.joining("\n"));
            if (text.isBlank()) {
                // Try UTF-8 without BOM, then GBK
                throw new RuntimeException("TXT文件为空或编码不支持");
            }
            return text;
        }
    }

    private User findOrCreateUser(String anonymousId, String region, String school) {
        if (anonymousId == null || anonymousId.isBlank()) {
            anonymousId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAnonymousId, anonymousId);
        User user = userMapper.selectOne(wrapper);

        String resolvedRegion = (region != null && !region.isBlank()) ? region : "未知地区";
        String resolvedSchool = (school != null && !school.isBlank()) ? school : "未知学校";

        if (user == null) {
            user = new User();
            user.setAnonymousId(anonymousId);
            user.setNickname("匿名用户");
            user.setRegion(resolvedRegion);
            user.setSchool(resolvedSchool);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
            log.info("Created new user: id={}, anonymousId={}, region={}, school={}",
                    user.getId(), anonymousId, resolvedRegion, resolvedSchool);
        } else {
            boolean updated = false;
            if (user.getRegion() == null || user.getRegion().isBlank()) {
                user.setRegion(resolvedRegion);
                updated = true;
            }
            if (user.getSchool() == null || user.getSchool().isBlank()) {
                user.setSchool(resolvedSchool);
                updated = true;
            }
            if (updated) {
                userMapper.updateById(user);
                log.info("Updated user region/school: id={}, region={}, school={}",
                        user.getId(), resolvedRegion, resolvedSchool);
            }
        }

        return user;
    }
}
