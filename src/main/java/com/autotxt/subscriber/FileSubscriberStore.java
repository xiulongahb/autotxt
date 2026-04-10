package com.autotxt.subscriber;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FileSubscriberStore implements SubscriberStore {

    private final Path filePath;
    private final Object lock = new Object();

    public FileSubscriberStore(@Value("${autotxt.subscribers.file:data/subscribers.txt}") String file) {
        this.filePath = Paths.get(file == null ? "data/subscribers.txt" : file.trim());
    }

    @Override
    public List<String> list() {
        synchronized (lock) {
            return new ArrayList<>(readAll());
        }
    }

    @Override
    public boolean add(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email 不能为空");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("email 不能为空");
        }

        synchronized (lock) {
            Set<String> emails = readAll();
            boolean added = emails.add(normalized);
            if (added) {
                writeAll(emails);
            }
            return added;
        }
    }

    private Set<String> readAll() {
        Set<String> out = new LinkedHashSet<>();
        if (!Files.exists(filePath)) {
            return out;
        }
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null) continue;
                String t = line.trim().toLowerCase();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        } catch (IOException e) {
            throw new IllegalStateException("读取订阅邮箱失败：" + e.getMessage(), e);
        }
    }

    private void writeAll(Set<String> emails) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = Paths.get(filePath.toString() + ".tmp");
            List<String> lines = new ArrayList<>(emails);
            Files.write(tmp, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("保存订阅邮箱失败：" + e.getMessage(), e);
        }
    }
}

