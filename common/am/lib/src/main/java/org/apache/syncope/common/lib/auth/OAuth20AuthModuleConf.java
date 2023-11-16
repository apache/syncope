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
package org.apache.syncope.common.lib.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.syncope.common.lib.to.AuthModuleTO;

public class OAuth20AuthModuleConf extends AbstractOAuth20AuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = 299820485764241682L;

    protected String authUrl;

    protected String profileUrl;

    protected Map<String, String> profileAttrs = new LinkedHashMap<>();

    protected boolean withState;

    protected String profileVerb = "POST";

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(final String authUrl) {
        this.authUrl = authUrl;
    }

    public Map<String, String> getProfileAttrs() {
        return profileAttrs;
    }

    public void setProfileAttrs(final Map<String, String> profileAttrs) {
        this.profileAttrs = profileAttrs;
    }

    public boolean isWithState() {
        return withState;
    }

    public void setWithState(final boolean withState) {
        this.withState = withState;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(final String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getProfileVerb() {
        return profileVerb;
    }

    public void setProfileVerb(final String profileVerb) {
        this.profileVerb = profileVerb;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
