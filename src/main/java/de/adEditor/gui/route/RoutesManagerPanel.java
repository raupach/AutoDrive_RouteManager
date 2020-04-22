package de.adEditor.gui.route;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.adEditor.ApplicationContextProvider;
import de.adEditor.config.AdConfiguration;
import de.adEditor.gui.editor.EditorFrame;
import de.adEditor.gui.editor.RoadMap;
import de.adEditor.helper.IconHelper;
import de.adEditor.dto.AutoDriveRoutesManager;
import de.adEditor.dto.GameSettings;
import de.adEditor.dto.Route;
import de.adEditor.dto.RouteExport;
import de.adEditor.service.RouteManagerService;
import de.adEditor.service.HttpClientService;
import de.autoDrive.NetworkServer.rest.dto_v1.RouteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class RoutesManagerPanel extends JPanel {

    private static Logger LOG = LoggerFactory.getLogger(RoutesManagerPanel.class);

    private final static String directory = "/autoDrive/routesManager";
    private final static String routesManagerPath = directory + "/routes.xml";
    private final static String routesDirectory = directory + "/routes";
    private final static String gameSettings = "gameSettings.xml";
    private final static String reloadCfn = "/reload.cfn";


    private JTable lokalTable;
    private JTable remoteTable;
    private EditorFrame editorFrame;

    private HttpClientService httpClientService;
    private RouteManagerService routeManagerService;

    public RoutesManagerPanel(EditorFrame editorFrame) {
        super(new BorderLayout());
        this.editorFrame = editorFrame;

        httpClientService = ApplicationContextProvider.getContext().getBean(HttpClientService.class);
        routeManagerService = ApplicationContextProvider.getContext().getBean(RouteManagerService.class);

        createTable();
    }

    public void reloadServerRoutes() {

        httpClientService.getRoutes(responseDto->{
            responseDto.ifPresentOrElse (dto->{
                setCursor(Cursor.getDefaultCursor());
                remoteTable.setModel(new AutoDriveRemoteRoutesTableModel(dto.getRoutes()));
            }, this::showErrorMessage);
        });
    }


    private void showErrorMessage() {
        Thread t = new Thread(() -> JOptionPane.showMessageDialog(null, "The routes could not be loaded.", "Error", JOptionPane.ERROR_MESSAGE));
        t.start();
    }

    private void downloadFullRoute() {
        getRouteId().ifPresent(id ->downloadWaypointsForRoute(id));
    }

    private Optional<String> getRouteId() {
        int rowIndex = remoteTable.getSelectedRow();
        if (rowIndex >= 0) {
            AutoDriveRemoteRoutesTableModel model = (AutoDriveRemoteRoutesTableModel) remoteTable.getModel();
            RouteDto routeDto = model.get(rowIndex);
            return Optional.ofNullable(routeDto.getId());
        }
        return Optional.empty();
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
        menuItemUpload.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_up.png")));
        menuItemUpload.addActionListener(e -> startUploadRoute());
        localPopupMenu.add(menuItemUpload);

        JMenuItem menuItemDownloadIntoEditor = new JMenuItem("Load into Editor");
        menuItemDownloadIntoEditor.setIcon(new ImageIcon(IconHelper.getImageUrl("note_edit.png")));
        menuItemDownloadIntoEditor.addActionListener(e -> {});
        localPopupMenu.add(menuItemDownloadIntoEditor);

        JMenuItem menuItemMergeItoEditor = new JMenuItem("Merge into Editor");
        menuItemMergeItoEditor.setIcon(new ImageIcon(IconHelper.getImageUrl("note_add.png")));
        menuItemMergeItoEditor.addActionListener(e -> {});
        localPopupMenu.add(menuItemMergeItoEditor);

        localPopupMenu.addSeparator();
        JMenuItem menuItemRefresh = new JMenuItem("Refresh");
        menuItemRefresh.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_refresh.png")));
        menuItemRefresh.addActionListener(e -> reloadXMLRouteMetaData());
        localPopupMenu.add(menuItemRefresh);

        localPopupMenu.addSeparator();
        JMenuItem menuItemDelete = new JMenuItem("Delete");
        menuItemDelete.setIcon(new ImageIcon(IconHelper.getImageUrl("cross.png")));
        menuItemDelete.addActionListener(e -> {});
        localPopupMenu.add(menuItemDelete);
        return localPopupMenu;
    }

    private JPopupMenu createRemoteRoutesPopupMenu() {
        JPopupMenu localPopupMenu = new JPopupMenu();

        JMenuItem menuItemDownload = new JMenuItem("Download");
        menuItemDownload.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_down.png")));
        menuItemDownload.addActionListener(e -> downloadFullRoute());
        localPopupMenu.add(menuItemDownload);

        JMenuItem menuItemDownloadIntoEditor = new JMenuItem("Download into Editor");
        menuItemDownloadIntoEditor.setIcon(new ImageIcon(IconHelper.getImageUrl("note_edit.png")));
        menuItemDownloadIntoEditor.addActionListener(e -> downloadIntoEditor());
        localPopupMenu.add(menuItemDownloadIntoEditor);

        JMenuItem menuItemMergeItoEditor = new JMenuItem("Merge into Editor");
        menuItemMergeItoEditor.setIcon(new ImageIcon(IconHelper.getImageUrl("note_add.png")));
        menuItemMergeItoEditor.addActionListener(e -> {});
        localPopupMenu.add(menuItemMergeItoEditor);

        localPopupMenu.addSeparator();
        JMenuItem menuItemRefresh = new JMenuItem("Refresh");
        menuItemRefresh.setIcon(new ImageIcon(IconHelper.getImageUrl("arrow_refresh.png")));
        menuItemRefresh.addActionListener(e -> reloadServerRoutes());
        localPopupMenu.add(menuItemRefresh);

        localPopupMenu.addSeparator();
        JMenuItem menuItemDelete = new JMenuItem("Delete");
        menuItemDelete.setIcon(new ImageIcon(IconHelper.getImageUrl("cross.png")));
        menuItemDelete.addActionListener(e -> {});
        localPopupMenu.add(menuItemDelete);

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
            routeManagerService.login();
            loginButton.setEnabled(false);
            editorFrame.setExtendedState(JFrame.ICONIFIED);
            editorFrame.setExtendedState(JFrame.NORMAL);
            editorFrame.setVisible(true);
            editorFrame.toFront();
            editorFrame.requestFocus();
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
        AutoDriveRoutesManager autoDriveRoutesManager = routeManagerService.readXmlRoutesMetaData();
        GameSettings gameSettings = readGameSettings();
        lokalTable.setModel(new AutoDriveLocalRoutesTableModel(autoDriveRoutesManager.getRoutes(), gameSettings));
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


    private void startUploadRoute() {
        int row = lokalTable.getSelectedRow();
        if (row >= 0) {
            AutoDriveLocalRoutesTableModel model = (AutoDriveLocalRoutesTableModel) lokalTable.getModel();
            String username = model.getUsername();
            Route route = model.get(row);
            RouteExport routeExport = routeManagerService.readXmlRouteData(route.getFileName());

            UploadDialog uploadDialog = new UploadDialog(editorFrame, route, routeExport, username);
            uploadDialog.setVisible(true);
            reloadServerRoutes();
        }
    }


    private void downloadWaypointsForRoute(String routeId) {

        getRouteId().ifPresent(id -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            httpClientService.getWayPoints(id, dtoOptional -> {
                setCursor(Cursor.getDefaultCursor());
                dtoOptional.ifPresentOrElse(waypointsResponseDto -> {
                    routeManagerService.processDownloadedWaypoints(waypointsResponseDto);
                    reloadXMLRouteMetaData();
                }, this::showErrorMessage);
            });
        });
    }

    private void downloadIntoEditor() {

        getRouteId().ifPresent(id -> {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            httpClientService.getWayPoints(id, dtoOptional->{
                setCursor(Cursor.getDefaultCursor());
                dtoOptional.ifPresent(waypointsResponseDto->{
                    SwingUtilities.invokeLater(() -> {
                        RoadMap roadMap = routeManagerService.toEditor(waypointsResponseDto);
                        editorFrame.prepareMapPanel(roadMap, waypointsResponseDto.getRoute().getMap());
                    });
                });
            });
        });
    }
}
