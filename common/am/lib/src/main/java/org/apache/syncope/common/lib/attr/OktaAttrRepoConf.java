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
package org.apache.syncope.common.lib.attr;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.AbstractOktaConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;

public class OktaAttrRepoConf extends AbstractOktaConf implements AttrRepoConf {

    private static final long serialVersionUID = 1019473980380211566L;

    /**
     * Username attribute to fetch attributes by.
     */
    private String usernameAttribute = "username";

    /**
     * Okta allows you to interact with Okta APIs using scoped OAuth 2.0 access tokens. Each access token
     * enables the bearer to perform specific actions on specific Okta endpoints, with that
     * ability controlled by which scopes the access token contains. Scopes are only used
     * when using client id and private-key.
     */
    private final List<String> scopes = Stream.of("okta.users.read", "okta.apps.read").collect(Collectors.toList());

    /**
     * Okta API token.
     */
    private String apiToken;

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(final String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(final String apiToken) {
        this.apiToken = apiToken;
    }

    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public Map<String, Object> map(final AttrRepoTO attrRepo, final Mapper mapper) {
        return mapper.map(attrRepo, this);
    }
}
