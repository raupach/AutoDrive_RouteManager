package de.adEditor;

import com.formdev.flatlaf.FlatLightLaf;
import de.adEditor.gui.editor.EditorFrame;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;

public class AutoDriveEditor extends JFrame {

    private static Logger LOG = LoggerFactory.getLogger(AutoDriveEditor.class);


    public static void main(String[] args) {
        LOG.info("AutoDrive start.............................................................................................");
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }

        SwingUtilities.invokeLater(() -> {
            GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
            new EditorFrame().setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            if (cacheManager.getStatus().equals(Status.AVAILABLE)) {
                cacheManager.close();
            }
        }));
    }

}
