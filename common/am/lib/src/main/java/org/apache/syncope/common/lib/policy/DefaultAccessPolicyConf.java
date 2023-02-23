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
package org.apache.syncope.common.lib.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class DefaultAccessPolicyConf implements AccessPolicyConf {

    private static final long serialVersionUID = 1153200197344709778L;

    private int order;

    private boolean enabled = true;

    private boolean ssoEnabled = true;

    private boolean requireAllAttributes = true;

    private boolean caseInsensitive;

    private URI unauthorizedRedirectUrl;

    @Schema(description =
            "Insert comma-separated values in the right input field if you like to specify more than one value")
    private final Map<String, String> requiredAttrs = new HashMap<>();

    @Schema(description =
            "Insert comma-separated values in the right input field if you like to specify more than one value")
    private final Map<String, String> rejectedAttrs = new HashMap<>();

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSsoEnabled() {
        return this.ssoEnabled;
    }

    public void setSsoEnabled(final boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    public boolean isRequireAllAttributes() {
        return requireAllAttributes;
    }

    public void setRequireAllAttributes(final boolean requireAllAttributes) {
        this.requireAllAttributes = requireAllAttributes;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(final boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    public URI getUnauthorizedRedirectUrl() {
        return unauthorizedRedirectUrl;
    }

    public void setUnauthorizedRedirectUrl(final URI unauthorizedRedirectUrl) {
        this.unauthorizedRedirectUrl = unauthorizedRedirectUrl;
    }

    public Map<String, String> getRequiredAttrs() {
        return requiredAttrs;
    }

    public Map<String, String> getRejectedAttrs() {
        return rejectedAttrs;
    }
}
