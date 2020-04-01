package de.adEditor.routes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.adEditor.AutoDriveEditor;
import de.adEditor.config.AdConfiguration;
import de.adEditor.helper.IconHelper;
import de.adEditor.routes.dto.*;
import de.adEditor.routes.events.ErrorMsg;
import de.adEditor.routes.events.GetRoutesEvent;
import de.adEditor.routes.events.HttpClientEventAdapter;
import de.autoDrive.NetworkServer.rest.RoutesRestPath;
import de.autoDrive.NetworkServer.rest.dto_v1.RouteDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointsResponseDto;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RoutesManagerPanel extends JPanel {

    private static Logger LOG = LoggerFactory.getLogger(RoutesManagerPanel.class);

    private final static String directory = "/autoDrive/routesManager";
    private final static String routesManagerPath = directory + "/routes.xml";
    private final static String routesDirectory = directory + "/routes";
    private final static String gameSettings = "gameSettings.xml";
    private final static String reloadCfn = "/reload.cfn";

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    private JTable lokalTable;
    private JTable remoteTable;

    private JFrame parentFrame;

    public RoutesManagerPanel(AutoDriveEditor parentFrame) {
        super(new BorderLayout());
        this.parentFrame = parentFrame;

        HttpClient.getInstance().addMyEventListener(new HttpClientEventAdapter() {
            @Override
            public void getRoutes(GetRoutesEvent evt) {
                setCursor(Cursor.getDefaultCursor());
                if (evt.getSource() instanceof ErrorMsg) {
                    ErrorMsg errorMsg = (ErrorMsg) evt.getSource();
                    JOptionPane.showMessageDialog(null, errorMsg.getMsg(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    remoteTable.setModel(new AutoDriveRemoteRoutesTableModel((List<RouteDto>) evt.getSource()));
                }
            }

            @Override
            public void getWaypoints(GetRoutesEvent evt) {
                setCursor(Cursor.getDefaultCursor());
                if (evt.getSource() instanceof ErrorMsg) {
                    ErrorMsg errorMsg = (ErrorMsg) evt.getSource();
                    JOptionPane.showMessageDialog(null, errorMsg.getMsg(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    WaypointsResponseDto waypointsResponseDto = (WaypointsResponseDto) evt.getSource();
                    String filename = writeXmlRouteData(toRouteExport(waypointsResponseDto));

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
                    reloadXMLRouteMetaData();
                    touchReloadCfn();
                }
            }

        });

        createTable();
    }

    private RouteExport toRouteExport(WaypointsResponseDto waypointsResponseDto) {
        int waypointCount = waypointsResponseDto.getWaypoints().size();
        RouteExport routeExport = new RouteExport();
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<Double> z = new ArrayList<>();
        List<String> out = new ArrayList<>();
        List<WaypointDto> waypointDtos = waypointsResponseDto.getWaypoints();
        Map<Integer, List<Integer>> inMap = new HashMap<>();

        // Fill in-Map with default-value -1. In case of that we have no referencing out-node.
        for (int i = 1; i <= waypointCount; i++) {
            List<Integer> initList = new ArrayList<>();
            initList.add(-1);
            inMap.put(i, initList);
        }

        waypointDtos.forEach(waypointDto -> {

            x.add(waypointDto.getX());
            y.add(waypointDto.getY());
            z.add(waypointDto.getZ());

            out.add(waypointDto.getOut().isEmpty() ? "-1" : StringUtils.join(waypointDto.getOut(), ","));

            waypointDto.getOut().forEach(i -> {
                List<Integer> in = inMap.get(i);

                // remove the default value if its alone.
                if (in.size() == 1 && in.get(0).equals(-1)) {
                    in.clear();
                }
                in.add((waypointDtos.indexOf(waypointDto)) + 1);
            });
        });

        Waypoints waypoints = new Waypoints();
        waypoints.setC(waypointCount);
        waypoints.setX(StringUtils.join(x, ";"));
        waypoints.setY(StringUtils.join(y, ";"));
        waypoints.setZ(StringUtils.join(z, ";"));
        waypoints.setOut(StringUtils.join(out, ";"));
        waypoints.setIn(StringUtils.join(inMap.values().stream().map(inIndex -> StringUtils.join(inIndex, ",")).collect(Collectors.toList()), ";"));

        routeExport.setWaypoints(waypoints);
        routeExport.setGroups(waypointsResponseDto.getGroups().stream().map(g -> new Group(g.getName())).collect(Collectors.toList()));
        routeExport.setMarkers(waypointsResponseDto.getMarkers().stream().map(m -> new Marker(m.getName(), m.getGroup(), m.getWaypointIndex() + 1)).collect(Collectors.toList()));
        return routeExport;
    }

    public void reloadServerRoutes() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Runnable runnableTask = () -> {
            try {
                HttpClient.getInstance().getRoutes();
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        };
        executorService.execute(runnableTask);
    }

    private void downloadFullRoute() {
        int rowIndex = remoteTable.getSelectedRow();
        if (rowIndex >= 0) {
            AutoDriveRemoteRoutesTableModel model = (AutoDriveRemoteRoutesTableModel) remoteTable.getModel();
            RouteDto routeDto = model.get(rowIndex);
            downloadWaypointsForRoute(routeDto.getId());
        }
    }


    private void downloadWaypointsForRoute(String routeId) {

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Runnable runnableTask = () -> {
            try {
                HttpClient.getInstance().getWaypoints(routeId);
            } catch (ExecutionException | InterruptedException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        };
        executorService.execute(runnableTask);
    }

    private void createTable() {

        JPanel panelLeft = createLeftPanel();
        JPanel panelRight = createRightPanel();

        lokalTable = new JTable(new AutoDriveLocalRoutesTableModel());
//        lokalTable.getColumnModel().getColumn(0).setPreferredWidth(500);
//        lokalTable.getColumnModel().getColumn(1).setPreferredWidth(50);
//        lokalTable.getColumnModel().getColumn(2).setPreferredWidth(20);
//        lokalTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        lokalTable.setComponentPopupMenu(createLocalRoutesPopupMenu());

        remoteTable = new JTable(new AutoDriveRemoteRoutesTableModel());
        remoteTable.setComponentPopupMenu(createRemoteRoutesPopupMenu());

        panelLeft.add(new JScrollPane(lokalTable));
        panelRight.add(new JScrollPane(remoteTable));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
        splitPane.setDividerLocation(0.5);
        add(splitPane);
    }

    private JPopupMenu createLocalRoutesPopupMenu() {
        JPopupMenu localPopupMenu = new JPopupMenu();

        JMenuItem menuItemUpload = new JMenuItem("Upload");
        menuItemUpload.addActionListener(e -> startUploadRoute());
        localPopupMenu.add(menuItemUpload);

        JMenuItem menuItemRefresh = new JMenuItem("Refresh");
        menuItemRefresh.addActionListener(e -> reloadXMLRouteMetaData());
        localPopupMenu.add(menuItemRefresh);

        return localPopupMenu;
    }

    private JPopupMenu createRemoteRoutesPopupMenu() {
        JPopupMenu localPopupMenu = new JPopupMenu();

        JMenuItem menuItemDownload = new JMenuItem("Download");
        menuItemDownload.addActionListener(e -> downloadFullRoute());
        localPopupMenu.add(menuItemDownload);

        JMenuItem menuItemRefresh = new JMenuItem("Refresh");
        menuItemRefresh.addActionListener(e -> reloadServerRoutes());
        localPopupMenu.add(menuItemRefresh);

        return localPopupMenu;
    }

    private JPanel createRightPanel() {
        JPanel panelRight = new JPanel();
        panelRight.setLayout(new BorderLayout());

        JPanel topBoxPanel = new JPanel();
        topBoxPanel.setLayout(new BoxLayout(topBoxPanel, BoxLayout.LINE_AXIS));
        topBoxPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 10));

        JLabel remoteText = new JLabel("Remote Routes:", JLabel.LEFT);
        topBoxPanel.add(remoteText);
        topBoxPanel.add(Box.createRigidArea(new Dimension(30, 0)));

        JButton refreshRemoteTable = new JButton();
        refreshRemoteTable.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_refresh.png"), "refresh"));
        refreshRemoteTable.setToolTipText("refresh routes");
        topBoxPanel.add(refreshRemoteTable);
        topBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        refreshRemoteTable.addActionListener(actionEvent -> reloadServerRoutes());

        JButton downloadRemoteRoute = new JButton();
        downloadRemoteRoute.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_down.png"), "download"));
        downloadRemoteRoute.setToolTipText("download selected route");
        downloadRemoteRoute.addActionListener(e -> downloadFullRoute());
        topBoxPanel.add(downloadRemoteRoute);

        topBoxPanel.add(Box.createRigidArea(new Dimension(200, 0)));
        JButton loginButton = new JButton();
        loginButton.setIcon(new ImageIcon(IconHelper.getImageUrl("user_go.png"), "login"));
        loginButton.setToolTipText("login to server");
        loginButton.setText("Login");
        loginButton.addActionListener(e -> {
            KeycloakService.getInstance().login();
            loginButton.setEnabled(false);
            parentFrame.setExtendedState(JFrame.ICONIFIED);
            parentFrame.setExtendedState(JFrame.NORMAL);
            parentFrame.setVisible(true);
            parentFrame.toFront();
            parentFrame.requestFocus();
            reloadServerRoutes();
        });
        topBoxPanel.add(loginButton);

        panelRight.add(topBoxPanel, BorderLayout.NORTH);

        return panelRight;
    }


    private JPanel createLeftPanel() {
        JPanel panelRight = new JPanel();
        panelRight.setLayout(new BorderLayout());

        JPanel topBoxPanel = new JPanel();
        topBoxPanel.setLayout(new BoxLayout(topBoxPanel, BoxLayout.LINE_AXIS));
        topBoxPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 10));

        JLabel localText = new JLabel("Lokal Routes:", JLabel.LEFT);
        topBoxPanel.add(localText);
        topBoxPanel.add(Box.createRigidArea(new Dimension(30, 0)));

        JButton refreshLocalTable = new JButton();
        refreshLocalTable.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_refresh.png"), "refresh"));
        refreshLocalTable.setToolTipText("refresh local routes");
        topBoxPanel.add(refreshLocalTable);
        topBoxPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        refreshLocalTable.addActionListener(actionEvent -> reloadXMLRouteMetaData());

        JButton uploadLocalRoute = new JButton();
        uploadLocalRoute.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_up.png"), "upload"));
        uploadLocalRoute.setToolTipText("upload selected route to server");
        uploadLocalRoute.addActionListener(e -> startUploadRoute());
        topBoxPanel.add(uploadLocalRoute);

        panelRight.add(topBoxPanel, BorderLayout.NORTH);

        return panelRight;
    }


    public void reloadXMLRouteMetaData() {
        AutoDriveRoutesManager autoDriveRoutesManager = readXmlRoutesMetaData();
        GameSettings gameSettings = readGameSettings();
        lokalTable.setModel(new AutoDriveLocalRoutesTableModel(autoDriveRoutesManager.getRoutes(), gameSettings));
    }

    private AutoDriveRoutesManager readXmlRoutesMetaData() {

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

    private GameSettings readGameSettings() {
        String gameDir = AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY);
        File gameSettingsFile = new File(gameDir + "/" + gameSettings);
        if (gameSettingsFile.exists()) {

            try {
                ObjectMapper mapper = new XmlMapper();
                mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                return mapper.readValue(gameSettingsFile, GameSettings.class);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return new GameSettings();
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

    private RouteExport readXmlRouteData(String fileName) {

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

    private void startUploadRoute() {
        int row = lokalTable.getSelectedRow();
        if (row >= 0) {
            AutoDriveLocalRoutesTableModel model = (AutoDriveLocalRoutesTableModel) lokalTable.getModel();
            String username = model.getUsername();
            Route route = model.get(row);
            RouteExport routeExport = readXmlRouteData(route.getFileName());

            UploadDialog uploadDialog = new UploadDialog(parentFrame, route, routeExport, username);
            uploadDialog.setVisible(true);
            reloadServerRoutes();
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

}
