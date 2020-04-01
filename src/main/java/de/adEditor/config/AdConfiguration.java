package de.adEditor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class AdConfiguration {

    private static Logger LOG = LoggerFactory.getLogger(AdConfiguration.class);

    public final static String CONFIG_FILE_NAME = "adEditor.config";
    public final static String LS19_GAME_DIRECTORY = "LS19_GAME_DIRECTORY";
    public final static String TOKEN = "TOKEN";

    private static AdConfiguration instance = null;
    private Properties properties = new Properties();

    public static AdConfiguration getInstance() {
        if (instance == null) {
            instance = new AdConfiguration();
        }
        return instance;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void readConfigFile() {
        try {
            File configFile = new File(AdConfiguration.CONFIG_FILE_NAME);
            FileReader reader = new FileReader(configFile);
            properties = new Properties();
            properties.load(reader);
            reader.close();
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public void writeConfigFile() {
        try {
            File configFile = new File(AdConfiguration.CONFIG_FILE_NAME);
            OutputStream output = new FileOutputStream(configFile);
            properties.store(output, null);
            output.close();
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
