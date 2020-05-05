package de.adEditor.helper;

import de.adEditor.AutoDriveEditor;
import de.adEditor.gui.editor.EditorFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class IconHelper {

    private static Logger LOG = LoggerFactory.getLogger(IconHelper.class);

    public static URL getImageUrl(String imageName) {
        String imgLocation = "/icons/" + imageName;
        return AutoDriveEditor.class.getResource(imgLocation);
    }

    public static Image loadImage(String path) {
        try {
            URL url = EditorFrame.class.getResource(path);
            return ImageIO.read(url);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }
}
