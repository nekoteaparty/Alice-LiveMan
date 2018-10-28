package site.alicemononobe.liveman.event;

import java.util.EventListener;

public interface MediaProxyEventListener extends EventListener {

    default void onProxyStart(MediaProxyEvent e) {
    }

    default void onProxyStop(MediaProxyEvent e) {
    }
}
