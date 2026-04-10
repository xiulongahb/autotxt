package com.autotxt.web;

import com.autotxt.service.HeadlinesService;
import com.autotxt.service.HeadlinesService.Headline;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Controller
public class HeadlinesController {

    private final HeadlinesService headlinesService;

    public HeadlinesController(HeadlinesService headlinesService) {
        this.headlinesService = headlinesService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "q", required = false) String q,
            Model model
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String query = (q == null) ? "" : q.trim();

        List<Headline> headlines;
        try {
            headlines = headlinesService.getHeadlines(targetDate, query);
        } catch (RuntimeException e) {
            headlines = Collections.emptyList();
            model.addAttribute("error", "新闻源暂时不可用，请稍后重试。");
        }
        model.addAttribute("date", targetDate);
        model.addAttribute("q", query);
        model.addAttribute("headlines", headlines);
        if (headlines.isEmpty()) {
            model.addAttribute("emptyHint", "该日期暂无匹配的头条（RSS 仅保留近期内容，可换一个更近的日期试试）。");
        }
        return "index";
    }
}

