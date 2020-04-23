package de.adEditor.gui.editor;


import de.adEditor.gui.graph.GEdge;
import de.adEditor.gui.graph.GNode;
import org.apache.commons.collections4.CollectionUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MapPanel2 extends JPanel {

    private static final String ESCAPE_KEY = "ESCAPE_KEY";
    private static final String BACKSPACE_KEY = "BACKSPACE_KEY";
    private static final String DELETE_KEY = "DELETE_KEY";
    private static Logger LOG = LoggerFactory.getLogger(MapPanel2.class);

    private static final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private Rectangle selectedRectangle = null;
    private Point selectedPoint = null;



    private enum MapPanelMode {NONE, DRAGGING_NODE, SELECTING_RECTANGLE, DRAWING}
    private EditorFrame editor;
    private RoadMap roadMap = new RoadMap();
    private BackgroundMapImage backgroundMapImage;
    private boolean mapMove = false;
    private Point lastMousePos = new Point();
    private Point mousePos;
    private GNode tempLastNode;
    private MapPanelMode mapPanelMode = MapPanelMode.NONE;
    private AffineTransform tx = new AffineTransform();
    private final static Polygon arrowHead;
    private GNode touchedNode;
    private GEdge touchedEdgeMidpoint;
    private Set<GNode> selectedNodes = new HashSet<>();

    static {
        arrowHead = new Polygon();
//        arrowHead.addPoint(0, 5);
//        arrowHead.addPoint(-5, -5);
//        arrowHead.addPoint(5, -5);
        arrowHead.addPoint(0, 0);
        arrowHead.addPoint(-5, -15);
        arrowHead.addPoint(5, -15);
    }

    public MapPanel2(EditorFrame editor) {
        this.editor = editor;
        setFocusable(true);

        addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                backgroundMapImage.move(0,0, e.getComponent().getWidth(), e.getComponent().getHeight());
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mousePos = e.getPoint();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mouseButton1Released(e.getX(), e.getY());
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    mouseButton3Released(e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mousePos = e.getPoint();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mouseButton1Pressed(e.getX(), e.getY());
                }
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    mouseButton3Pressed(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (editor.getEditorMode().equals(EditorMode.DRAW)) {
                    setCursor(crosshairCursor);
                    if (mapPanelMode.equals(MapPanelMode.DRAWING)) {
                        mousePos = e.getPoint();
                        repaint();
                    }
                }
                if (editor.getEditorMode().equals(EditorMode.MOVE)) {
                    setCursor(Cursor.getDefaultCursor());

                    boolean shouldRepaint = false;
                    if (touchedEdgeMidpoint !=null) {
                        touchedEdgeMidpoint = null;
                        shouldRepaint = true;
                    }
                    findEdgeMidpointAt(e.getX() ,e.getY()).ifPresent(edge -> {touchedEdgeMidpoint = edge; repaint();});
                    if (shouldRepaint) {
                        repaint();
                    }
                }
                if (editor.getEditorMode().equals(EditorMode.MOVE) || editor.getEditorMode().equals(EditorMode.DRAW)) {
                    boolean shouldRepaint = false;
                    if (touchedNode !=null) {
                        touchedNode = null;
                        shouldRepaint = true;
                    }
                    findNodeAt(e.getX() ,e.getY()).ifPresent(node -> {touchedNode = node; repaint();});

                    if (shouldRepaint) {
                        repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mousePos = e.getPoint();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseDraggedButton1(e.getX(), e.getY());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    mouseDraggedButton3(e.getX(), e.getY());
                }
            }
        });

        addMouseWheelListener(mouseWheelEvent -> mouseWheelMovedd(mouseWheelEvent.getWheelRotation()));


        InputMap iMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap aMap = getActionMap();
        iMap.put(KeyStroke.getKeyStroke("ESCAPE"), ESCAPE_KEY);
        aMap.put(ESCAPE_KEY, new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                escape();
            }
        });
        iMap.put(KeyStroke.getKeyStroke("BACK_SPACE"), BACKSPACE_KEY);
        aMap.put(BACKSPACE_KEY, new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                backSpace();
            }
        });
        iMap.put(KeyStroke.getKeyStroke("DELETE"), DELETE_KEY);
        aMap.put(DELETE_KEY, new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                delete();
            }
        });
    }

    private Optional<GNode> findNodeAt (int x, int y) {
        Rectangle r = new Rectangle(x-6, y-6, 12,12);
        return findNodesInRect(r).stream().findFirst();
    }

    private Optional<GEdge> findEdgeMidpointAt (int x, int y) {
        Rectangle r = new Rectangle(x-6, y-6, 12,12);
        return findEdgeMidpointInRect(r).stream().findFirst();
    }


    private void mouseDraggedButton1(int x, int y) {
        if (editor.getEditorMode().equals(EditorMode.MOVE) && mapPanelMode.equals(MapPanelMode.DRAGGING_NODE)) {
            Point2D lastMouseWorldPos = screenPosToWorldPos(lastMousePos);
            Point2D currentMouseWorldPos = screenPosToWorldPos(new Point(x, y));
            double dx =  currentMouseWorldPos.getX() - lastMouseWorldPos.getX();
            double dy = currentMouseWorldPos.getY() -lastMouseWorldPos.getY() ;
            getSelectedNodes().forEach(node-> {
                node.setX(node.getX() +dx);
                node.setY(node.getY() +dy);
                roadMap.getGraph().edgesOf(node).forEach(edge->{
                    GNode source = roadMap.getGraph().getEdgeSource(edge);
                    GNode target = roadMap.getGraph().getEdgeTarget(edge);
                    edge.setMidpoint(midpoint(source, target));
                });
            });
            repaint();
        }
        if (editor.getEditorMode().equals(EditorMode.MOVE) && mapPanelMode.equals(MapPanelMode.SELECTING_RECTANGLE)) {
            if (selectedRectangle != null) {
                selectedRectangle = (new Line2D.Double(new Point2D.Double(selectedPoint.x,selectedPoint.y), new Point2D.Double(x,y))).getBounds();
                repaint();
            }
        }
        lastMousePos = new Point(x,y);
    }

    private void mouseWheelMovedd(int wheelRotation) {
//        LOG.debug("mouseWheelMovedd {}", wheelRotation);
        Rectangle rect1 = backgroundMapImage.getScaledRectangle();


        backgroundMapImage.zoom(wheelRotation*-1);
        Rectangle rect2 = backgroundMapImage.getScaledRectangle();
        int dx = (rect2.width - rect1.width) / 2;
        int dy = (rect2.height - rect1.height) / 2;

        backgroundMapImage.move(dx, dy, this.getWidth(), this.getHeight());
        repaint();
    }

    private void mouseButton3Released(int x, int y) {
        mapMove = false;
    }

    private void mouseButton1Released(int x, int y) {
        if(editor.getEditorMode().equals(EditorMode.DRAW)) {
            mousePos = new Point(x, y);
            if (mapPanelMode.equals(MapPanelMode.NONE)) {
                mapPanelMode = MapPanelMode.DRAWING;
            }
            GNode newVertex = findNodeAt(x, y).orElseGet(() -> screenPosToWorldVertex(x, y));
            Graph<GNode, GEdge> graph = roadMap.getGraph();
            graph.addVertex(newVertex);
            if (tempLastNode !=null) {
                double d = Point2D.distance(tempLastNode.getX(), tempLastNode.getY(), newVertex.getX(), newVertex.getY());
                graph.addEdge(tempLastNode, newVertex, new GEdge(midpoint(new Point2D.Double(tempLastNode.getX(), tempLastNode.getY()), new Point2D.Double(newVertex.getX(), newVertex.getY())), true));
            }
            tempLastNode = newVertex;
            repaint();
        } else if (editor.getEditorMode().equals(EditorMode.MOVE)) {
            if (mapPanelMode.equals(MapPanelMode.SELECTING_RECTANGLE) && selectedRectangle != null) {
                findNodesInRect (selectedRectangle).forEach(n -> n.setSelected(true));
                selectedRectangle = null;
            }
            mapPanelMode = MapPanelMode.NONE;
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

    }

    private Set<GNode> findNodesInRect(Rectangle r) {
        return roadMap.getGraph().vertexSet().stream()
                .filter(n -> r.contains(worldVertexToScreenPos(n)))
                .collect(Collectors.toSet());
    }

    private Set<GEdge> findEdgeMidpointInRect(Rectangle r) {
        return roadMap.getGraph().edgeSet().stream()
                .filter(n -> r.contains(worldPosToScreenPos(n.getMidpoint())))
                .collect(Collectors.toSet());
    }

    private GNode screenPosToWorldVertex(int x, int y) {
        Point2D worldPos = screenPosToWorldPos (new Point (x, y));
        return new GNode (worldPos);
    }

    private Point2D screenPosToWorldPos(Point point) {
        Rectangle viewPort = backgroundMapImage.getRectangle();
        double scaleFactor = backgroundMapImage.getScaleFactor();
        double worldPosX = (point.x  + viewPort.x) / scaleFactor;
        double worldPosY = (point.y  + viewPort.y) / scaleFactor;
        return new Point2D.Double(worldPosX, worldPosY);
    }

    private Point worldVertexToScreenPos(GNode gNode) {
        Rectangle viewPort = backgroundMapImage.getRectangle();
        double scaleFactor = backgroundMapImage.getScaleFactor();
        double screenPosX = (gNode.getX()*scaleFactor) - viewPort.x;
        double screenPosY = (gNode.getY()*scaleFactor) - viewPort.y;
        return new Point((int) screenPosX, (int) screenPosY);
    }

    private Point worldPosToScreenPos(Point2D p) {
        Rectangle viewPort = backgroundMapImage.getRectangle();
        double scaleFactor = backgroundMapImage.getScaleFactor();
        double screenPosX = (p.getX()*scaleFactor) - viewPort.x;
        double screenPosY = (p.getY()*scaleFactor) - viewPort.y;
        return new Point((int) screenPosX, (int) screenPosY);
    }

    private void mouseDraggedButton3(int x, int y) {
        if (mapPanelMode.equals(MapPanelMode.DRAWING)) {
            mousePos = new Point(x, y);
        }
        if (mapMove) {
            backgroundMapImage.move( lastMousePos.x-x, lastMousePos.y-y, this.getWidth(), this.getHeight());
            lastMousePos = new Point(x,y);
            repaint();
        }

    }

    private void mouseButton3Pressed(int x, int y) {
        LOG.debug("mouseButton3Pressed");
        mapMove = true;
        lastMousePos = new Point(x,y);
    }

    private void mouseButton1Pressed(int x, int y) {
        if (editor.getEditorMode().equals(EditorMode.MOVE)) {
            touchedNode = null;

            Optional<GNode> optionalGNode = findNodeAt(x, y);
            if (optionalGNode.isPresent()){
                GNode node = optionalGNode.get();
                if (!node.isSelected()) {
                    clearSelectedNodes();
                    node.setSelected(true);
                }
                mapPanelMode = MapPanelMode.DRAGGING_NODE;
            } else {
                clearSelectedNodes();
                mapPanelMode = MapPanelMode.SELECTING_RECTANGLE;
                selectedRectangle = new Rectangle(x,y, 1,1);
                selectedPoint = new Point(x,y);
            }
            lastMousePos = new Point(x,y);
            setCursor(handCursor);
            repaint();
        }
    }

    private void clearSelectedNodes() {
        roadMap.getGraph().vertexSet().forEach(node->node.setSelected(false));
    }

    private Set<GNode> getSelectedNodes() {
        return roadMap.getGraph().vertexSet().parallelStream().filter(GNode::isSelected).collect(Collectors.toSet());
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        long start = System.currentTimeMillis();

        Graphics2D g2 = (Graphics2D) g;
        backgroundMapImage.draw(g2);

        Stroke stroke_1 = new BasicStroke(1);
        Stroke stroke_2 = new BasicStroke(2);

        if (touchedNode!= null) {
            int alpha = 127; // 50% transparent
            Color myColour = new Color(255, 0, 0, alpha);
            g.setColor(myColour);
            Point p = worldVertexToScreenPos(touchedNode);
            g.fillRect(p.x-6, p.y-6, 12, 12);
        }


        for (GNode p : roadMap.getGraph().vertexSet()) {
            roadMap.getGraph().outgoingEdgesOf(p).forEach(outEdge -> {
                GNode sourceNode = roadMap.getGraph().getEdgeSource(outEdge);
                GNode targetNode = roadMap.getGraph().getEdgeTarget(outEdge);
                ((Graphics2D) g).setStroke(stroke_2);
                drawEdge(g, sourceNode, targetNode);
            });
            g.setColor(Color.yellow);
            ((Graphics2D) g).setStroke(stroke_1);
            drawVertex(g, p);
        }

        if (editor.getEditorMode().equals(EditorMode.MOVE)) {
            g.setColor(Color.yellow);
            ((Graphics2D) g).setStroke(stroke_1);
            roadMap.getGraph().edgeSet().forEach(edge -> {
                Point2D midpoint = edge.getMidpoint();
                if (midpoint != null) {
                    if (edge.equals(touchedEdgeMidpoint)) {
                        g.setColor(Color.red);
                    } else {
                        g.setColor(Color.yellow);
                    }

                    Point p = worldPosToScreenPos(midpoint);
                    g.drawLine(p.x - 4, p.y, p.x + 4, p.y);
                    g.drawLine(p.x, p.y - 4, p.x, p.y + 4);
                }
            });
        }

        if (mapPanelMode.equals(MapPanelMode.DRAWING) && tempLastNode != null && mousePos != null) {
            ((Graphics2D) g).setStroke(stroke_2);
            g.setColor(Color.RED);
            Point p = worldVertexToScreenPos(tempLastNode);
            g.drawLine(p.x, p.y, mousePos.x, mousePos.y);
        }

        if (mapPanelMode.equals(MapPanelMode.SELECTING_RECTANGLE) && selectedRectangle!=null) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(selectedRectangle.x, selectedRectangle.y, selectedRectangle.width, selectedRectangle.height);
        }

        LOG.debug("paintComponent end {}ms.", System.currentTimeMillis()-start);
    }

    private void drawEdge(Graphics g, GNode sourceNode, GNode targetNode) {
        GEdge edge = roadMap.getGraph().getEdge(sourceNode, targetNode);
        Point sourcePoint = worldVertexToScreenPos(sourceNode);
        Point targetPoint = worldVertexToScreenPos(targetNode);
        if (edge.isSelected()) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.GREEN);
        }
        g.drawLine(sourcePoint.x, sourcePoint.y, targetPoint.x, targetPoint.y);
        drawArrowHead((Graphics2D) g, sourcePoint, targetPoint);
    }

    private void drawVertex(Graphics g, GNode gNode) {
        Point p = worldVertexToScreenPos(gNode);
        if (gNode.isSelected()) {
            g.setColor(Color.RED);
            g.fillRect(p.x-4, p.y-4, 8, 8);
        }
        else {
            g.drawRect(p.x - 2, p.y - 2, 4, 4);
        }
    }


    private void drawArrowHead(Graphics2D g2d, Point p1, Point p2) {
        tx.setToIdentity();
        double angle = Math.atan2(p2.y-p1.y, p2.x-p1.x);
        tx.translate(p2.x, p2.y);
        tx.rotate((angle-Math.PI/2d));

        Graphics2D g = (Graphics2D) g2d.create();
        g.setTransform(tx);
        g.fill(arrowHead);
        g.dispose();
    }

    private void drawArrowHead(Graphics2D g2d, GNode p1, GNode p2) {
        tx.setToIdentity();
        double angle = Math.atan2(p2.getY()-p1.getY(), p2.getX()-p1.getX());
        tx.translate(p2.getX(), p2.getY());
        tx.rotate((angle-Math.PI/2d));

        Graphics2D g = (Graphics2D) g2d.create();
        g.setTransform(tx);
        g.fill(arrowHead);
        g.dispose();
    }

    public void setRoadMap(RoadMap roadMap) {
        this.roadMap = roadMap;
    }

    public RoadMap getRoadMap() {
        return roadMap;
    }

    public void setMapZoomFactor(int zoomFactor) {

    }

    public BackgroundMapImage getBackgroundMapImage() {
        return backgroundMapImage;
    }

    public void setBackgroundMapImage(BackgroundMapImage backgroundMapImage) {
        this.backgroundMapImage = backgroundMapImage;
    }

    public void escape() {
        if (mapPanelMode.equals(MapPanelMode.DRAWING)){
            mapPanelMode = MapPanelMode.NONE;
            roadMap.getGraph().edgeSet().forEach(e->e.setSelected(false));
            tempLastNode = null;
            repaint();
        }
    }

    public void backSpace() {
        if (mapPanelMode.equals(MapPanelMode.DRAWING) && tempLastNode != null) {
            Graph<GNode, GEdge> graph = roadMap.getGraph();
            Optional<GEdge> edgeOptional = graph.incomingEdgesOf(tempLastNode).stream().findFirst();
            if (edgeOptional.isPresent()) {
                GEdge incomingEdge = edgeOptional.get();
                GNode source = graph.getEdgeSource(incomingEdge);
                graph.removeVertex(tempLastNode);
                graph.removeEdge(incomingEdge);
                tempLastNode = source;
            }else {
                graph.removeVertex(tempLastNode);
                tempLastNode = null;
            }
            repaint();
        }
    }

    private void delete() {
        if (editor.getEditorMode().equals(EditorMode.MOVE)) {
            Graph<GNode, GEdge> graph = roadMap.getGraph();

            getSelectedNodes().forEach(selectedNode -> {
                List<GEdge> incomingEdges = new ArrayList<>();
                List<GEdge> outgoingEdges = new ArrayList<>();
                CollectionUtils.addAll(incomingEdges, graph.incomingEdgesOf(selectedNode));
                CollectionUtils.addAll(outgoingEdges, graph.outgoingEdgesOf(selectedNode));
                if (outgoingEdges.isEmpty()) {
                    graph.removeAllEdges(incomingEdges);
                    graph.removeVertex(selectedNode);
                } else if (incomingEdges.isEmpty()) {
                    graph.removeAllEdges(outgoingEdges);
                    graph.removeVertex(selectedNode);
                } else if (outgoingEdges.size() == 1 && incomingEdges.size() == 1) {
                    GNode newTarget = graph.getEdgeTarget(outgoingEdges.get(0));
                    GNode newSource = graph.getEdgeSource(incomingEdges.get(0));
                    graph.removeAllEdges(incomingEdges);
                    graph.removeAllEdges(outgoingEdges);
                    graph.removeVertex(selectedNode);
                    graph.addEdge(newSource, newTarget);
                } else {
                    graph.removeAllEdges(incomingEdges);
                    graph.removeAllEdges(outgoingEdges);
                    graph.removeVertex(selectedNode);
                }

            });

            repaint();
        }
    }

    private Point2D.Double midpoint(Point2D p1, Point2D p2) {
        return new Point2D.Double ((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2) ;
    }

    private Point2D.Double midpoint(GNode p1, GNode p2) {
        return new Point2D.Double ((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2) ;
    }

    public void reset() {
    }
}
