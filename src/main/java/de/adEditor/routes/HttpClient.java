package de.adEditor.routes;

import com.google.gson.Gson;
import de.adEditor.config.AdConfiguration;
import de.adEditor.routes.dto.*;
import de.adEditor.routes.events.ErrorMsg;
import de.adEditor.routes.events.GetRoutesEvent;
import de.adEditor.routes.events.HttpClientEventListener;
import de.adEditor.routes.events.UploadCompletedEvent;
import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.EventListenerList;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class HttpClient {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(RoutesRestPath.DATE_FORMAT);
    private Gson gson = new Gson();
    private EventListenerList listenerList = new EventListenerList();
    private IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setSoTimeout(Timeout.ofSeconds(300)).build();
    private static HttpClient instance = null;

//    private HttpHost target = new HttpHost("localhost", 8080);
    private HttpHost target = new HttpHost("https","autodrive.si12.de");

    private static Logger LOG = LoggerFactory.getLogger(HttpClient.class);


    public static HttpClient getInstance() {
        if (instance == null) {
            instance = new HttpClient();
        }
        return instance;
    }

    public void addMyEventListener(HttpClientEventListener listener) {
        listenerList.add(HttpClientEventListener.class, listener);
    }

    public void removeMyEventListener(HttpClientEventListener listener) {
        listenerList.remove(HttpClientEventListener.class, listener);
    }

    public void upload(RouteExport routeExport, String name, String map, Integer revision, Date date, String username, String description) throws ExecutionException, InterruptedException, IOException {

        long start = System.currentTimeMillis();
        RoutesRequestDto dto = toDto(routeExport, name, map, revision, date, username, description);

        SimpleHttpRequest httppost = SimpleHttpRequests.post(target, RoutesRestPath.CONTEXT_PATH + RoutesRestPath.ROUTES);
        httppost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ KeycloakService.getInstance().getToken());
        String body = gson.toJson(dto);
        httppost.setBody(body, ContentType.APPLICATION_JSON);
        try (CloseableHttpAsyncClient client = HttpAsyncClients.custom().setIOReactorConfig(ioReactorConfig).build()) {
            client.start();
            Future<SimpleHttpResponse> future = client.execute(httppost, new FutureCallback<>() {

                @Override
                public void completed(SimpleHttpResponse response) {
                    long end = System.currentTimeMillis();
                    RoutesStoreResponseDto routesStoreResponseDto = gson.fromJson(response.getBodyText(), RoutesStoreResponseDto.class);
                    LOG.info("completed: {} at {}ms", response.getCode(), end - start);
                    fireUploadCompletedEvent(new UploadCompletedEvent(routesStoreResponseDto));
                }

                @Override
                public void failed(Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    fireUploadCompletedEvent(new UploadCompletedEvent(new ErrorMsg(ex.getMessage())));
                }

                @Override
                public void cancelled() {
                    long end = System.currentTimeMillis();
                    LOG.error("cancelled: {}ms", end - start);
                    fireUploadCompletedEvent(new UploadCompletedEvent(new ErrorMsg("Operation canceled")));
                }

            });
            SimpleHttpResponse simpleHttpResponse = future.get();

            long end = System.currentTimeMillis();
            LOG.info("Upload response: {} at {}ms", simpleHttpResponse.getCode(), end - start);
        }
    }

    private RoutesRequestDto toDto(RouteExport routeExport, String name, String map, Integer revision, Date date, String username, String description) {
        RoutesRequestDto dto = new RoutesRequestDto();
        dto.setDate(ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(formatter));
        dto.setName(name);
        dto.setMap(map);
        dto.setRevision(revision);
        dto.setUsername(username);
        dto.setDescription(StringUtils.substring(description, 0, 2999));
        dto.setGroups(routeExport.getGroups().stream().map(group -> toGroupDto(group)).collect(Collectors.toList()));
        dto.setMarkers(routeExport.getMarkers().stream().map(m -> toMarkerDto(m)).collect(Collectors.toList()));
        dto.setWaypoints(toWaypointDtos(routeExport.getWaypoints()));
        return dto;
    }

    private List<WaypointDto> toWaypointDtos(Waypoints waypoints) {

        // TODO: remove stream
        List<WaypointDto> dtos = Arrays.stream(waypoints.getX().split(";")).map(x -> {
            WaypointDto wp = new WaypointDto();
            wp.setX(Double.valueOf(x));
            return wp;
        }).collect(Collectors.toList());

        String[] y = waypoints.getY().split(";");
        String[] z = waypoints.getZ().split(";");
        String[] out = waypoints.getOut().split(";");

        for (int i = 0; i < dtos.size(); i++) {
            WaypointDto dto = dtos.get(i);
            dto.setY(Double.valueOf(y[i]));
            dto.setZ(Double.valueOf(z[i]));
            String[] oValue = out[i].split(",");
            for (String s : oValue) {
                if (StringUtils.isNotBlank(s)) {
                    dto.getOut().add(Integer.valueOf(s));
                }
            }
        }

        return dtos;
    }

    private MarkerDto toMarkerDto(Marker marker) {
        var dto = new MarkerDto();
        dto.setGroup(marker.getGroup());
        dto.setName(marker.getName());
        dto.setWaypointIndex(marker.getWaypointIndex());
        return dto;
    }

    private GroupDto toGroupDto(Group group) {
        var dto = new GroupDto();
        dto.setName(group.getName());
        return dto;
    }


    public void getRoutes() throws ExecutionException, InterruptedException, IOException {

        SimpleHttpRequest httpget = SimpleHttpRequests.get(target, RoutesRestPath.CONTEXT_PATH + RoutesRestPath.ROUTES);
        httpget.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ KeycloakService.getInstance().getToken());
        try (CloseableHttpAsyncClient client = HttpAsyncClients.custom().setIOReactorConfig(ioReactorConfig).build()) {
            client.start();
            Future<SimpleHttpResponse> future = client.execute(httpget, new FutureCallback<>() {

                @Override
                public void completed(SimpleHttpResponse response) {
                    String bodyText = response.getBodyText();
                    RoutesResponseDtos routesResponseDtos = gson.fromJson(bodyText, RoutesResponseDtos.class);
                    fireGetRouteEvent(new GetRoutesEvent(routesResponseDtos.getRoutes()));
                }

                @Override
                public void failed(Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    fireGetRouteEvent(new GetRoutesEvent(new ErrorMsg(ex.getMessage())));
                }

                @Override
                public void cancelled() {
                    LOG.error("getRoutes cancelled.");
                    fireGetRouteEvent(new GetRoutesEvent(new ErrorMsg("Operation canceled")));
                }

            });
            SimpleHttpResponse simpleHttpResponse = future.get();
            LOG.info("Upload response: {}", simpleHttpResponse.getCode());
        }
    }

    public void getWaypoints(String routeId) throws ExecutionException, InterruptedException, IOException {

        SimpleHttpRequest httpget = SimpleHttpRequests.get(target, RoutesRestPath.CONTEXT_PATH + RoutesRestPath.ROUTES+"/"+routeId + RoutesRestPath.WAYPOINTS);
        httpget.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ KeycloakService.getInstance().getToken());
        try (CloseableHttpAsyncClient client = HttpAsyncClients.custom().setIOReactorConfig(ioReactorConfig).build()) {
            client.start();
            Future<SimpleHttpResponse> future = client.execute(httpget, new FutureCallback<>() {

                @Override
                public void completed(SimpleHttpResponse response) {
                    String bodyText = response.getBodyText();
                    WaypointsResponseDto waypointsResponseDto = gson.fromJson(bodyText, WaypointsResponseDto.class);
                    fireGetWayointsEvent(new GetRoutesEvent(waypointsResponseDto));
                }

                @Override
                public void failed(Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    fireGetRouteEvent(new GetRoutesEvent(new ErrorMsg(ex.getMessage())));
                }

                @Override
                public void cancelled() {
                    LOG.error("getWaypoints cancelled.");
                    fireGetRouteEvent(new GetRoutesEvent(new ErrorMsg("Operation canceled")));
                }

            });
            SimpleHttpResponse simpleHttpResponse = future.get();
            LOG.info("Upload response: {}", simpleHttpResponse.getCode());
        }
    }

    void fireGetRouteEvent(GetRoutesEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i = i + 2) {
            if (listeners[i] == HttpClientEventListener.class) {
                ((HttpClientEventListener) listeners[i + 1]).getRoutes(evt);
            }
        }
    }

    void fireGetWayointsEvent(GetRoutesEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i = i + 2) {
            if (listeners[i] == HttpClientEventListener.class) {
                ((HttpClientEventListener) listeners[i + 1]).getWaypoints(evt);
            }
        }
    }

    void fireUploadCompletedEvent(UploadCompletedEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i = i + 2) {
            if (listeners[i] == HttpClientEventListener.class) {
                ((HttpClientEventListener) listeners[i + 1]).getUploadRouteResponse(evt);
            }
        }
    }

    private List<Route> toRoute(RoutesResponseDtos routesResponseDtos) {
        return routesResponseDtos.getRoutes().stream().map(this::toRoute).collect(Collectors.toList());
    }

    private Route toRoute(RouteDto dto) {
        Route route = new Route();
        SimpleDateFormat sdf = new SimpleDateFormat(RoutesRestPath.DATE_FORMAT);
        try {
            route.setDate(sdf.parse(dto.getDate()));
        } catch (ParseException e) {
            LOG.error(e.getMessage(),e);
        }
        route.setMap(dto.getMap());
        route.setRevision(dto.getRevision());
        route.setName(dto.getName());
        return route;
    }
}
