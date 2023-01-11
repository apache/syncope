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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.Attr;

public class DefaultAccessPolicyConf implements AccessPolicyConf {

    private static final long serialVersionUID = 1153200197344709778L;

    private int order;

    private boolean enabled = true;

    private boolean ssoEnabled = true;

    private boolean requireAllAttributes = true;

    private boolean caseInsensitive;

    private URI unauthorizedRedirectUrl;

    private final List<Attr> requiredAttrs = new ArrayList<>();

    private final List<Attr> rejectedAttrs = new ArrayList<>();

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

    @JacksonXmlElementWrapper(localName = "requiredAttrs")
    @JacksonXmlProperty(localName = "requiredAttr")
    public List<Attr> getRequiredAttrs() {
        return requiredAttrs;
    }

    @JacksonXmlElementWrapper(localName = "rejectedAttrs")
    @JacksonXmlProperty(localName = "rejectedAttr")
    public List<Attr> getRejectedAttrs() {
        return rejectedAttrs;
    }
}
