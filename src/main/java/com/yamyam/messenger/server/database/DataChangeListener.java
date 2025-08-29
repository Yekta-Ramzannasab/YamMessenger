package com.yamyam.messenger.server.database;

public interface DataChangeListener {
    void onDataChanged(String eventType, Object data);
}
