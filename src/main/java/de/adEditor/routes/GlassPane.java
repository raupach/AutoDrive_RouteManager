package de.adEditor.routes;

import de.adEditor.helper.IconHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

public class GlassPane extends JComponent {


    public GlassPane() {
        setLayout(new BorderLayout());
        JLabel imageLabel = new JLabel(new ImageIcon(IconHelper.getImageUrl("831.gif")));
        add(imageLabel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
        });
    }

    public void paintComponent(Graphics g) {
        Rectangle rect = getBounds();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(new Color(0.9f, 0.9f, 0.9f, 0.6f));
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
    }
}
