package de.adEditor.helper;

import de.adEditor.AutoDriveEditor;

import java.net.URL;

public class IconHelper {
    public static URL getImageUrl(String imageName) {
        String imgLocation = "/icons/" + imageName;
        return AutoDriveEditor.class.getResource(imgLocation);
    }

}
