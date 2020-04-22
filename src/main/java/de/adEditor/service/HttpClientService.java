package de.adEditor.service;

import de.autoDrive.NetworkServer.rest.NetworkServiceRestType;
import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesRequestDto;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesResponseDtos;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesStoreResponseDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointsResponseDto;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class HttpClientService {

    private static Logger LOG = LoggerFactory.getLogger(HttpClientService.class);
    private WebClient client;

    private final static int PORT = 8080;
    private final static String HOST = "localhost";
    private final static boolean USE_SSL = false;

//    private final static int PORT = 443;
//    private final static String HOST = "autodrive.si12.de";
//    private final static boolean USE_SSL = true;


    @Autowired
    private KeycloakService keycloakService;


    @PostConstruct
    public void setup() {
        Vertx vertx = Vertx.vertx();
        client = WebClient.create(vertx);
    }

    public void getRoutes(Handler<Optional<RoutesResponseDtos>> handler) {

        String path = RoutesRestPath.CONTEXT_PATH + RoutesRestPath.ROUTES;
        HttpRequest<RoutesResponseDtos> getRoutes = client.get(PORT, HOST, path)
                .ssl(USE_SSL)
//                .bearerTokenAuthentication(keycloakService.getToken())
                .putHeader("Accept", NetworkServiceRestType.MEDIATYPE_NETWORKSERVICE_JSON_V1)
                .as(BodyCodec.json(RoutesResponseDtos.class))
                .expect(ResponsePredicate.SC_OK);

        getRoutes.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<RoutesResponseDtos> response = ar.result();
                RoutesResponseDtos dto = response.body();

                LOG.info("Received response with status code {}", response.statusCode());
                handler.handle(Optional.of(dto));
            } else {
                LOG.error("Something went wrong {}", ar.cause().getMessage());
                handler.handle(Optional.empty());
            }
        });

    }

    public void upload(RoutesRequestDto requestDtodto, Handler<Optional<RoutesStoreResponseDto>> handler) {
        long start = System.currentTimeMillis();
        String path = RoutesRestPath.CONTEXT_PATH + RoutesRestPath.ROUTES;
        HttpRequest<RoutesStoreResponseDto> upload = client.post(PORT, HOST, path)
                .ssl(USE_SSL)
//                .bearerTokenAuthentication(keycloakService.getToken())
                .putHeader("ContentType", "application/json")
                .as(BodyCodec.json(RoutesStoreResponseDto.class))
                .expect(ResponsePredicate.SC_CREATED);

        upload.sendJson(requestDtodto, ar -> {
            if (ar.succeeded()) {
                HttpResponse<RoutesStoreResponseDto> response = ar.result();
                RoutesStoreResponseDto responseDto = response.body();

                long end = System.currentTimeMillis();
                LOG.info("Received response with status code {}, {}ms", response.statusCode(), end - start);
                handler.handle(Optional.of(responseDto));
            } else {
                LOG.error("Something went wrong {}", ar.cause().getMessage());
                handler.handle(Optional.empty());
            }
        });
    }


    public void getWayPoints(String id, Handler<Optional<WaypointsResponseDto>> handler) {

        String path = RoutesRestPath.CONTEXT_PATH + RoutesRestPath.ROUTES + "/" + id + RoutesRestPath.WAYPOINTS;
        HttpRequest<WaypointsResponseDto> getWaypoints = client.get(PORT, HOST, path)
                .ssl(USE_SSL)
//                .bearerTokenAuthentication(keycloakService.getToken())
                .putHeader("Accept", NetworkServiceRestType.MEDIATYPE_NETWORKSERVICE_JSON_V1)
                .as(BodyCodec.json(WaypointsResponseDto.class))
                .expect(ResponsePredicate.SC_OK);

        getWaypoints.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<WaypointsResponseDto> response = ar.result();
                WaypointsResponseDto dto = response.body();

                LOG.info("Received response with status code {}", response.statusCode());
                handler.handle(Optional.of(dto));
            } else {
                LOG.error("Something went wrong {}", ar.cause().getMessage());
                handler.handle(Optional.empty());
            }
        });

    }
}
