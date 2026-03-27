package com.autotxt.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class HeadlinesService {

    private static final String FEED_URL = "https://www.chinanews.com.cn/rss/scroll-news.xml";
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter RSS_PUBDATE = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final RestTemplate restTemplate;

    public HeadlinesService() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(f);
    }

    public static final class Headline {
        private final String title;
        private final String link;
        private final String description;
        private final String time;
        private final ZonedDateTime publishedAt;

        public Headline(String title, String link, String description, String time, ZonedDateTime publishedAt) {
            this.title = title;
            this.link = link;
            this.description = description;
            this.time = time;
            this.publishedAt = publishedAt;
        }

        public String getTitle() {
            return title;
        }

        public String getLink() {
            return link;
        }

        public String getDescription() {
            return description;
        }

        public String getTime() {
            return time;
        }

        public ZonedDateTime getPublishedAt() {
            return publishedAt;
        }
    }

    public List<Headline> getHeadlines(LocalDate date, String keyword) {
        if (date == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        String q = (keyword == null) ? "" : keyword.trim();

        String xml = fetchFeedXml();
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        Elements items = doc.select("rss > channel > item");
        List<Headline> out = new ArrayList<>();

        for (Element item : items) {
            String title = item.selectFirst("title") != null ? item.selectFirst("title").text().trim() : "";
            String link = item.selectFirst("link") != null ? item.selectFirst("link").text().trim() : "";
            String desc = item.selectFirst("description") != null ? item.selectFirst("description").text().trim() : "";
            String pub = item.selectFirst("pubDate") != null ? item.selectFirst("pubDate").text().trim() : "";
            if (title.isEmpty() || link.isEmpty() || pub.isEmpty()) {
                continue;
            }

            ZonedDateTime publishedAtShanghai;
            try {
                publishedAtShanghai = ZonedDateTime.parse(pub, RSS_PUBDATE).withZoneSameInstant(SHANGHAI);
            } catch (RuntimeException ignored) {
                continue;
            }

            if (!publishedAtShanghai.toLocalDate().equals(date)) {
                continue;
            }
            if (!q.isEmpty()) {
                String haystack = (title + "\n" + desc).toLowerCase(Locale.ROOT);
                if (!haystack.contains(q.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }

            String time = publishedAtShanghai.format(TIME_FMT);
            out.add(new Headline(title, link, desc, time, publishedAtShanghai));
        }

        out.sort(Comparator.comparing(Headline::getPublishedAt).reversed());
        if (out.size() > 50) {
            return new ArrayList<>(out.subList(0, 50));
        }
        return out;
    }

    private String fetchFeedXml() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; AutoTxt/2.0; +" + FEED_URL + ")");
        headers.add(HttpHeaders.ACCEPT, "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8");
        headers.add(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> resp = restTemplate.exchange(FEED_URL, HttpMethod.GET, entity, byte[].class);
        byte[] body = resp.getBody();
        if (body == null || body.length == 0) {
            throw new IllegalStateException("新闻源无响应，请稍后重试");
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}

