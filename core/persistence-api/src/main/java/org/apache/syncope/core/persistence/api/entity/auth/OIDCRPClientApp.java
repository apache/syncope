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
package org.apache.syncope.core.persistence.api.entity.auth;

import java.util.Set;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;

public interface OIDCRPClientApp extends ClientApp {

    void setClientId(String id);

    String getClientId();

    void setClientSecret(String secret);

    String getClientSecret();

    Set<String> getRedirectUris();

    Set<OIDCGrantType> getSupportedGrantTypes();

    Set<OIDCResponseType> getSupportedResponseTypes();

    boolean isSignIdToken();

    void setSignIdToken(boolean signIdToken);

    boolean isJwtAccessToken();

    void setJwtAccessToken(boolean jwtAccessToken);

    OIDCSubjectType getSubjectType();

    void setSubjectType(OIDCSubjectType subjectType);

    String getLogoutUri();

    void setLogoutUri(String logoutUri);
}
