package de.adEditor.gui.editor;

import de.adEditor.AppConfig;
import de.adEditor.ApplicationContextProvider;
import de.adEditor.config.AdConfiguration;
import de.adEditor.gui.ConfigDialog;
import de.adEditor.gui.graph.RoadMapMarker;
import de.adEditor.gui.route.RoutesManagerPanel;
import de.adEditor.helper.IconHelper;
import de.adEditor.mapper.AutoDriveConfigToRoadMap;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;

public class EditorFrame extends JFrame {

    public static final String AUTO_DRIVE_COURSE_EDITOR_TITLE = "AutoDrive Course Editor 2.0";

    private MapPanel mapPanel;
    private RoutesManagerPanel routesManagerPanel;
    private JTabbedPane tabPane;
    private Component destinationTreePanel;

    private File xmlConfigFile;
    private boolean stale = false;
    private EditorMode editorMode = EditorMode.MOVE;
    private DefaultMutableTreeNode markerRootNode = new DefaultMutableTreeNode();
    private JTree markerTree;

    private static Logger LOG = LoggerFactory.getLogger(EditorFrame.class);


    public EditorFrame() {
        super();
        setTitle(createTitle());
        setIconImage(loadIcon("/tractor.png"));
        setJMenuBar(createMenuBar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isStale()) {
                    int response = JOptionPane.showConfirmDialog(null, "There are unsaved changes. Should they be saved now?", "AutoDrive", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        saveMap();
                    }
                }
                CacheManager cacheManager = ApplicationContextProvider.getContext().getBean(CacheManager.class);
                if (cacheManager.getStatus().equals(Status.AVAILABLE)) {
                    cacheManager.close();
                }
                super.windowClosing(e);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                checkAndLoadProperties();
            }

        });


        tabPane = new JTabbedPane (JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT );
        add(tabPane);
        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        routesManagerPanel = new RoutesManagerPanel(this);
        tabPane.addTab("Course Editor", new ImageIcon(IconHelper.getImageUrl("note_edit.png")) , editorPanel);
        tabPane.addTab("Network Manager", new ImageIcon(IconHelper.getImageUrl("note_go.png")), routesManagerPanel);


        c.gridx = 0;
        c.gridy = 0;
        c.fill= GridBagConstraints.VERTICAL;
        editorPanel.add(createToolBar(), c);


        // create a new panel with GridBagLayout manager
        mapPanel = new MapPanel(this);

        // set border for the panel
        mapPanel.setPreferredSize(new Dimension(1024, 768));
        mapPanel.setMinimumSize(new Dimension(1024, 768));

        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.fill= GridBagConstraints.BOTH;
        editorPanel.add(mapPanel, c);

        destinationTreePanel = createDestinationTreePanel();
        c.gridx = 2;
        c.gridy = 0;
        c.weighty = 0;
        c.weightx = 0;
        c.fill= GridBagConstraints.VERTICAL;
        editorPanel.add(destinationTreePanel, c );

        pack();
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private Component createDestinationTreePanel() {
        JPanel markerTreePanel = new JPanel(new BorderLayout());
        markerTreePanel.setPreferredSize(new Dimension(250,100));
        markerTreePanel.setMinimumSize(new Dimension(250,100));
        markerTreePanel.add (new Label("Markers"), BorderLayout.NORTH);

        markerTree = new JTree(markerRootNode);
        markerTree.setEditable(true);
//        markerTree.setCellEditor(getEditor());
        markerTree.setRootVisible(false);
        markerTree.setShowsRootHandles(true);

        markerTree.addTreeSelectionListener(event -> {
            DefaultMutableTreeNode selectedComponent = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
            Object userObject = selectedComponent.getUserObject();
            if (userObject instanceof RoadMapMarker) {
                RoadMapMarker roadMapMarker = (RoadMapMarker) userObject;
                mapPanel.showNode(roadMapMarker.getgNode());
            }
        });

        markerTreePanel.add(new JScrollPane(markerTree), BorderLayout.CENTER);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(new ImageIcon(IconHelper.getImageUrl("marker.png")));
        markerTree.setCellRenderer(renderer);

        return markerTreePanel;
    }

    private TreeCellEditor getEditor() {
        return new DefaultTreeCellEditor(markerTree, (DefaultTreeCellRenderer) markerTree.getCellRenderer()){
            @Override
            public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
                System.out.println("editing ");
                return super.getTreeCellEditorComponent(tree, value, isSelected, expanded,leaf, row);
            }

        };
    }

    private Component createToolBar() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JButton openBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/open.png")));
        JButton saveBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/save.png")));
        JToggleButton moveBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/move.png")));
        moveBtn.setSelected(true);
        moveBtn.setToolTipText("Select and move objects");

        JToggleButton autoNodeBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/autonode.png")));
        autoNodeBtn.setToolTipText("Draw lines and points");

        JToggleButton deleteBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/delete.png")));
        JButton joinBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/mergenodes.png")));
        joinBtn.setToolTipText("Merge points");

        JButton splitBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/unglueways.png")));
        splitBtn.setToolTipText("Separate points");

        JButton flipBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/wayflip.png")));
        flipBtn.setToolTipText("Flip direction");

        JButton twoWayBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/conflict.png")));
        twoWayBtn.setToolTipText("Flip dual/single way");

        JToggleButton propBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/tag.png")));
        propBtn.setSelected(true);
        propBtn.setToolTipText("Hide and show markers");
        propBtn.addActionListener(actionEvent -> destinationTreePanel.setVisible(propBtn.isSelected()));

        openBtn.addActionListener(actionEvent -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select AutoDrive Config");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("AutoDrive config", "xml");
            fc.addChoosableFileFilter(filter);
            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fileName = fc.getSelectedFile();
                AutoDriveConfigToRoadMap autoDriveConfigToRoadMap = ApplicationContextProvider.getContext().getBean(AutoDriveConfigToRoadMap.class);
                try {
                    RoadMap roadMap = autoDriveConfigToRoadMap.loadXmlConfigFile(fileName);
                    mapPanel.setRoadMap(roadMap);
                    updateMarker(roadMap);
                    mapPanel.repaint();
                } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
                    LOG.error(e.getMessage(), e);
                }

            }
        });


        moveBtn.addActionListener(actionEvent -> {
            if (moveBtn.isSelected()) {
                editorMode = EditorMode.MOVE;
                autoNodeBtn.setSelected(false);
                deleteBtn.setSelected(false);
            }
        });

        autoNodeBtn.addActionListener(actionEvent -> {
            if (autoNodeBtn.isSelected()) {
                editorMode = EditorMode.DRAW;
                deleteBtn.setSelected(false);
                moveBtn.setSelected(false);
            }
        });

        deleteBtn.addActionListener(actionEvent -> {
            if (deleteBtn.isSelected()) {
                editorMode = EditorMode.DELETE;
                autoNodeBtn.setSelected(false);
                moveBtn.setSelected(false);
            }
        });

        joinBtn.addActionListener(actionEvent-> mapPanel.joinNodes());
        splitBtn.addActionListener(actionEvent ->   mapPanel.splitNode());
        flipBtn.addActionListener(actionEvent -> mapPanel.flipEdge());
        twoWayBtn.addActionListener(actionEvent -> mapPanel.flipTwoWay());

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.insets = new Insets( 4, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTH;
        toolbar.add(openBtn, c);
        c.gridy = 1;
        c.insets = new Insets( 4, 4, 10, 4);
        toolbar.add(saveBtn, c);
        c.insets = new Insets( 4, 4, 0, 4);
        c.gridy = 2;
        toolbar.add(moveBtn, c);
        c.gridy = 3;
        toolbar.add(autoNodeBtn, c);
        c.gridy = 4;
        toolbar.add(deleteBtn, c);
        c.gridy = 5;
        toolbar.add(joinBtn, c);
        c.gridy = 6;
        toolbar.add(splitBtn, c);
        c.gridy = 7;
        toolbar.add(flipBtn, c);
        c.gridy = 8;
        toolbar.add(twoWayBtn, c);

        c.insets = new Insets( 14, 4, 0, 4);
        c.gridy = 9;
        toolbar.add(propBtn, c);

        c.gridy = 10;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        toolbar.add(new Label(), c);

        return toolbar;
    }

    private void updateMarker(RoadMap roadMap) {
        markerRootNode.removeAllChildren();
        roadMap.getGroups().forEach(roadMapGroup ->{
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(roadMapGroup);
            roadMapGroup.getMarkers().forEach(marker ->{
                groupNode.add(new DefaultMutableTreeNode(marker));
            });
            markerRootNode.add(groupNode);
        });
        DefaultTreeModel model = (DefaultTreeModel)markerTree.getModel();
        model.reload();
        for (int r = 0; r < markerTree.getRowCount(); r++) {
            markerTree.expandRow(r);
        }
    }


    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Editor");
        menuBar.add(menu);

        JMenuItem menuConfigItem = new JMenuItem("Configuration");
        menuConfigItem.addActionListener(e ->{
            showConfigDialog();
            enableManagerPanel();
        });
        menu.add(menuConfigItem);

        JMenuItem menuClearCacheItem = new JMenuItem("Clear cache");
        menuClearCacheItem.addActionListener(e ->{
            CacheManager cacheManager = ApplicationContextProvider.getContext().getBean(CacheManager.class);
            Cache<String, Image> cache1 = cacheManager.getCache(AppConfig.IMAGES_CACHE_L1, String.class, Image.class);
            Cache<String, byte[]> cache2 = cacheManager.getCache(AppConfig.IMAGES_CACHE_L2, String.class, byte[].class);
            Cache<String, byte[]> cache3 = cacheManager.getCache(AppConfig.IMAGES_CACHE_L3, String.class, byte[].class);

            cache1.clear();
            cache2.clear();
            cache3.clear();

            mapPanel.repaint();
        });
        menu.add(menuClearCacheItem);

        JMenuItem menuQuitItem = new JMenuItem("Quit");
        menuQuitItem.addActionListener(e -> System.exit(0));
        menu.add(menuQuitItem);

        return menuBar;
    }

    private BufferedImage loadIcon(String name) {
        try {
            URL url = EditorFrame.class.getResource(name);
            if (url != null) {
                return ImageIO.read(url);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }



    public void prepareMapPanel(RoadMap roadMap, String mapName) {
        mapPanel.setRoadMap(roadMap);
        mapPanel.repaint();
        tabPane.setSelectedIndex(0);
    }


    private BufferedImage loadMapImageFromDisk(String mapName) {
        String mapPath = "/mapImages/" + mapName + ".png";
        URL url = EditorFrame.class.getResource(mapPath);

        BufferedImage image = null;
        try {
            image = ImageIO.read(url);
        } catch (Exception e) {
            try {
                mapPath = "./mapImages/" + mapName + ".png";
                image = ImageIO.read(new File(mapPath));
            } catch (Exception e1) {
                try {
                    mapPath = "./src/mapImages/" + mapName + ".png";
                    image = ImageIO.read(new File(mapPath));
                } catch (Exception e2) {
                    try {
                        mapPath = "./" + mapName + ".png";
                        image = ImageIO.read(new File(mapPath));
                    } catch (Exception e3) {
                        //mapBoxSetEnabled(true);
                        LOG.info("Editor has no map file for map: {}", mapName);
                    }
                }
            }
        }
        return image;
    }

    public void saveMap() {
        LOG.info("SaveMap called");
        RoadMap roadMap = mapPanel.getRoadMap();

        try
        {
            saveXmlConfig(xmlConfigFile, roadMap);
            setStale(false);
            JOptionPane.showMessageDialog(this, xmlConfigFile.getName() + " has been successfully saved.", "AutoDrive", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "The AutoDrive Config could not be saved.", "AutoDrive", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveXmlConfig(File file, RoadMap roadMap) throws ParserConfigurationException, IOException, SAXException, TransformerException {

//        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//        Document doc = docBuilder.parse(file);
//
//        Node AutoDrive = doc.getFirstChild();
//        Element root = doc.getDocumentElement();
//
//        Node waypoints = doc.getElementsByTagName("waypoints").item(0);
//
//        // loop the staff child node
//        NodeList list = waypoints.getChildNodes();
//
//        for (int i = 0; i < list.getLength(); i++) {
//            Node node = list.item(i);
//
//            if ("id".equals(node.getNodeName())) {
//                StringBuilder ids = new StringBuilder();
//                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
//                    MapNode mapNode = roadMap.mapNodes.get(j);
//                    ids.append(mapNode.id);
//                    if (j < (roadMap.mapNodes.size() - 1)) {
//                        ids.append(",");
//                    }
//                }
//                node.setTextContent(ids.toString());
//            }
//            if ("x".equals(node.getNodeName())) {
//                StringBuilder xPositions = new StringBuilder();
//                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
//                    MapNode mapNode = roadMap.mapNodes.get(j);
//                    xPositions.append(mapNode.x);
//                    if (j < (roadMap.mapNodes.size() - 1)) {
//                        xPositions.append(",");
//                    }
//                }
//                node.setTextContent(xPositions.toString());
//            }
//            if ("y".equals(node.getNodeName())) {
//                StringBuilder yPositions = new StringBuilder();
//                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
//                    MapNode mapNode = roadMap.mapNodes.get(j);
//                    yPositions.append(mapNode.y);
//                    if (j < (roadMap.mapNodes.size() - 1)) {
//                        yPositions.append(",");
//                    }
//                }
//                node.setTextContent(yPositions.toString());
//            }
//            if ("z".equals(node.getNodeName())) {
//                StringBuilder zPositions = new StringBuilder();
//                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
//                    MapNode mapNode = roadMap.mapNodes.get(j);
//                    zPositions.append(mapNode.z);
//                    if (j < (roadMap.mapNodes.size() - 1)) {
//                        zPositions.append(",");
//                    }
//                }
//                node.setTextContent(zPositions.toString());
//            }
//            if ("incoming".equals(node.getNodeName())) {
//                StringBuilder incomingString = new StringBuilder();
//                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
//                    MapNode mapNode = roadMap.mapNodes.get(j);
//                    StringBuilder incomingsPerNode = new StringBuilder();
//                    for (int incomingIndex = 0; incomingIndex < mapNode.incoming.size(); incomingIndex++) {
//                        MapNode incomingNode = mapNode.incoming.get(incomingIndex);
//                        incomingsPerNode.append(incomingNode.id);
//                        if (incomingIndex < (mapNode.incoming.size() - 1)) {
//                            incomingsPerNode.append(",");
//                        }
//                    }
//                    if (incomingsPerNode.toString().isEmpty()) {
//                        incomingsPerNode = new StringBuilder("-1");
//                    }
//                    incomingString.append(incomingsPerNode);
//                    if (j < (roadMap.mapNodes.size() - 1)) {
//                        incomingString.append(";");
//                    }
//                }
//                node.setTextContent(incomingString.toString());
//            }
//            if ("out".equals(node.getNodeName())) {
//                StringBuilder outgoingString = new StringBuilder();
//                for (int j = 0; j < roadMap.mapNodes.size(); j++) {
//                    MapNode mapNode = roadMap.mapNodes.get(j);
//                    StringBuilder outgoingPerNode = new StringBuilder();
//                    for (int outgoingIndex = 0; outgoingIndex < mapNode.outgoing.size(); outgoingIndex++) {
//                        MapNode outgoingNode = mapNode.outgoing.get(outgoingIndex);
//                        outgoingPerNode.append(outgoingNode.id);
//                        if (outgoingIndex < (mapNode.outgoing.size() - 1)) {
//                            outgoingPerNode.append(",");
//                        }
//                    }
//                    if (outgoingPerNode.toString().isEmpty()) {
//                        outgoingPerNode = new StringBuilder("-1");
//                    }
//                    outgoingString.append(outgoingPerNode);
//                    if (j < (roadMap.mapNodes.size() - 1)) {
//                        outgoingString.append(";");
//                    }
//                }
//                node.setTextContent(outgoingString.toString());
//            }
//        }
//
//        for (int markerIndex = 1; markerIndex < roadMap.mapMarkers.size() + 100; markerIndex++) {
//            Element element = (Element) doc.getElementsByTagName("mm" + (markerIndex)).item(0);
//            if (element != null) {
//                Element parent = (Element) element.getParentNode();
//                while (parent.hasChildNodes())
//                    parent.removeChild(parent.getFirstChild());
//            }
//        }
//
//        NodeList markerList = doc.getElementsByTagName("mapmarker");
//        Node markerNode = markerList.item(0);
//        int mapMarkerCount = 1;
//        for (MapMarker mapMarker : roadMap.mapMarkers) {
//            Element newMapMarker = doc.createElement("mm" + mapMarkerCount);
//
//            Element markerID = doc.createElement("id");
//            markerID.appendChild(doc.createTextNode("" + mapMarker.mapNode.id));
//            newMapMarker.appendChild(markerID);
//
//            Element markerName = doc.createElement("name");
//            markerName.appendChild(doc.createTextNode(mapMarker.name));
//            newMapMarker.appendChild(markerName);
//
//            Element markerGroup = doc.createElement("group");
//            markerGroup.appendChild(doc.createTextNode(mapMarker.group));
//            newMapMarker.appendChild(markerGroup);
//
//            markerNode.appendChild(newMapMarker);
//            mapMarkerCount += 1;
//        }
//
//
//        Node mapNameNode = waypoints.getParentNode();
//        String newMapName = mapNameNode.getNodeValue();
//        String fileName = xmlConfigFile.getName();
//        if (fileName.contains("AutoDrive_") && fileName.contains("_config")) {
//            int newPathStartIndex = fileName.lastIndexOf("AutoDrive_");
//            newPathStartIndex += "AutoDrive_".length();
//            int newPathEndIndex = fileName.lastIndexOf("_config");
//            if (fileName.endsWith("_init_config")) {
//                newPathEndIndex = fileName.lastIndexOf("_init_config");
//            }
//            newMapName = fileName.substring(newPathStartIndex, newPathEndIndex);
//            LOG.info("Found new map name in: {} : {}", fileName, newMapName);
//        }
//        doc.renameNode(mapNameNode, null, newMapName);
//
//        // write the content into xml file
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//
//        DOMSource source = new DOMSource(doc);
//        StreamResult result = new StreamResult(xmlConfigFile);
//        transformer.transform(source, result);

        LOG.info("Done save");
    }


    private String createTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(AUTO_DRIVE_COURSE_EDITOR_TITLE);
        if (xmlConfigFile != null) {
            sb.append(" - ").append(xmlConfigFile.getAbsolutePath()).append(isStale() ? " *" : "");
        }
        return sb.toString();
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public void setMapPanel(MapPanel mapPanel) {
        this.mapPanel = mapPanel;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        if (isStale() != stale) {
            this.stale = stale;
            setTitle(createTitle());
        }
    }


    private void checkAndLoadProperties() {
        File configFile = new File(AdConfiguration.CONFIG_FILE_NAME);
        if (configFile.exists()) {
            AdConfiguration.getInstance().readConfigFile();
        } else {
            showConfigDialog();
        }
        enableManagerPanel();
    }

    private void enableManagerPanel() {
        if ( StringUtils.isNotBlank(AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY))) {
            tabPane.setEnabledAt(1, true);
            try {
                routesManagerPanel.reloadXMLRouteMetaData();
            }catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        else {
            tabPane.setEnabledAt(1, false);
        }
    }

    private void showConfigDialog() {
        ConfigDialog configDialog = new ConfigDialog(this, true);
        configDialog.setVisible(true);
        ConfigDialog.DIALOG_STATE dialog_state = configDialog.getState();
        if (dialog_state.equals(ConfigDialog.DIALOG_STATE.OK)) {
            AdConfiguration.getInstance().writeConfigFile();
        }
    }

    public EditorMode getEditorMode() {
        return editorMode;
    }

    public void selectMarker(RoadMapMarker marker) {
        getTreePathForMarker(marker).ifPresent(path -> {
            markerTree.setSelectionPath(path);
            markerTree.scrollPathToVisible(path);
        });
    }

    private Optional<TreePath> getTreePathForMarker(RoadMapMarker marker) {
        Enumeration<TreeNode> nodeEnumeration = markerRootNode.preorderEnumeration();
        while (nodeEnumeration.hasMoreElements()) {
            DefaultMutableTreeNode element = (DefaultMutableTreeNode) nodeEnumeration.nextElement();
            Enumeration<TreeNode> leafNodeEnumeration = element.preorderEnumeration();
            while (leafNodeEnumeration.hasMoreElements()) {
                DefaultMutableTreeNode element2 = (DefaultMutableTreeNode) leafNodeEnumeration.nextElement();
                if (marker.equals(element2.getUserObject())) {
                    return Optional.of(new TreePath(element2.getPath()));
                }
            }
        }
        return Optional.empty();
    }
}
