package de.adEditor.routes;

import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.installed.KeycloakInstalled;
import org.keycloak.common.util.KeycloakUriBuilder;

import java.io.InputStream;

public class OfflineKeycloakInstalled extends KeycloakInstalled {

    public OfflineKeycloakInstalled(InputStream config) {
        super(config);
    }

    protected String createAuthUrl(String redirectUri, String state, Pkce pkce) {

        KeycloakUriBuilder builder = getDeployment().getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, getDeployment().getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.OFFLINE_ACCESS);

        if (state != null) {
            builder.queryParam(OAuth2Constants.STATE, state);
        }

        if (getLocale() != null) {
            builder.queryParam(OAuth2Constants.UI_LOCALES_PARAM, getLocale().getLanguage());
        }

        if (pkce != null) {
            builder.queryParam(OAuth2Constants.CODE_CHALLENGE, pkce.getCodeChallenge());
            builder.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, "S256");
        }

        return builder.build().toString();
    }
}
