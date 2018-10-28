package site.alicemononobe.liveman.event;

import site.alicemononobe.liveman.mediaproxy.proxytask.MediaProxyTask;

import java.util.EventObject;

public class MediaProxyEvent extends EventObject {
    private MediaProxyTask mediaProxyTask;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public MediaProxyEvent(Object source) {
        super(source);
    }

    public MediaProxyTask getMediaProxyTask() {
        return mediaProxyTask;
    }

    public void setMediaProxyTask(MediaProxyTask mediaProxyTask) {
        this.mediaProxyTask = mediaProxyTask;
    }
}
