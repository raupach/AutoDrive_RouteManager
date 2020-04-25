package de.adEditor.gui.editor;

import de.adEditor.gui.graph.GNode;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class BackgroundMapImage {

    private static Logger LOG = LoggerFactory.getLogger(BackgroundMapImage.class);

    private BufferedImage originalImage;
    private BufferedImage scaledImage;
    private Rectangle rectangle;
    private int currentZoomLevel = -1;
    private static final double[] scale = {1, 1.25, 1.5, 1.75, 2, 2.25, 2.5, 2.75, 3, 3.25, 3.5, 3.75, 4, 4.25, 4.5, 4.75, 5, 5.25, 5.5, 5.75,};
    private AffineTransform tx = new AffineTransform();

    public BackgroundMapImage(BufferedImage image) {
        this.originalImage = image;
        this.rectangle = new Rectangle(0, 0, originalImage.getWidth(), originalImage.getHeight());
        zoom(0);
    }


    public void draw(Graphics2D g) {
        BufferedImage cutoutImage = scaledImage.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        g.drawImage(cutoutImage, 0, 0, rectangle.width, rectangle.height, null);
    }

    public void zoom(int level) {
        long start = System.currentTimeMillis();
        int newZoomLevel;
        if (currentZoomLevel + level >= 20) {
            newZoomLevel = 19;
        } else if (currentZoomLevel + level < 0) {
            newZoomLevel = 0;
        } else {
            newZoomLevel = currentZoomLevel + level;
        }

        if (currentZoomLevel!=newZoomLevel) {
            currentZoomLevel = newZoomLevel;
            double scaleFactor = getScaleFactor();
            double w = originalImage.getWidth() * scaleFactor;
            double h = originalImage.getHeight() * scaleFactor;
            scaledImage = Scalr.resize(originalImage, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, (int) w, (int) h);
        }
        LOG.info("zoom currentZoomLevel: {} in {}ms", currentZoomLevel, System.currentTimeMillis() - start);
    }

    public void move(int x, int y, int dx, int dy) {

        rectangle.x += x;
        rectangle.y += y;
        rectangle.width = dx;
        rectangle.height = dy;

        if (rectangle.x < 0) {
            rectangle.x = 0;
        }

        if (rectangle.y < 0) {
            rectangle.y = 0;
        }

        if (rectangle.x + rectangle.width > scaledImage.getWidth()) {
            rectangle.x = scaledImage.getWidth() - rectangle.width;
        }

        if (rectangle.y + rectangle.height > scaledImage.getHeight()) {
            rectangle.y = scaledImage.getHeight() - rectangle.height;
        }

        if (rectangle.width > scaledImage.getWidth()) {
            rectangle.width = scaledImage.getWidth();
        }

        if (rectangle.height > scaledImage.getHeight()) {
            rectangle.height = scaledImage.getHeight();
        }
    }

    public Rectangle getScaledRectangle() {
        return new Rectangle((int)(rectangle.x* getScaleFactor()), (int)(rectangle.y* getScaleFactor()), (int)(rectangle.width* getScaleFactor()), (int)(rectangle.height* getScaleFactor()));
    }

    public double getScaleFactor() {
        return scale[currentZoomLevel];
    }


    public Rectangle getRectangle() {
        return rectangle;
    }

    public GNode screenPosToWorldVertex(int x, int y) {
        Point2D worldPos = screenPosToWorldPos (new Point (x, y));
        return new GNode (worldPos);
    }

    public Point2D screenPosToWorldPos(Point point) {
        Rectangle viewPort = getRectangle();
        double scaleFactor = getScaleFactor();
        double worldPosX = (point.x  + viewPort.x) / scaleFactor;
        double worldPosY = (point.y  + viewPort.y) / scaleFactor;
        return new Point2D.Double(worldPosX-1024, worldPosY-1024);
    }


    public Point worldVertexToScreenPos(GNode gNode) {
        return worldPosToScreenPos(gNode.getPoint2D());
    }

    public Point worldPosToScreenPos(Point2D p) {
        Rectangle viewPort = getRectangle();
        double scaleFactor = getScaleFactor();
        double screenPosX = ((p.getX()+1024)*scaleFactor) - viewPort.x;
        double screenPosY = ((p.getY()+1024)*scaleFactor) - viewPort.y;
        return new Point((int) screenPosX, (int) screenPosY);
//        Point2D result = t(p);
//        return new Point((int)result.getX(), (int)result.getY());
    }

    protected Point2D t(Point2D p) {
        Rectangle viewPort = getRectangle();
        tx.setToTranslation(-viewPort.x, -viewPort.y);
        AffineTransform transform = new AffineTransform();
        transform.concatenate(AffineTransform.getScaleInstance(getScaleFactor(), getScaleFactor()));
        transform.concatenate(tx);
        return transform.transform(p, null);
    }

    protected Point2D translate(Point2D p) {
        AffineTransform translate = AffineTransform.getTranslateInstance(-1024, -1024);
        return translate.transform(p, null);
    }

}