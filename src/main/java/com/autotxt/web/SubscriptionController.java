package com.autotxt.web;

import com.autotxt.subscriber.SubscriberStore;
import com.autotxt.service.HeadlinesEmailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.mail.internet.InternetAddress;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class SubscriptionController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final SubscriberStore subscriberStore;
    private final HeadlinesEmailService emailService;

    public SubscriptionController(SubscriberStore subscriberStore, HeadlinesEmailService emailService) {
        this.subscriberStore = subscriberStore;
        this.emailService = emailService;
    }

    @GetMapping("/subscribe")
    public String page(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model
    ) {
        populateSubscribers(model, page, size);
        return "subscribe_page";
    }

    @PostMapping("/subscribe")
    public String submit(
            @RequestParam("email") String email,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model
    ) {
        try {
            String normalized = normalizeEmail(email);
            boolean added = subscriberStore.add(normalized);
            populateSubscribers(model, 1, size);
            model.addAttribute("email", normalized);
            model.addAttribute("added", added);
            return "subscribe_page";
        } catch (RuntimeException e) {
            populateSubscribers(model, page, size);
            model.addAttribute("error", e.getMessage() == null ? "订阅失败" : e.getMessage());
            model.addAttribute("email", email == null ? "" : email.trim());
            return "subscribe_page";
        }
    }

    @PostMapping("/subscribe/send-yesterday")
    public String sendYesterday(
            @RequestParam("email") String email,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model
    ) {
        populateSubscribers(model, page, size);

        try {
            String normalized = normalizeEmail(email);
            model.addAttribute("email", normalized);

            if (!emailService.isEnabled()) {
                model.addAttribute("sendError", "邮件功能未启用：请在 application.properties 设置 autotxt.mail.enabled=true，并配置 spring.mail.* SMTP 参数。");
                return "subscribe_page";
            }

            LocalDate yesterday = ZonedDateTime.now(emailService.zoneId()).toLocalDate().minusDays(1);
            emailService.sendHeadlinesTo(yesterday, Collections.singletonList(normalized));
            model.addAttribute("sendOk", true);
            model.addAttribute("sendDate", yesterday.toString());
            return "subscribe_page";
        } catch (RuntimeException e) {
            model.addAttribute("sendError", e.getMessage() == null ? "发送失败" : e.getMessage());
            model.addAttribute("email", email == null ? "" : email.trim());
            return "subscribe_page";
        }
    }

    private void populateSubscribers(Model model, int page, int size) {
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        int currentPage = page <= 0 ? 1 : page;

        List<String> all = new ArrayList<>(subscriberStore.list());
        Collections.reverse(all); // show latest first

        int total = all.size();
        int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int from = Math.min((currentPage - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<String> pageItems = all.subList(from, to);

        model.addAttribute("count", total);
        model.addAttribute("subscribers", pageItems);
        model.addAttribute("page", currentPage);
        model.addAttribute("size", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrev", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);
        model.addAttribute("prevPage", Math.max(1, currentPage - 1));
        model.addAttribute("nextPage", Math.min(totalPages, currentPage + 1));
    }

    private static String normalizeEmail(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        try {
            InternetAddress addr = new InternetAddress(t);
            addr.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        return t.toLowerCase();
    }
}

