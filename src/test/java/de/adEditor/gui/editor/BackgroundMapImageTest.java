package de.adEditor.gui.editor;

import org.junit.Test;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;


public class BackgroundMapImageTest {

    @Test
    public void translate() {
        BackgroundMapImage backgroundMapImage = new BackgroundMapImage(new BufferedImage(1, 1, 1));
        Point2D p = backgroundMapImage.translate(new Point2D.Double(0, 0));
        assertEquals(-1024, p.getX(), 0);
        assertEquals(-1024, p.getY(), 0);
        p = backgroundMapImage.translate(new Point2D.Double(1024, 1024));
        assertEquals(0, p.getX(), 0);
        assertEquals(0, p.getY(), 0);
        p = backgroundMapImage.translate(new Point2D.Double(2048, 2048));
        assertEquals(1024, p.getX(), 0);
        assertEquals(1024, p.getY(), 0);
    }
}