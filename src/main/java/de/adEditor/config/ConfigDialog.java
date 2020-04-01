package de.adEditor.config;

import de.adEditor.helper.IconHelper;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ConfigDialog extends JDialog {

    private JTextField ls19Directory;

    public enum DIALOG_STATE {OK, CANCELED}
    private DIALOG_STATE state;

    public ConfigDialog(JFrame frame, boolean b) {
        super(frame, b);

        setTitle("AutoDrive Editor Configuration");
        setSize(400, 200);
        setLocationRelativeTo(frame);

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 20, 0, 20);

        JLabel directoryLabel = new JLabel("Your LS19 game directory:");
        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 0;
        c.anchor = GridBagConstraints.SOUTHWEST;
        add(directoryLabel, c);

        JPanel choosePanel = new JPanel();
        choosePanel.setLayout(new BoxLayout(choosePanel, BoxLayout.X_AXIS));
        ls19Directory = new JTextField(15);
        ls19Directory.setText(AdConfiguration.getInstance().getProperties().getProperty(AdConfiguration.LS19_GAME_DIRECTORY));
        JButton fileChooserButton = new JButton(new ImageIcon(IconHelper.getImageUrl("folder.png")));
        fileChooserButton.addActionListener(actionEvent -> {
            JFileChooser fc = new JFileChooser();

            fc.setDialogTitle("Select LS19 Directory");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            int returnVal = fc.showOpenDialog(frame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fileName = fc.getSelectedFile();
                ls19Directory.setText(fileName.getAbsolutePath());
            }
        });
        choosePanel.add(ls19Directory);
        choosePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        choosePanel.add(fileChooserButton);

        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_START;
        add(choosePanel, c);

        JButton okButton = new JButton("OK");
        okButton.setIcon(new ImageIcon(IconHelper.getImageUrl("accept.png")));
        okButton.addActionListener(e->{
            AdConfiguration.getInstance().getProperties().setProperty(AdConfiguration.LS19_GAME_DIRECTORY, ls19Directory.getText());
            state = DIALOG_STATE.OK;
            dispose();
        });
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.CENTER;
        add(okButton, c);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setIcon(new ImageIcon(IconHelper.getImageUrl("cancel.png")));
        cancelButton.addActionListener(e->{
            state = DIALOG_STATE.CANCELED;
            dispose();
        });
        c.gridx = 1;
        c.gridwidth = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.CENTER;
        add(cancelButton, c);
    }

    public DIALOG_STATE getState() {
        return state;
    }

}
