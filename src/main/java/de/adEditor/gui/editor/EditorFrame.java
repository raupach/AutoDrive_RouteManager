package de.adEditor.gui.editor;

import de.adEditor.ApplicationContextProvider;
import de.adEditor.config.AdConfiguration;
import de.adEditor.gui.ConfigDialog;
import de.adEditor.gui.route.RoutesManagerPanel;
import de.adEditor.helper.IconHelper;
import de.adEditor.mapper.AutoDriveConfigToRoadMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class EditorFrame extends JFrame {


    public static final int EDITORSTATE_NOOP = -1;
    public static final int EDITORSTATE_MOVING = 0;
    public static final int EDITORSTATE_DELETING = 1;
    public static final int EDITORSTATE_CONNECTING = 2;
    public static final int EDITORSTATE_CREATING = 3;
    public static final int EDITORSTATE_DELETING_DESTINATION = 4;
    public static final int EDITORSTATE_CREATING_DESTINATION = 5;
    public static final String MOVE_NODES = "Move Nodes";
    public static final String CONNECT_NODES = "Connect Nodes";
    public static final String REMOVE_NODES = "Remove Nodes";
    public static final String REMOVE_DESTINATIONS = "Remove Destinations";
    public static final String CREATE_NODES = "Create Nodes";
    public static final String CREATE_DESTINATIONS = "Create Destinations";
    public static final String AUTO_DRIVE_COURSE_EDITOR_TITLE = "AutoDrive Course Editor 0.1";

    private MapPanel mapPanel;
    private RoutesManagerPanel routesManagerPanel;
    private JButton saveButton;
    private JButton loadImageButton;
    private JToggleButton removeNode;
    private JToggleButton removeDestination;
    private JToggleButton moveNode;
    private JToggleButton connectNodes;
    private JToggleButton createNode;
    private JToggleButton createDestination;
    private JRadioButton oneTimesMap;
    private JRadioButton fourTimesMap;
    private JRadioButton sixteenTimesMap;
    private JTabbedPane tabPane;
    private Component destinationTreePanel;

    public int editorState = EDITORSTATE_NOOP;
    private File xmlConfigFile;
    private boolean stale = false;
    private EditorMode editorMode = EditorMode.MOVE;

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
        mapPanel.setBackgroundMapImage (new BackgroundMapImage(loadMapImageFromDisk("Felsbrunn")));

        // set border for the panel
        mapPanel.setPreferredSize(new Dimension(1024, 768));
        mapPanel.setMinimumSize(new Dimension(1024, 768));

        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.fill= GridBagConstraints.BOTH;
        editorPanel.add(mapPanel, c);

//        EditorListener editorListener = new EditorListener(this);

//        JPanel buttonPanel = new JPanel(new FlowLayout());
//
//        JPanel configBox = new JPanel();
//        configBox.setBorder(BorderFactory.createTitledBorder("Config"));
//        buttonPanel.add(configBox);
//
//        JButton loadRoadMapButton = new JButton("Load");
//        loadRoadMapButton.addActionListener(editorListener);
//        loadRoadMapButton.setActionCommand("Load");
//        configBox.add(loadRoadMapButton);
//
//        saveButton = new JButton("Save");
//        saveButton.addActionListener(editorListener);
//        saveButton.setActionCommand("Save");
//        saveButton.setEnabled(false);
//        configBox.add(saveButton);
//
//        JPanel mapBox = new JPanel();
//        mapBox.setBorder(BorderFactory.createTitledBorder("Map and zoom factor"));
//        buttonPanel.add(mapBox);
//
//        loadImageButton = new JButton("Load Map");
//        loadImageButton.addActionListener(editorListener);
//        loadImageButton.setActionCommand("Load Image");
//        mapBox.add(loadImageButton);
//
//        ButtonGroup zoomGroup = new ButtonGroup();
//        oneTimesMap = new JRadioButton(" 1x");
//        oneTimesMap.addActionListener(editorListener);
//        oneTimesMap.setActionCommand("OneTimesMap");
//        oneTimesMap.setSelected(true);
//        mapBox.add(oneTimesMap);
//        zoomGroup.add(oneTimesMap);
//
//        fourTimesMap = new JRadioButton(" 4x");
//        fourTimesMap.addActionListener(editorListener);
//        fourTimesMap.setActionCommand("FourTimesMap");
//        mapBox.add(fourTimesMap);
//        zoomGroup.add(fourTimesMap);
//
//        sixteenTimesMap = new JRadioButton(" 16x");
//        sixteenTimesMap.addActionListener(editorListener);
//        sixteenTimesMap.setActionCommand("SixteenTimesMap");
//        mapBox.add(sixteenTimesMap);
//        zoomGroup.add(sixteenTimesMap);
//
//        JPanel nodeBox = new JPanel();
//        nodeBox.setBorder(BorderFactory.createTitledBorder("Nodes"));
//        buttonPanel.add(nodeBox);
//
//        moveNode = new JToggleButton("Move Nodes");
//        moveNode.addActionListener(editorListener);
//        moveNode.setActionCommand(MOVE_NODES);
//        nodeBox.add(moveNode);
//
//        connectNodes = new JToggleButton("Connect Nodes");
//        connectNodes.addActionListener(editorListener);
//        connectNodes.setActionCommand(CONNECT_NODES);
//        connectNodes.setName(CONNECT_NODES);
//        nodeBox.add(connectNodes);
//
//        removeNode = new JToggleButton("Delete Nodes");
//        removeNode.addActionListener(editorListener);
//        removeNode.setActionCommand(REMOVE_NODES);
//        nodeBox.add(removeNode);
//
//        removeDestination = new JToggleButton("Delete Destination");
//        removeDestination.addActionListener(editorListener);
//        removeDestination.setActionCommand(REMOVE_DESTINATIONS);
//        nodeBox.add(removeDestination);
//
//        createNode = new JToggleButton("Create Node");
//        createNode.addActionListener(editorListener);
//        createNode.setActionCommand(CREATE_NODES);
//        nodeBox.add(createNode);
//
//        createDestination = new JToggleButton("Create Destination");
//        createDestination.addActionListener(editorListener);
//        createDestination.setActionCommand(CREATE_DESTINATIONS);
//        nodeBox.add(createDestination);
//
//        updateButtons();
//        nodeBoxSetEnabled(false);
//        mapBoxSetEnabled(false);

        destinationTreePanel = createDestinationTreePanel();
        c.gridx = 2;
        c.gridy = 0;
        c.weighty = 0;
        c.weightx = 0;
        c.fill= GridBagConstraints.VERTICAL;
        editorPanel.add(destinationTreePanel, c );

//        editorPanel.add(buttonPanel, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private Component createDestinationTreePanel() {
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setPreferredSize(new Dimension(200,100));
        treePanel.setMinimumSize(new Dimension(200,100));
        treePanel.add (new Label("Markers"), BorderLayout.NORTH);

        //create the root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        //create the child nodes
        DefaultMutableTreeNode vegetableNode = new DefaultMutableTreeNode("Fields");
        vegetableNode.add(new DefaultMutableTreeNode("Train Station Mill"));
        vegetableNode.add(new DefaultMutableTreeNode("Silo tanken"));
        vegetableNode.add(new DefaultMutableTreeNode("Port Northwest"));
        vegetableNode.add(new DefaultMutableTreeNode("Bio Gas Plant"));
        DefaultMutableTreeNode fruitNode = new DefaultMutableTreeNode("Buy Points");
        fruitNode.add(new DefaultMutableTreeNode("Gas Station East"));
        fruitNode.add(new DefaultMutableTreeNode("Parkplatz Drescher"));
        fruitNode.add(new DefaultMutableTreeNode("Railroad Silo West"));
        fruitNode.add(new DefaultMutableTreeNode("Spinnery"));
        fruitNode.add(new DefaultMutableTreeNode("Gas Station South"));
        //add the child nodes to the root node
        root.add(vegetableNode);
        root.add(fruitNode);

        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);

        return treePanel;
    }

    private Component createToolBar() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        JButton openBtn = new JButton(new ImageIcon(IconHelper.getImageUrl("toolbar/open.png")));
        JToggleButton moveBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/move.png")));
        moveBtn.setSelected(true);
        JToggleButton autoNodeBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/autonode.png")));
        JToggleButton deleteBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/delete.png")));
        JToggleButton propBtn = new JToggleButton(new ImageIcon(IconHelper.getImageUrl("toolbar/tag.png")));
        propBtn.setSelected(true);
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

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.CENTER;
        c.insets = new Insets( 4, 4, 10, 4);
        c.anchor = GridBagConstraints.NORTH;
        toolbar.add(openBtn, c);
        c.insets = new Insets( 4, 4, 0, 4);
        c.gridy = 1;
        toolbar.add(moveBtn, c);
        c.gridy = 2;
        toolbar.add(autoNodeBtn, c);
        c.gridy = 3;
        toolbar.add(deleteBtn, c);
        c.gridy = 4;
        toolbar.add(propBtn, c);

        c.gridy = 5;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        toolbar.add(new Label(), c);

        return toolbar;
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

    private void nodeBoxSetEnabled(boolean enabled) {
        moveNode.setEnabled(enabled);
        connectNodes.setEnabled(enabled);
        removeNode.setEnabled(enabled);
        removeDestination.setEnabled(enabled);
        createNode.setEnabled(enabled);
        createDestination.setEnabled(enabled);
    }

    private void mapBoxSetEnabled(boolean enabled) {
        loadImageButton.setEnabled(enabled);
    }

    public void updateButtons() {
        moveNode.setSelected(false);
        connectNodes.setSelected(false);
        removeNode.setSelected(false);
        removeDestination.setSelected(false);
        createNode.setSelected(false);
        createDestination.setSelected(false);

        switch (editorState) {
            case EDITORSTATE_MOVING:
                moveNode.setSelected(true);
                break;
            case EDITORSTATE_DELETING:
                removeNode.setSelected(true);
                break;
            case EDITORSTATE_CONNECTING:
                connectNodes.setSelected(true);
                break;
            case EDITORSTATE_CREATING:
                createNode.setSelected(true);
                break;
            case EDITORSTATE_DELETING_DESTINATION:
                removeDestination.setSelected(true);
                break;
            case EDITORSTATE_CREATING_DESTINATION:
                createDestination.setSelected(true);
                break;
        }
    }


    public void prepareMapPanel(RoadMap roadMap, String mapName) {
        BufferedImage image = loadMapImageFromDisk(mapName);
        if (image != null) {
            mapPanel.setBackgroundMapImage(new BackgroundMapImage(image));
        }
        mapPanel.setRoadMap(roadMap);
        mapPanel.reset();
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
                        mapBoxSetEnabled(true);
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

    public void updateMapZoomFactor(int zoomFactor) {
        mapPanel.setMapZoomFactor(zoomFactor);
        mapPanel.repaint();
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
}
