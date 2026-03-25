package com.autotxt.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 检索中国国家图书馆「读者云门户」电子图书分类（read.nlc.cn），解析公开检索结果页。
 * 国图未提供全文 TXT 开放接口，正文多在平台内在线阅读；此处生成含书目信息与官方详情链接的 TXT 文件。
 */
@Service
public class NlcReadCloudService {

    private static final String READ_CLOUD_ORIGIN = "http://read.nlc.cn";
    private static final String SEARCH_PATH = "/allSearch/searchList";

    private final RestTemplate restTemplate = new RestTemplate();

    public static final class DownloadResult {
        private final String filename;
        private final byte[] content;

        public DownloadResult(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }

        public String filename() {
            return filename;
        }

        public byte[] content() {
            return content;
        }
    }

    public DownloadResult searchAndBuildTxt(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入书名或关键词");
        }

        URI searchUri = UriComponentsBuilder.fromUriString(READ_CLOUD_ORIGIN + SEARCH_PATH)
                .queryParam("searchType", "1")
                .queryParam("showType", "1")
                .queryParam("pageNo", "1")
                .queryParam("searchWord", query.trim())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (compatible; AutoTxt/1.0; +http://read.nlc.cn/)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                searchUri, HttpMethod.GET, entity, String.class);
        String html = response.getBody();
        if (html == null || html.trim().isEmpty()) {
            throw new IllegalStateException("国家图书馆读者云检索页无响应，请稍后重试");
        }

        Document doc = Jsoup.parse(html, READ_CLOUD_ORIGIN);
        Element hit = doc.selectFirst("ul.YMH2019_New_BSLW_List1 li > a[href*=searchDetail]");
        if (hit == null) {
            hit = doc.selectFirst("a[href*=/allSearch/searchDetail]");
        }
        if (hit == null) {
            throw new IllegalStateException("未在「电子图书」中找到匹配结果，请换关键词或到 read.nlc.cn 直接检索");
        }

        String detailHref = hit.attr("href");
        if (detailHref == null || detailHref.isEmpty()) {
            throw new IllegalStateException("结果条目缺少详情链接");
        }
        String detailUrl = detailHref.startsWith("http") ? detailHref : READ_CLOUD_ORIGIN + detailHref;

        String bookTitle = hit.select("span.tt").text().trim();
        if (bookTitle.isEmpty()) {
            bookTitle = query.trim();
        }

        Elements metaPs = hit.select("div.txt p");
        String lineAuthor = metaPs.size() > 0 ? metaPs.get(0).text().trim() : "";
        String linePublisher = metaPs.size() > 1 ? metaPs.get(1).text().trim() : "";

        String when = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("中国国家图书馆 · 读者云门户 · 检索摘录\r\n");
        sb.append("========================================\r\n\r\n");
        sb.append("检索关键词：").append(query.trim()).append("\r\n");
        sb.append("检索入口：").append(searchUri.toString()).append("\r\n");
        sb.append("生成时间：").append(when).append("（北京时间）\r\n\r\n");
        sb.append("【首条命中书目】\r\n");
        sb.append("题名：").append(bookTitle).append("\r\n");
        if (!lineAuthor.isEmpty()) {
            sb.append(lineAuthor).append("\r\n");
        }
        if (!linePublisher.isEmpty()) {
            sb.append(linePublisher).append("\r\n");
        }
        sb.append("详情与在线阅读（官方页面）：\r\n").append(detailUrl).append("\r\n\r\n");
        sb.append("————————————————————————————————————————\r\n");
        sb.append("说明：国家图书馆数字图书通常在平台内在线阅读，本站不抓取正文，仅根据公开检索页整理书目与链接。\r\n");
        sb.append("请遵守国家图书馆服务条款与著作权相关规定。\r\n");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String safeName = sanitizeFilename(bookTitle);
        return new DownloadResult(safeName + "-国图书目.txt", bytes);
    }

    private static String sanitizeFilename(String title) {
        String base = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (base.isEmpty()) {
            base = "nlc-read";
        }
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        return base;
    }
}
