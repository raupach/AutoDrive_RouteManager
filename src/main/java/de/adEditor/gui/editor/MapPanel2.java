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
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MapPanel2 extends JPanel {

    private static final String ESCAPE_KEY = "ESCAPE_KEY";
    private static final String BACKSPACE_KEY = "BACKSPACE_KEY";
    private static final String DELETE_KEY = "DELETE_KEY";
    private static Logger LOG = LoggerFactory.getLogger(MapPanel2.class);

    private static final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor crosshairCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);


    private enum MapPanelMode {NONE, DRAGGING_NODE, DRAWING};
    private EditorFrame editor;
    private RoadMap roadMap = new RoadMap();
    private BackgroundMapImage backgroundMapImage;
    private boolean mapMove = false;
    private int lastMousePosX=0, lastMousePosY = 0;
    private Point mousePos;
    private GNode tempLastNode;
    private MapPanelMode mapPanelMode = MapPanelMode.NONE;
    private AffineTransform tx = new AffineTransform();
    private final static Polygon arrowHead;
    private GNode touchedNode;
    private GNode selectedNode;

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
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mouseButton1Released(e.getX(), e.getY());
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    mouseButton3Released(e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
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
                } else if (editor.getEditorMode().equals(EditorMode.MOVE)) {
                    setCursor(Cursor.getDefaultCursor());
                    boolean shouldRepaint = false;
                    if (touchedNode !=null) {
                        touchedNode = null;
                        shouldRepaint = true;
                    }
                    Rectangle r = new Rectangle(e.getX()-6, e.getY()-6, 12,12);
                    roadMap.getGraph().vertexSet().stream()
                            .filter(n -> r.contains(worldVertexToScreenPos(n)) && !n.isSelected())
                            .findFirst()
                            .ifPresent(s -> {
                                touchedNode = s;
                                repaint();
                            });

                    if (shouldRepaint) {
                        repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseDraggedButton1(e.getX(), e.getY());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    mouseDraggedButton3(e.getX(), e.getY());
                }
            }
        });

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
                mouseWheelMovedd(mouseWheelEvent.getWheelRotation());
            }
        });


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


    private void mouseDraggedButton1(int x, int y) {
        if (editor.getEditorMode().equals(EditorMode.MOVE) && selectedNode!=null && mapPanelMode.equals(MapPanelMode.DRAGGING_NODE)) {
            selectedNode.setPos(screenPosToWorldPos(new Point(x, y)));
            repaint();
        }

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
            GNode newVertex = screenPosToWorldVertex (x, y);
            Graph<GNode, GEdge> graph = roadMap.getGraph();
            graph.addVertex(newVertex);
            graph.addVertex(newVertex);
            if (tempLastNode !=null) {
                graph.addEdge(tempLastNode, newVertex, new GEdge(true));
            }
            tempLastNode = newVertex;
            repaint();
        } else if (editor.getEditorMode().equals(EditorMode.MOVE)) {
            mapPanelMode = MapPanelMode.NONE;
            setCursor(Cursor.getDefaultCursor());
        }

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


    private void mouseDraggedButton3(int x, int y) {
        if (mapPanelMode.equals(MapPanelMode.DRAWING)) {
            mousePos = new Point(x, y);
        }
        if (mapMove) {
            backgroundMapImage.move( lastMousePosX-x, lastMousePosY-y, this.getWidth(), this.getHeight());
            lastMousePosX = x;
            lastMousePosY = y;
            repaint();
        }

    }

    private void mouseButton3Pressed(int x, int y) {
        LOG.debug("mouseButton3Pressed");
        mapMove = true;
        lastMousePosX = x;
        lastMousePosY = y;
    }

    private void mouseButton1Pressed(int x, int y) {
        if (editor.getEditorMode().equals(EditorMode.MOVE)) {
            touchedNode = null;
            Rectangle r = new Rectangle(x - 6, y - 6, 12, 12);
            if (selectedNode != null){
                selectedNode.setSelected(false);
                selectedNode = null;
            }

            roadMap.getGraph().vertexSet().stream().filter(n -> r.contains(worldVertexToScreenPos(n))).findFirst().ifPresent(s -> selectedNode = s);

            if (selectedNode != null) {
                selectedNode.setSelected( true);
            }
            mapPanelMode = MapPanelMode.DRAGGING_NODE;

            setCursor(handCursor);
            repaint();
        }
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
//                g.setColor(Color.GREEN);
                ((Graphics2D) g).setStroke(stroke_2);
                drawEdge(g, sourceNode, targetNode);
            });
            g.setColor(Color.yellow);
            ((Graphics2D) g).setStroke(stroke_1);
            drawVertex(g, p);
        }


//        for (GNode p : tempNewLine.vertexSet()) {
//            tempNewLine.outgoingEdgesOf(p).forEach(outEdge -> {
//                GNode sourceNode = tempNewLine.getEdgeSource(outEdge);
//                GNode targetNode = tempNewLine.getEdgeTarget(outEdge);
//                g.setColor(Color.RED);
//                ((Graphics2D) g).setStroke(stroke_2);
//                drawEdge(g, sourceNode, targetNode);
//            });
//            g.setColor(Color.yellow);
//            ((Graphics2D) g).setStroke(stroke_1);
//            drawVertex(g, p);
//        }

        if (mapPanelMode.equals(MapPanelMode.DRAWING) && tempLastNode != null && mousePos != null) {
            ((Graphics2D) g).setStroke(stroke_2);
            g.setColor(Color.RED);
            Point p = worldVertexToScreenPos(tempLastNode);
            g.drawLine(p.x, p.y, mousePos.x, mousePos.y);
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
        if (editor.getEditorMode().equals(EditorMode.MOVE) && selectedNode != null) {
            Graph<GNode, GEdge> graph = roadMap.getGraph();
            List<GEdge> incomingEdges = new ArrayList<>();
            List<GEdge> outgoingEdges = new ArrayList<>();
            CollectionUtils.addAll(incomingEdges, graph.incomingEdgesOf(selectedNode));
            CollectionUtils.addAll( outgoingEdges ,graph.outgoingEdgesOf(selectedNode));
            if (outgoingEdges.isEmpty()) {
                graph.removeAllEdges(incomingEdges);
                graph.removeVertex(selectedNode);
            } else if (incomingEdges.isEmpty()){
                graph.removeAllEdges(outgoingEdges);
                graph.removeVertex(selectedNode);
            } else if (outgoingEdges.size() == 1 && incomingEdges.size() == 1) {
                GNode newTarget = graph.getEdgeTarget(outgoingEdges.get(0));
                GNode newSource = graph.getEdgeSource(incomingEdges.get(0));
                graph.removeAllEdges(incomingEdges);
                graph.removeAllEdges(outgoingEdges);
                graph.removeVertex(selectedNode);
                graph.addEdge(newSource, newTarget);
            }

            repaint();
        }
    }

    public void reset() {
    }
}
