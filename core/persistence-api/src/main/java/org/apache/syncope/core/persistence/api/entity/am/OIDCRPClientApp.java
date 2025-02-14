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
package org.apache.syncope.core.persistence.api.entity.am;

import java.util.Set;
import org.apache.syncope.common.lib.types.OIDCApplicationType;
import org.apache.syncope.common.lib.types.OIDCClientAuthenticationMethod;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionAlg;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;

public interface OIDCRPClientApp extends ClientApp {

    void setClientId(String id);

    String getClientId();

    void setClientSecret(String secret);

    String getClientSecret();

    Set<String> getRedirectUris();

    Set<OIDCGrantType> getSupportedGrantTypes();

    Set<OIDCResponseType> getSupportedResponseTypes();

    Set<String> getScopes();

    String getIdTokenIssuer();

    void setIdTokenIssuer(String idTokenIssuer);

    boolean isSignIdToken();

    void setSignIdToken(boolean signIdToken);

    OIDCTokenSigningAlg getIdTokenSigningAlg();

    void setIdTokenSigningAlg(OIDCTokenSigningAlg idTokenSigningAlg);

    boolean isEncryptIdToken();

    void setEncryptIdToken(boolean encryptIdToken);

    OIDCTokenEncryptionAlg getIdTokenEncryptionAlg();

    void setIdTokenEncryptionAlg(OIDCTokenEncryptionAlg idTokenEncryptionAlg);

    OIDCTokenEncryptionEncoding getIdTokenEncryptionEncoding();

    void setIdTokenEncryptionEncoding(OIDCTokenEncryptionEncoding idTokenEncryptionEncoding);

    OIDCTokenSigningAlg getUserInfoSigningAlg();

    void setUserInfoSigningAlg(OIDCTokenSigningAlg userInfoSigningAlg);

    OIDCTokenEncryptionAlg getUserInfoEncryptedResponseAlg();

    void setUserInfoEncryptedResponseAlg(OIDCTokenEncryptionAlg userInfoEncryptedResponseAlg);

    OIDCTokenEncryptionEncoding getUserInfoEncryptedResponseEncoding();

    void setUserInfoEncryptedResponseEncoding(OIDCTokenEncryptionEncoding encoding);

    boolean isJwtAccessToken();

    void setJwtAccessToken(boolean jwtAccessToken);

    boolean isBypassApprovalPrompt();

    void setBypassApprovalPrompt(boolean bypassApprovalPrompt);

    boolean isGenerateRefreshToken();

    void setGenerateRefreshToken(boolean generateRefreshToken);

    OIDCSubjectType getSubjectType();

    void setSubjectType(OIDCSubjectType subjectType);

    OIDCApplicationType getApplicationType();

    void setApplicationType(OIDCApplicationType applicationType);

    String getJwks();

    void setJwks(String jwks);

    String getJwksUri();

    void setJwksUri(String jwksUri);

    OIDCClientAuthenticationMethod getTokenEndpointAuthenticationMethod();

    void setTokenEndpointAuthenticationMethod(OIDCClientAuthenticationMethod tokenEndpointAuthenticationMethod);

    String getLogoutUri();

    void setLogoutUri(String logoutUri);

}
