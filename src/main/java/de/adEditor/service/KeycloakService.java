package de.adEditor.service;

import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.KeycloakInstalled;
import org.keycloak.common.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

@Service
public class KeycloakService {

    private static long minValidity = 30L;
    private KeycloakInstalled keycloak;
    private static Logger LOG = LoggerFactory.getLogger(KeycloakService.class);


    public KeycloakService() {
        InputStream config = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak.json");
        keycloak = new KeycloakInstalled(config);
    }

    public void login() {
        try {
            keycloak.loginDesktop();
        } catch (IOException | VerificationException | OAuthErrorException | URISyntaxException | ServerRequest.HttpFailure | InterruptedException e) {
            LOG.error(e.getMessage(), e);
        } catch (UnsupportedOperationException uoe) {
            try {
                keycloak.loginManual();
            } catch (IOException | ServerRequest.HttpFailure | VerificationException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void logout() {
        try {
            keycloak.logout();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public String getToken() {
//        try {
//            if (keycloak.getToken() == null) {
//                login();
//            }
//            return keycloak.getTokenString(minValidity, TimeUnit.SECONDS);
//        } catch (VerificationException | IOException | ServerRequest.HttpFailure e) {
//            LOG.error(e.getMessage(), e);
//        }
        return "";
    }

}

