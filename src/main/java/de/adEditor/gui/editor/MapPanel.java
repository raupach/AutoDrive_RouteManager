package de.adEditor.gui.editor;

import de.adEditor.gui.graph.GEdge;
import de.adEditor.helper.ADUtils;
import de.adEditor.gui.graph.GNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

public class MapPanel extends JPanel{

    private static Logger LOG = LoggerFactory.getLogger(MapPanel.class);

    private BufferedImage image;
    private BufferedImage resizedImage;

    private double x = 0.5;
    private double y = 0.5;
    private double zoomLevel = 1.0;
    private double lastZoomLevel = 0;
    private int mapZoomFactor = 1;
    private double nodeSize = 1;
    private EditorFrame editor;

    private RoadMap roadMap;
    private MapNode hoveredNode = null;
    private MapNode movingNode = null;
    private MapNode selected = null;

    private int mousePosX = 0;
    private int mousePosY = 0;

    private boolean isDragging = false;
    private boolean isDraggingNode = false;
    private int lastX = 0;
    private int lastY = 0;
    private Point2D rectangleStart;

    public MapPanel(EditorFrame editor) {
        this.editor = editor;

        MouseListener mouseListener = new MouseListener(this);
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        addMouseWheelListener(mouseListener);

        addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                resizeMap();
                repaint();
            }
        });
    }

    public void reset() {
        x = 0.5;
        y = 0.5;
        zoomLevel = 1.0;
        lastZoomLevel = 0;
        lastX = 0;
        lastY = 0;
        hoveredNode = null;
        movingNode = null;
        selected = null;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {

            if (lastZoomLevel != zoomLevel) {
                resizeMap();
            }

            g.clipRect(0, 0, this.getWidth(), this.getHeight());

            g.drawImage(resizedImage, 0, 0, this); // see javadoc for more info on the parameters

            if (roadMap != null) {
                long start = System.currentTimeMillis();
                g.setColor(Color.GREEN);
                Graph<GNode, GEdge> graph = roadMap.getGraph();
                graph.edgeSet().forEach(e -> {
                    GNode source = graph.getEdgeSource(e);
                    GNode target = graph.getEdgeTarget(e);

                    Point2D nodePos = worldPosToScreenPos(source.getX(), source.getZ() );
                    Point2D outPos = worldPosToScreenPos(target.getX(), target.getZ());
                    drawArrowBetween(g, nodePos, outPos, false);

                });




//                for (MapNode mapNode : roadMap.getMapNodes()) {
//                    g.setColor(Color.BLUE);
//                    if (mapNode == selected) {
//                        g.setColor(Color.PINK);
//                    }
//                    Point2D nodePos = worldPosToScreenPos(mapNode.getX(), mapNode.getZ() );
//                    g.fillArc((int) (nodePos.getX() - ((nodeSize * zoomLevel) * 0.5)), (int) (nodePos.getY() - ((nodeSize * zoomLevel) * 0.5)), (int) (nodeSize * zoomLevel), (int) (nodeSize * zoomLevel), 0, 360);
//                    for (MapNode outgoing : mapNode.getOutgoing()) {
//                        g.setColor(Color.GREEN);
//                        boolean dual = RoadMap.isDual(mapNode, outgoing);
//                        if (dual) {
//                            g.setColor(Color.RED);
//                        }
//                        Point2D outPos = worldPosToScreenPos(outgoing.x, outgoing.z);
//                        drawArrowBetween(g, nodePos, outPos, dual);
//                    }
//                }


//                for (MapMarker mapMarker : roadMap.getMapMarkers()) {
//                    g.setColor(Color.BLUE);
//                    Point2D nodePos = worldPosToScreenPos(mapMarker.mapNode.getX() + 3, mapMarker.mapNode.getZ());
//                    g.drawString(mapMarker.name, (int) (nodePos.getX()), (int) (nodePos.getY()));
//                }
//
//                if (selected != null && editor.editorState == EditorFrame.EDITORSTATE_CONNECTING) {
//                    Point2D nodePos = worldPosToScreenPos(selected.x, selected.z);
//                    g.setColor(Color.WHITE);
//                    g.drawLine((int) (nodePos.getX()), (int) (nodePos.getY()), mousePosX, mousePosY);
//                }
//
//                if (hoveredNode != null) {
//                    g.setColor(Color.WHITE);
//                    Point2D nodePos = worldPosToScreenPos(hoveredNode.x, hoveredNode.z);
//                    g.fillArc((int) (nodePos.getX() - ((nodeSize * zoomLevel) * 0.5)), (int) (nodePos.getY() - ((nodeSize * zoomLevel) * 0.5)), (int) (nodeSize * zoomLevel), (int) (nodeSize * zoomLevel), 0, 360);
//                    for (MapMarker mapMarker : roadMap.getMapMarkers()) {
//                        if (hoveredNode.id == mapMarker.mapNode.id) {
//                            Point2D nodePosMarker = worldPosToScreenPos(mapMarker.mapNode.getX() + 3, mapMarker.mapNode.getZ());
//                            g.drawString(mapMarker.name, (int) (nodePosMarker.getX()), (int) (nodePosMarker.getY()));
//                        }
//                    }
//                }
//
//                if (rectangleStart != null) {
//                    g.setColor(Color.WHITE);
//                    int width = (int) (mousePosX - rectangleStart.getX());
//                    int height = (int) (mousePosY - rectangleStart.getY());
//                    int x = Double.valueOf(rectangleStart.getX()).intValue();
//                    int y = Double.valueOf(rectangleStart.getY()).intValue();
//                    if (width < 0) {
//                        x += width;
//                        width = -width;
//                    }
//                    if (height < 0) {
//                        y += height;
//                        height = -height;
//                    }
//                    g.drawRect(x, y, width, height);
//                }

//                LOG.info("paint {}ms", System.currentTimeMillis()-start);
            }
        }
    }

    private void resizeMap() {
        if (image != null) {
            int widthScaled = (int) (getWidth() / zoomLevel);
            int heightScaled = (int) (getHeight() / zoomLevel);

            double maxX = 1 - (((getWidth() * 0.5) / zoomLevel) / image.getWidth());
            double minX = (((getWidth() * 0.5) / zoomLevel) / image.getWidth());
            double maxY = 1 - (((getHeight() * 0.5) / zoomLevel) / image.getHeight());
            double minY = (((getHeight() * 0.5) / zoomLevel) / image.getHeight());

            x = Math.min(x, maxX);
            x = Math.max(x, minX);
            y = Math.min(y, maxY);
            y = Math.max(y, minY);

            int centerX = (int) (x * image.getWidth());
            int centerY = (int) (y * image.getHeight());

            int offsetX = (centerX - (widthScaled / 2));
            int offsetY = (centerY - (heightScaled / 2));

            BufferedImage croppedImage = image.getSubimage(offsetX, offsetY, widthScaled, heightScaled);

            resizedImage = new BufferedImage(getWidth(), getHeight(), image.getType());
            Graphics2D g2 = resizedImage.createGraphics();
            g2.drawImage(croppedImage, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();

            lastZoomLevel = zoomLevel;
        }
    }

    public void moveMapBy(int diffX, int diffY) {
        LOG.debug("moveMapBy {}, {}", diffX, diffY);
        if (roadMap == null || image == null) {
            return;
        }
        x -= diffX / (zoomLevel * image.getWidth());
        y -= diffY / (zoomLevel * image.getHeight());

        LOG.debug("x {}, y {}", x, y);
        resizeMap();
        LOG.debug("resize");
        repaint();
    }

    public void increaseZoomLevelBy(int rotations) {
        double step = rotations * (zoomLevel * 0.1);
        if (roadMap == null || image == null) {
            return;
        }
        if (((getWidth()/(zoomLevel - step)) > image.getWidth()) || ((getHeight()/(zoomLevel - step)) > image.getHeight())) {
            return;
        }

        if ((zoomLevel - step) < 30) {
            zoomLevel -= step;
            resizeMap();
            repaint();
        }
    }

    public void moveNodeBy(MapNode node, int diffX, int diffY) {
        node.x +=  ((diffX * mapZoomFactor) / zoomLevel);
        node.z += ((diffY * mapZoomFactor) / zoomLevel);
        editor.setStale(true);
        repaint();
    }

    public MapNode getNodeAt(double posX, double posY) {
        if (roadMap != null && image != null) {
            for (MapNode mapNode : roadMap.getMapNodes()) {
                double currentNodeSize = nodeSize * zoomLevel * 0.5;

                Point2D outPos = worldPosToScreenPos(mapNode.getX(), mapNode.getZ());

                if (posX < outPos.getX() + currentNodeSize && posX > outPos.getX() - currentNodeSize && posY < outPos.getY() + currentNodeSize && posY > outPos.getY() - currentNodeSize) {
                    return mapNode;
                }
            }
        }
        return null;
    }

    public void removeNode(MapNode toDelete) {
        roadMap.removeMapNode(toDelete);
        editor.setStale(true);
        repaint();
    }

    public void removeDestination(MapNode toDelete) {
        MapMarker destinationToDelete = null;
        for (MapMarker mapMarker : roadMap.getMapMarkers()) {
            if (mapMarker.mapNode.id == toDelete.id) {
                destinationToDelete = mapMarker;
            }
        }
        if (destinationToDelete != null) {
            roadMap.removeMapMarker(destinationToDelete);
            editor.setStale(true);
            repaint();
        }
    }

    public void createNode(int screenX, int screenY) {
        if (roadMap == null || image == null) {
            return;
        }
        LOG.info("createNode: {}, {}", screenX, screenY);
        MapNode mapNode = new MapNode(roadMap.getMapNodes().size()+1, screenX, -1, screenY);

        roadMap.getMapNodes().add(mapNode);
        editor.setStale(true);
        repaint();
    }

    public Point2D screenPosToWorldPos(int screenX, int screenY) {
        double centerX = (x * (image.getWidth()));
        double centerY = (y * (image.getHeight()));

        double widthScaled = (getWidth() / zoomLevel);
        double heightScaled = (getHeight() / zoomLevel);

        double topLeftX = centerX - (widthScaled/2);
        double topLeftY = centerY - (heightScaled/2);

        double diffScaledX = screenX / zoomLevel;
        double diffScaledY = screenY / zoomLevel;

        LOG.info("topLeftX: {}, diffScaledX: {}", topLeftX, diffScaledX);
        LOG.info("topLeftY: {}, diffScaledY: {}", topLeftY, diffScaledY);

        int centerPointOffset = 1024 * mapZoomFactor;

        double worldPosX = ((topLeftX + diffScaledX) * mapZoomFactor) - centerPointOffset;
        double worldPosY = ((topLeftY + diffScaledY) * mapZoomFactor) - centerPointOffset;

        return new Point2D.Double(worldPosX, worldPosY);
    }

    public Point2D worldPosToScreenPos(double worldX, double worldY) {

        int centerPointOffset = 1024 * mapZoomFactor;

        worldX += centerPointOffset;
        worldY += centerPointOffset;

        double scaledX = (worldX/mapZoomFactor) * zoomLevel;
        double scaledY = (worldY/mapZoomFactor) * zoomLevel;

        double centerXScaled = (x * (image.getWidth()*zoomLevel));
        double centerYScaled = (y * (image.getHeight()*zoomLevel));

        double topLeftX = centerXScaled - ((double) getWidth() /2);
        double topLeftY = centerYScaled - ((double) getHeight()/2);

        return new Point2D.Double(scaledX - topLeftX,scaledY - topLeftY);
    }

    public void createConnectionBetween(MapNode start, MapNode target) {
        if (start == target) {
            return;
        }
        if (!start.outgoing.contains(target)) {
            start.outgoing.add(target);
            target.incoming.add(start);
        }
        else {
            start.outgoing.remove(target);
            target.incoming.remove(start);
        }
        editor.setStale(true);
    }

    public void createDestinationAt(MapNode mapNode, String destinationName) {
        if (mapNode != null && destinationName != null && destinationName.length() > 0) {
            MapMarker mapMarker = new MapMarker(mapNode, destinationName, "All");
            roadMap.addMapMarker(mapMarker);
            editor.setStale(true);
        }
    }

    public void removeAllNodesInScreenArea(Point2D rectangleStartScreen, Point2D rectangleEndScreen) {
        if (roadMap == null || image == null) {
            return;
        }
        int screenStartX = (int) rectangleStartScreen.getX();
        int screenStartY = (int) rectangleStartScreen.getY();
        int width = (int)(rectangleEndScreen.getX() - rectangleStartScreen.getX());
        int height = (int)(rectangleEndScreen.getY() - rectangleStartScreen.getY());

        Rectangle2D rectangle = ADUtils.getNormalizedRectangleFor(screenStartX, screenStartY, width, height);
        screenStartX = (int) rectangle.getX();
        screenStartY = (int) rectangle.getY();
        width = (int) rectangle.getWidth();
        height = (int) rectangle.getHeight();

        LinkedList<MapNode> toDelete = new LinkedList<>();
        for (MapNode mapNode : roadMap.getMapNodes()) {
            double currentNodeSize = nodeSize * zoomLevel * 0.5;

            Point2D nodePos = worldPosToScreenPos(mapNode.getX(), mapNode.getZ());

            if (screenStartX < nodePos.getX() + currentNodeSize && (screenStartX + width) > nodePos.getX() - currentNodeSize && screenStartY < nodePos.getY() + currentNodeSize && (screenStartY + height) > nodePos.getY() - currentNodeSize) {
                toDelete.add(mapNode);
            }
        }
        if (!toDelete.isEmpty()) {
            editor.setStale(true);

            for (MapNode node : toDelete) {
                roadMap.removeMapNode(node);
            }
        }
    }

    public void drawArrowBetween(Graphics g, Point2D start, Point2D target, boolean dual) {
        double vecX = start.getX() - target.getX();
        double vecY = start.getY() - target.getY();

        double angleRad = Math.atan2(vecY, vecX);

        angleRad = normalizeAngle(angleRad);

        double arrowLength = 1.3 * zoomLevel;

        double arrowLeft = normalizeAngle(angleRad + Math.toRadians(-20));
        double arrowRight = normalizeAngle(angleRad + Math.toRadians(20));

        double arrowLeftX = target.getX() + Math.cos(arrowLeft) * arrowLength;
        double arrowLeftY = target.getY() + Math.sin(arrowLeft) * arrowLength;

        double arrowRightX = target.getX() + Math.cos(arrowRight) * arrowLength;
        double arrowRightY = target.getY() + Math.sin(arrowRight) * arrowLength;

        g.drawLine((int) (start.getX()), (int) (start.getY()), (int) (target.getX()), (int) (target.getY()));
        g.drawLine((int) (target.getX()), (int) (target.getY()), (int) arrowLeftX, (int) arrowLeftY);
        g.drawLine((int) (target.getX()), (int) (target.getY()), (int) arrowRightX, (int) arrowRightY);

        if (dual) {
            angleRad = normalizeAngle(angleRad+Math.PI);

            arrowLeft = normalizeAngle(angleRad + Math.toRadians(-20));
            arrowRight = normalizeAngle(angleRad + Math.toRadians(20));

            arrowLeftX = start.getX() + Math.cos(arrowLeft) * arrowLength;
            arrowLeftY = start.getY() + Math.sin(arrowLeft) * arrowLength;
            arrowRightX = start.getX() + Math.cos(arrowRight) * arrowLength;
            arrowRightY = start.getY() + Math.sin(arrowRight) * arrowLength;

            g.drawLine((int) (start.getX()), (int) (start.getY()), (int) arrowLeftX, (int) arrowLeftY);
            g.drawLine((int) (start.getX()), (int) (start.getY()), (int) arrowRightX, (int) arrowRightY);
        }
    }

    public double normalizeAngle(double input) {
        if (input > (2*Math.PI)) {
            input = input - (2*Math.PI);
        }
        else {
            if (input < -(2*Math.PI)) {
                input = input + (2*Math.PI);
            }
        }

        return input;
    }

    public void mouseButton1Clicked(int x, int y) {
        if (editor.editorState == EditorFrame.EDITORSTATE_CONNECTING) {
            movingNode = getNodeAt(x, y);
            if (movingNode != null) {
                if (selected == null) {
                    selected = movingNode;
                } else {
                    createConnectionBetween(selected, movingNode);
                    selected = null;
                }
                repaint();
            }
        }
        if (editor.editorState == EditorFrame.EDITORSTATE_CREATING) {
            Point2D worldPos = screenPosToWorldPos(x, y);
            createNode((int)worldPos.getX(), (int)worldPos.getY());
        }
        if (editor.editorState == EditorFrame.EDITORSTATE_CREATING_DESTINATION) {
            movingNode = getNodeAt(x, y);
            if (movingNode != null) {
                String destinationName = JOptionPane.showInputDialog("New destination name:", "" + movingNode.id );
                if (destinationName != null) {
                    createDestinationAt(movingNode, destinationName);
                    repaint();
                }
            }
        }
    }

    public void mouseMoved(int x, int y) {
        mousePosX = x;
        mousePosY = y;
        if (editor.editorState == EditorFrame.EDITORSTATE_CONNECTING && selected != null) {
            repaint();
        }
        movingNode = getNodeAt(x, y);
        if (movingNode != hoveredNode) {
            hoveredNode = movingNode;
            repaint();
        }
    }

    public void mouseDragged(int x, int y) {
        mousePosX = x;
        mousePosY = y;
        if (isDragging) {
            int diffX = x - lastX;
            int diffY = y - lastY;
            lastX = x;
            lastY = y;
            moveMapBy(diffX, diffY);
        }
        else {
            if (isDraggingNode) {
                int diffX = x - lastX;
                int diffY = y - lastY;
                lastX = x;
                lastY = y;
                moveNodeBy(movingNode, diffX, diffY);
            }
        }
        if (editor.editorState == EditorFrame.EDITORSTATE_DELETING && rectangleStart != null) {
            repaint();
        }
    }

    public void mouseButton1Pressed(int x, int y) {
        isDragging = true;
        lastX = x;
        lastY = y;
        movingNode = getNodeAt(x, y);
        if (movingNode != null) {
            isDragging = false;
            if (editor.editorState == EditorFrame.EDITORSTATE_MOVING) {
                isDraggingNode = true;
            }
            if (editor.editorState == EditorFrame.EDITORSTATE_DELETING) {
                removeNode(movingNode);
            }
            if (editor.editorState == EditorFrame.EDITORSTATE_DELETING_DESTINATION) {
                removeDestination(movingNode);
            }
        }
    }

    public void mouseButton3Pressed(int x, int y) {
        LOG.info("Rectangle start set at {}/{}", x, y);
        rectangleStart = new Point2D.Double(x, y);
    }

    public void mouseButton1Released() {
        isDragging = false;
        isDraggingNode = false;
    }

    public void mouseButton3Released(int x, int y) {
        Point2D rectangleEnd = new Point2D.Double(x, y);
        LOG.info("Rectangle end set at {}/{}", x, y);
        if (rectangleStart != null) {
            if (editor.editorState == EditorFrame.EDITORSTATE_DELETING) {

                LOG.info("Removing all nodes in area");
                removeAllNodesInScreenArea(rectangleStart, rectangleEnd);
                repaint();
            }
        }
        rectangleStart = null;
    }


    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public RoadMap getRoadMap() {
        return roadMap;
    }

    public void setRoadMap(RoadMap roadMap) {
        this.roadMap = roadMap;
    }

    public void setMapZoomFactor(int mapZoomFactor) {
        this.mapZoomFactor = mapZoomFactor;
    }
}
