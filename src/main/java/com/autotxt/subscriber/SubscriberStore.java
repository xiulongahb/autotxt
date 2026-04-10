package com.autotxt.subscriber;

import java.util.List;

public interface SubscriberStore {
    List<String> list();

    /**
     * @return true if newly added, false if already exists
     */
    boolean add(String email);
}

