package de.adEditor.gui.editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class EditorListener implements ActionListener {

    private static Logger LOG = LoggerFactory.getLogger(EditorListener.class);
    public EditorFrame editor;

    public EditorListener (EditorFrame editor) {
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LOG.info("ActionCommand: {}", e.getActionCommand());
        if (e.getActionCommand().equals("Save")) {
            editor.saveMap();
        }
        if (e.getActionCommand().equals(EditorFrame.MOVE_NODES)) {
            this.editor.editorState = EditorFrame.EDITORSTATE_MOVING;
        }
        if (e.getActionCommand().equals(EditorFrame.REMOVE_NODES)) {
            this.editor.editorState = EditorFrame.EDITORSTATE_DELETING;
        }
        if (e.getActionCommand().equals(EditorFrame.REMOVE_DESTINATIONS)) {
            this.editor.editorState = EditorFrame.EDITORSTATE_DELETING_DESTINATION;
        }
        if (e.getActionCommand().equals(EditorFrame.CONNECT_NODES)) {
            this.editor.editorState = EditorFrame.EDITORSTATE_CONNECTING;
        }
        if (e.getActionCommand().equals(EditorFrame.CREATE_NODES)) {
            this.editor.editorState = EditorFrame.EDITORSTATE_CREATING;
        }
        if (e.getActionCommand().equals(EditorFrame.CREATE_DESTINATIONS)) {
            this.editor.editorState = EditorFrame.EDITORSTATE_CREATING_DESTINATION;
        }
        editor.updateButtons();
        if (e.getActionCommand().equals("Load")) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select AutoDrive Config");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("AutoDrive config", "xml");
            fc.addChoosableFileFilter(filter);
            int returnVal = fc.showOpenDialog(editor);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fileName = fc.getSelectedFile();
                editor.loadConfigFile(fileName);
                editor.pack();
                editor.repaint();
                editor.getMapPanel().repaint();

            }
        }
        if (e.getActionCommand().equals("Load Image")) {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(editor);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    editor.getMapPanel().setBackgroundMapImage(new BackgroundMapImage(ImageIO.read(fc.getSelectedFile())));
                    editor.getMapPanel().setPreferredSize(new Dimension(1024, 768));
                    editor.getMapPanel().setMinimumSize(new Dimension(1024, 768));
                    editor.getMapPanel().revalidate();
                    editor.pack();
                } catch (IOException e1) {
                    LOG.error(e1.getMessage(), e1);
                }
            }
        }
        if (e.getActionCommand().equals("OneTimesMap")) {
            editor.updateMapZoomFactor(1);
        }
        if (e.getActionCommand().equals("FourTimesMap")) {
            editor.updateMapZoomFactor(2);
        }
        if (e.getActionCommand().equals("SixteenTimesMap")) {
            editor.updateMapZoomFactor(4);
        }
    }
}
