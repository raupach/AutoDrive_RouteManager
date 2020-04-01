package de.adEditor.routes.events;

import java.util.EventListener;

public interface HttpClientEventListener extends EventListener {
    void getRoutes(GetRoutesEvent evt);
    void getWaypoints(GetRoutesEvent evt);
    void getUploadRouteResponse(UploadCompletedEvent evt);
}
