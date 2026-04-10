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
import java.util.Collections;
import java.util.List;

@Controller
public class SubscriptionController {

    private final SubscriberStore subscriberStore;
    private final HeadlinesEmailService emailService;

    public SubscriptionController(SubscriberStore subscriberStore, HeadlinesEmailService emailService) {
        this.subscriberStore = subscriberStore;
        this.emailService = emailService;
    }

    @GetMapping("/subscribe")
    public String page(Model model) {
        List<String> subs = subscriberStore.list();
        model.addAttribute("count", subs.size());
        return "subscribe";
    }

    @PostMapping("/subscribe")
    public String submit(@RequestParam("email") String email, Model model) {
        try {
            String normalized = normalizeEmail(email);
            boolean added = subscriberStore.add(normalized);
            List<String> subs = subscriberStore.list();

            model.addAttribute("count", subs.size());
            model.addAttribute("email", normalized);
            model.addAttribute("added", added);
            return "subscribe";
        } catch (RuntimeException e) {
            List<String> subs = subscriberStore.list();
            model.addAttribute("count", subs.size());
            model.addAttribute("error", e.getMessage() == null ? "订阅失败" : e.getMessage());
            model.addAttribute("email", email == null ? "" : email.trim());
            return "subscribe";
        }
    }

    @PostMapping("/subscribe/send-yesterday")
    public String sendYesterday(@RequestParam("email") String email, Model model) {
        List<String> subs = subscriberStore.list();
        model.addAttribute("count", subs.size());

        try {
            String normalized = normalizeEmail(email);
            model.addAttribute("email", normalized);

            if (!emailService.isEnabled()) {
                model.addAttribute("sendError", "邮件功能未启用：请在 application.properties 设置 autotxt.mail.enabled=true，并配置 spring.mail.* SMTP 参数。");
                return "subscribe";
            }

            LocalDate yesterday = ZonedDateTime.now(emailService.zoneId()).toLocalDate().minusDays(1);
            emailService.sendHeadlinesTo(yesterday, Collections.singletonList(normalized));
            model.addAttribute("sendOk", true);
            model.addAttribute("sendDate", yesterday.toString());
            return "subscribe";
        } catch (RuntimeException e) {
            model.addAttribute("sendError", e.getMessage() == null ? "发送失败" : e.getMessage());
            model.addAttribute("email", email == null ? "" : email.trim());
            return "subscribe";
        }
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

