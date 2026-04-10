package com.autotxt.service;

import com.autotxt.service.HeadlinesService.Headline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class HeadlinesEmailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JavaMailSender mailSender;
    private final HeadlinesService headlinesService;

    private final boolean enabled;
    private final String from;
    private final String to;
    private final String subjectPrefix;
    private final String keyword;
    private final String zone;

    public HeadlinesEmailService(
            JavaMailSender mailSender,
            HeadlinesService headlinesService,
            @Value("${autotxt.mail.enabled:false}") boolean enabled,
            @Value("${autotxt.mail.from:}") String from,
            @Value("${autotxt.mail.to:}") String to,
            @Value("${autotxt.mail.subjectPrefix:头条新闻}") String subjectPrefix,
            @Value("${autotxt.mail.keyword:}") String keyword,
            @Value("${autotxt.mail.zone:Asia/Shanghai}") String zone
    ) {
        this.mailSender = mailSender;
        this.headlinesService = headlinesService;
        this.enabled = enabled;
        this.from = safeTrim(from);
        this.to = safeTrim(to);
        this.subjectPrefix = safeTrim(subjectPrefix);
        this.keyword = safeTrim(keyword);
        this.zone = safeTrim(zone);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(zone.isEmpty() ? "Asia/Shanghai" : zone);
        } catch (RuntimeException ignored) {
            return ZoneId.of("Asia/Shanghai");
        }
    }

    public void sendHeadlines(LocalDate date) {
        if (!enabled) {
            return;
        }
        if (date == null) {
            throw new IllegalArgumentException("date 不能为空");
        }
        if (to.isEmpty()) {
            throw new IllegalStateException("未配置收件人：autotxt.mail.to");
        }

        List<Headline> headlines = headlinesService.getHeadlines(date, keyword);
        String subject = (subjectPrefix.isEmpty() ? "头条新闻" : subjectPrefix) + " " + date.format(DATE_FMT);

        String html = buildHtml(date, headlines, keyword);
        sendHtml(subject, html);
    }

    private void sendHtml(String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            if (!from.isEmpty()) {
                helper.setFrom(from);
            }
            helper.setTo(parseRecipients(to));
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("邮件发送失败：" + e.getMessage(), e);
        }
    }

    private static String buildHtml(LocalDate date, List<Headline> headlines, String keyword) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\"/></head><body style=\"font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial;\">");
        sb.append("<h2 style=\"margin:0 0 12px;\">头条新闻 · ").append(escape(date.toString())).append("</h2>");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sb.append("<div style=\"color:#6b7280;margin:0 0 12px;\">关键词：").append(escape(keyword.trim())).append("</div>");
        }
        if (headlines == null || headlines.isEmpty()) {
            sb.append("<p style=\"color:#6b7280;\">当天暂无匹配新闻（RSS 仅保留近期内容）。</p>");
        } else {
            sb.append("<ol style=\"padding-left:18px;\">");
            for (Headline h : headlines) {
                sb.append("<li style=\"margin:10px 0;\">")
                        .append("<div style=\"font-size:14px;color:#111827;\"><strong>")
                        .append(escape(nullToEmpty(h.getTime())))
                        .append("</strong> ")
                        .append("<a href=\"").append(escapeAttr(h.getLink())).append("\" style=\"color:#2563eb;text-decoration:none;\">")
                        .append(escape(h.getTitle()))
                        .append("</a></div>");
                if (h.getDescription() != null && !h.getDescription().trim().isEmpty()) {
                    sb.append("<div style=\"margin-top:4px;color:#4b5563;font-size:13px;line-height:1.5;\">")
                            .append(escape(h.getDescription().trim()))
                            .append("</div>");
                }
                sb.append("</li>");
            }
            sb.append("</ol>");
        }
        sb.append("<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:18px 0;\"/>");
        sb.append("<div style=\"color:#9ca3af;font-size:12px;\">由 AutoTxt 自动发送</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String[] parseRecipients(String raw) {
        String s = safeTrim(raw);
        if (s.isEmpty()) {
            return new String[0];
        }
        String[] parts = s.split("[,;\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = safeTrim(p);
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.toArray(new String[0]);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeAttr(String s) {
        return escape(s);
    }
}

