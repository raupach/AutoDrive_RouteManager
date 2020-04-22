package de.adEditor.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.adEditor.config.AdConfiguration;
import de.adEditor.gui.editor.RoadMap;
import de.adEditor.mapper.RouteEditorMapper;
import de.adEditor.mapper.RouteExportMapper;
import de.adEditor.dto.AutoDriveRoutesManager;
import de.adEditor.dto.Route;
import de.adEditor.dto.RouteExport;
import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointsResponseDto;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class RouteManagerService {

    private static Logger LOG = LoggerFactory.getLogger(RouteManagerService.class);
    private final static String directory = "/autoDrive/routesManager";
    private final static String routesManagerPath = directory + "/routes.xml";
    private final static String routesDirectory = directory + "/routes";
    private final static String gameSettings = "gameSettings.xml";
    private final static String reloadCfn = "/reload.cfn";

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private RouteExportMapper routeExportMapper;

    @Autowired
    private RouteEditorMapper routeEditorMapper;

    @Autowired
    private KeycloakService keycloakService;


    public void processDownloadedWaypoints(WaypointsResponseDto waypointsResponseDto) {
        String filename = writeXmlRouteData(routeExportMapper.toRouteExport(waypointsResponseDto));

        AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
        Route newRoute = new Route();
        newRoute.setName(waypointsResponseDto.getRoute().getName());
        newRoute.setRevision(waypointsResponseDto.getRoute().getRevision());
        newRoute.setMap(waypointsResponseDto.getRoute().getMap());
        SimpleDateFormat sdf = new SimpleDateFormat(RoutesRestPath.DATE_FORMAT);
        try {
            newRoute.setDate(sdf.parse(waypointsResponseDto.getRoute().getDate()));
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }
        newRoute.setFileName(filename);
        newRoute.setServerId(waypointsResponseDto.getRoute().getId());
        Objects.requireNonNull(autoDriveRoutesManager).getRoutes().add(newRoute);
        writeXmlRoutesMetaData(autoDriveRoutesManager, filename);

        touchReloadCfn();
    }


    private String writeXmlRouteData(RouteExport routeExport) {

        String gameDir = AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY);
        File adDirectory = new File(gameDir + routesDirectory);
        if (!adDirectory.exists()) {
            adDirectory.mkdirs();
        }

        try {
            String fileName = UUID.randomUUID().toString() + ".xml";
            ObjectMapper mapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            mapper.writeValue(new File(gameDir + routesDirectory + "/" + fileName), routeExport);
            return fileName;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public RouteExport readXmlRouteData(String fileName) {

        try {
            ObjectMapper mapper = new XmlMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String gameDir = AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY);
            return mapper.readValue(new File(gameDir + routesDirectory + "/" + fileName), RouteExport.class);

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public AutoDriveRoutesManager readXmlRoutesMetaData() {

        String gameDir = AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY);
        File adDirectory = new File(gameDir + directory);
        if (!adDirectory.exists()) {
            adDirectory.mkdirs();
        }

        File adRoute = new File(gameDir + routesManagerPath);
        if (adRoute.exists()) {
            try {
                ObjectMapper mapper = new XmlMapper();
                mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                return mapper.readValue(adRoute, AutoDriveRoutesManager.class);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return new AutoDriveRoutesManager();
    }

    private void writeXmlRoutesMetaData(AutoDriveRoutesManager autoDriveRoutesManager, String filename) {

        try {
            ObjectMapper mapper = new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String gameDir = AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY);
            mapper.writeValue(new File(gameDir + routesManagerPath), autoDriveRoutesManager);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void touchReloadCfn() {
        String gameDir = AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY);
        File reloadCfnFile = new File(gameDir + directory + reloadCfn);
        try {
            FileUtils.touch(reloadCfnFile);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void login() {
        keycloakService.login();
    }

    public RoadMap toEditor(WaypointsResponseDto waypointsResponseDto) {
        return routeEditorMapper.toRoadMap(waypointsResponseDto);
    }
}
