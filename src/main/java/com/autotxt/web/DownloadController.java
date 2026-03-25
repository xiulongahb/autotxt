package com.autotxt.web;

import com.autotxt.service.NlcReadCloudService;
import com.autotxt.service.NlcReadCloudService.DownloadResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

@Controller
public class DownloadController {

    private final NlcReadCloudService downloadService;

    public DownloadController(NlcReadCloudService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/download")
    public Object download(@RequestParam("title") String title, Model model) {
        try {
            DownloadResult result = downloadService.searchAndBuildTxt(title);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(result.filename(), StandardCharsets.UTF_8)
                            .build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(result.content());
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title != null ? title : "");
            return "index";
        }
    }
}
