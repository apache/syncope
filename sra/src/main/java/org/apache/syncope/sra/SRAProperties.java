/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.sra;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeProperties;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apereo.cas.client.Protocol;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

@ConfigurationProperties(SRAProperties.PREFIX)
public class SRAProperties extends SyncopeProperties {

    private static final long serialVersionUID = 1250377680268118123L;

    public static final String PREFIX = "sra";

    public static final String AM_TYPE = "am-type";

    public static class Global implements Serializable {

        private static final long serialVersionUID = -2035267979830256742L;

        private URI error = URI.create("/error");

        private URI postLogout = URI.create("/logout");

        public URI getError() {
            return error;
        }

        public void setError(final URI error) {
            this.error = error;
        }

        public URI getPostLogout() {
            return postLogout;
        }

        public void setPostLogout(final URI postLogout) {
            this.postLogout = postLogout;
        }
    }

    public enum AMType {
        OIDC,
        OAUTH2,
        SAML2,
        CAS

    }

    public static class OIDC implements Serializable {

        private static final long serialVersionUID = 4428057402762583676L;

        private String configuration;

        private String clientId;

        private String clientSecret;

        private List<String> scopes = Arrays.asList(
                OidcScopes.OPENID,
                OidcScopes.ADDRESS,
                OidcScopes.EMAIL,
                OidcScopes.PHONE,
                OidcScopes.PROFILE);

        public String getConfiguration() {
            return configuration;
        }

        public void setConfiguration(final String configuration) {
            this.configuration = configuration;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(final String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(final String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(final List<String> scopes) {
            this.scopes = scopes;
        }
    }

    public static class OAUTH2 implements Serializable {

        private static final long serialVersionUID = -5051777207539192764L;

        private String tokenUri;

        private String authorizationUri;

        private String userInfoUri;

        private String userNameAttributeName;

        private String jwkSetUri;

        private String issuer;

        private String clientId;

        private String clientSecret;

        private List<String> scopes = new ArrayList<>();

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(final String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(final String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(final String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        public String getUserNameAttributeName() {
            return userNameAttributeName;
        }

        public void setUserNameAttributeName(final String userNameAttributeName) {
            this.userNameAttributeName = userNameAttributeName;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(final String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(final String issuer) {
            this.issuer = issuer;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(final String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(final String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(final List<String> scopes) {
            this.scopes = scopes;
        }
    }

    public static class SAML2 implements Serializable {

        private static final long serialVersionUID = 6819907914821190235L;

        private SAML2BindingType authnRequestBinding = SAML2BindingType.POST;

        private SAML2BindingType logoutRequestBinding = SAML2BindingType.POST;

        private SAML2BindingType logoutResponseBinding = SAML2BindingType.REDIRECT;

        private String entityId;

        private long maximumAuthenticationLifetime = 3600;

        private long acceptedSkew = 300;

        private String spMetadataFilePath;

        private String idpMetadata;

        private String keystore;

        private String keystoreType;

        private String keystoreStorepass;

        private String keystoreKeypass;

        public SAML2BindingType getAuthnRequestBinding() {
            return authnRequestBinding;
        }

        public void setAuthnRequestBinding(final SAML2BindingType authnRequestBinding) {
            this.authnRequestBinding = authnRequestBinding;
        }

        public SAML2BindingType getLogoutRequestBinding() {
            return logoutRequestBinding;
        }

        public void setLogoutRequestBinding(final SAML2BindingType logoutRequestBinding) {
            this.logoutRequestBinding = logoutRequestBinding;
        }

        public SAML2BindingType getLogoutResponseBinding() {
            return logoutResponseBinding;
        }

        public void setLogoutResponseBinding(final SAML2BindingType logoutResponseBinding) {
            this.logoutResponseBinding = logoutResponseBinding;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(final String entityId) {
            this.entityId = entityId;
        }

        public long getMaximumAuthenticationLifetime() {
            return maximumAuthenticationLifetime;
        }

        public void setMaximumAuthenticationLifetime(final long maximumAuthenticationLifetime) {
            this.maximumAuthenticationLifetime = maximumAuthenticationLifetime;
        }

        public long getAcceptedSkew() {
            return acceptedSkew;
        }

        public void setAcceptedSkew(final int acceptedSkew) {
            this.acceptedSkew = acceptedSkew;
        }

        public String getSpMetadataFilePath() {
            return spMetadataFilePath;
        }

        public void setSpMetadataFilePath(final String spMetadataFilePath) {
            this.spMetadataFilePath = spMetadataFilePath;
        }

        public String getIdpMetadata() {
            return idpMetadata;
        }

        public void setIdpMetadata(final String idpMetadata) {
            this.idpMetadata = idpMetadata;
        }

        public String getKeystore() {
            return keystore;
        }

        public void setKeystore(final String keystore) {
            this.keystore = keystore;
        }

        public String getKeystoreType() {
            return keystoreType;
        }

        public void setKeystoreType(final String keystoreType) {
            this.keystoreType = keystoreType;
        }

        public String getKeystoreStorePass() {
            return keystoreStorepass;
        }

        public void setKeystoreStorePass(final String keystoreStorePass) {
            this.keystoreStorepass = keystoreStorePass;
        }

        public String getKeystoreKeypass() {
            return keystoreKeypass;
        }

        public void setKeystoreKeypass(final String keystoreKeyPass) {
            this.keystoreKeypass = keystoreKeyPass;
        }
    }

    public static class CAS implements Serializable {

        private static final long serialVersionUID = -5413988649759834473L;

        private String serverPrefix;

        private Protocol protocol = Protocol.CAS3;

        public String getServerPrefix() {
            return serverPrefix;
        }

        public void setServerPrefix(final String serverPrefix) {
            this.serverPrefix = serverPrefix;
        }

        public Protocol getProtocol() {
            return protocol;
        }

        public void setProtocol(final Protocol protocol) {
            this.protocol = protocol;
        }
    }

    private final Global global = new Global();

    private AMType amType = AMType.OIDC;

    private final OIDC oidc = new OIDC();

    private final OAUTH2 oauth2 = new OAUTH2();

    private final SAML2 saml2 = new SAML2();

    private final CAS cas = new CAS();

    public Global getGlobal() {
        return global;
    }

    public AMType getAmType() {
        return amType;
    }

    public void setAmType(final AMType amType) {
        this.amType = amType;
    }

    public OIDC getOidc() {
        return oidc;
    }

    public OAUTH2 getOauth2() {
        return oauth2;
    }

    public SAML2 getSaml2() {
        return saml2;
    }

    public CAS getCas() {
        return cas;
    }
}
