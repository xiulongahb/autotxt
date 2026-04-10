package com.autotxt.schedule;

import com.autotxt.service.HeadlinesEmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Component
public class HeadlinesEmailScheduler {

    private final HeadlinesEmailService emailService;

    public HeadlinesEmailScheduler(HeadlinesEmailService emailService) {
        this.emailService = emailService;
    }

    @Scheduled(cron = "${autotxt.mail.cron:0 0 8 * * *}", zone = "${autotxt.mail.zone:Asia/Shanghai}")
    public void sendDaily() {
        if (!emailService.isEnabled()) {
            return;
        }
        LocalDate date = ZonedDateTime.now(emailService.zoneId()).toLocalDate();
        emailService.sendHeadlines(date);
    }
}

