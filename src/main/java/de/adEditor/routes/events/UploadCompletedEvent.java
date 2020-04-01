package de.adEditor.routes.events;

import java.util.EventObject;

public class UploadCompletedEvent extends EventObject {
    public UploadCompletedEvent(Object source) {
        super(source);
    }
}
