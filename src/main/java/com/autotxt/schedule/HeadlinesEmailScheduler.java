package com.autotxt.schedule;

import com.autotxt.service.HeadlinesEmailService;
import com.autotxt.subscriber.SubscriberStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class HeadlinesEmailScheduler {

    private final HeadlinesEmailService emailService;
    private final SubscriberStore subscriberStore;

    public HeadlinesEmailScheduler(HeadlinesEmailService emailService, SubscriberStore subscriberStore) {
        this.emailService = emailService;
        this.subscriberStore = subscriberStore;
    }

    @Scheduled(cron = "${autotxt.mail.cron:0 0 9 * * *}", zone = "${autotxt.mail.zone:Asia/Shanghai}")
    public void sendDaily() {
        if (!emailService.isEnabled()) {
            return;
        }

        LocalDate yesterday = ZonedDateTime.now(emailService.zoneId()).toLocalDate().minusDays(1);
        List<String> subs = subscriberStore.list();
        Set<String> uniq = new LinkedHashSet<>(subs);
        if (!uniq.isEmpty()) {
            emailService.sendHeadlinesTo(yesterday, new java.util.ArrayList<>(uniq));
            return;
        }

        // fallback to config recipients
        if (emailService.hasConfiguredRecipients()) {
            emailService.sendHeadlines(yesterday);
        }
    }
}

