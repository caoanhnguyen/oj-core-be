package com.kma.ojcore.controller;

import com.kma.ojcore.service.TestcaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("${app.api.prefix}/admin/testcases")
@RequiredArgsConstructor
public class TestcaseController {

    private final TestcaseService testcaseService;

    @PostMapping("/upload/{problemId}")
    public void uploadTestcases(@PathVariable UUID problemId, @RequestBody MultipartFile file) throws IOException {
        testcaseService.processAndUploadTestcases(problemId, file);
    }
}
