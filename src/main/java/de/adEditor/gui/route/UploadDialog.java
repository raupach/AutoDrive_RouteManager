package de.adEditor.gui.route;

import de.adEditor.ApplicationContextProvider;
import de.adEditor.mapper.RoutesRequestMapper;
import de.adEditor.gui.GlassPane;
import de.adEditor.dto.Route;
import de.adEditor.dto.RouteExport;
import de.adEditor.service.HttpClientService;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesRequestDto;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesStoreResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class UploadDialog extends JDialog {

    private static Logger LOG = LoggerFactory.getLogger(UploadDialog.class);
    private final static String[] routeType = {"FULL", "PARTIAL"};
    private Route route;
    private RouteExport routeExport;
    private String username;
    private GlassPane glassPane = new GlassPane();
    private Component oldGlassPane;
    private JLabel msg;
    private JButton uploadButton;
    private JTextArea descriptionTextArea;

    private HttpClientService httpClientService;
    private RoutesRequestMapper routesRequestMapper;

    public UploadDialog(JFrame frame, Route route, RouteExport routeExport, String username) {
        super(frame, true);
        this.route = route;
        this.username = username;
        this.routeExport = routeExport;

        LOG.info("UploadDialog.");
        httpClientService = ApplicationContextProvider.getContext().getBean(HttpClientService.class);
        routesRequestMapper = ApplicationContextProvider.getContext().getBean(RoutesRequestMapper.class);

        setTitle("AutoDrive Upload Route");
        setSize(700, 400);
        setLocationRelativeTo(frame);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        // 1
        JLabel l1 = new JLabel("Name: ");
        Font f = l1.getFont();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 20, 0, 0);
        add(l1, c);

        JLabel l2 = new JLabel(route.getName());
        l2.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 10, 0, 0);
        add(l2, c);


        // 2
        JLabel l21 = new JLabel("Map: ");
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 20, 0, 0);
        add(l21, c);

        JLabel l22 = new JLabel(route.getMap());
        l22.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 10, 0, 0);
        add(l22, c);


        // 3
        JLabel l31 = new JLabel("Revision: ");
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 20, 0, 0);
        add(l31, c);

        JLabel l32 = new JLabel(route.getRevision().toString());
        l32.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 10, 0, 0);
        add(l32, c);

        // 4
        JLabel l41 = new JLabel("Date: ");
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 20, 0, 0);
        add(l41, c);

        JLabel l42 = new JLabel(route.getDate().toString());
        l42.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 10, 0, 0);
        add(l42, c);

        // 5
        JLabel l51 = new JLabel("Typ: ");
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 20, 0, 0);
        add(l51, c);

        JComboBox<String> routeTypeComboBox = new JComboBox<>(routeType);
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 10, 0, 0);
        add(routeTypeComboBox, c);


        // 6
        JLabel l61 = new JLabel("Description:");
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(5, 20, 10, 0);
        add(l61, c);

        descriptionTextArea = new JTextArea("");
        JScrollPane scrollpane = new JScrollPane(descriptionTextArea);
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 20);
        add(scrollpane, c);


        c.weightx = 0;
        c.weighty = 0;

        msg = new JLabel("The route was successfully uploaded !");
        msg.setBackground(new Color(134, 255, 23));
        msg.setOpaque(true);
        msg.setVisible(false);
        c.gridx = 1;
        c.gridy = 6;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.WEST;
        c.insets = new Insets(5, 10, 10, 0);
        add(msg, c);

        uploadButton = new JButton("Upload");
        uploadButton.setPreferredSize(new Dimension(120, 20));
        uploadButton.addActionListener(e -> upload());
        c.gridx = 1;
        c.gridy = 7;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 10, 20, 20);
        add(uploadButton, c);

        JButton cancelButton = new JButton("Quit");
        cancelButton.setPreferredSize(new Dimension(120, 20));
        cancelButton.addActionListener(e -> quit());
        c.gridx = 2;
        c.gridy = 7;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 10, 20, 20);
        add(cancelButton, c);

        oldGlassPane = getGlassPane();
    }

    private void quit() {
        dispose();
    }

    private void uploadRouteResponse(RoutesStoreResponseDto dto) {
        msg.setVisible(true);
        uploadButton.setEnabled(false);
    }

    private void upload() {
        setGlassPane(glassPane);
        glassPane.setVisible(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        RoutesRequestDto requestDto = routesRequestMapper.toDto(routeExport, route.getName(), route.getMap(), route.getRevision(), route.getDate(), username, descriptionTextArea.getText());
        httpClientService.upload(requestDto, responseDto -> {
            responseDto.ifPresentOrElse(this::uploadRouteResponse, this::showErrorMessage);
            setCursor(Cursor.getDefaultCursor());
            glassPane.setVisible(false);
            setGlassPane(oldGlassPane);
        });
    }

    private void showErrorMessage() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "An error occurred during upload.", "Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}
