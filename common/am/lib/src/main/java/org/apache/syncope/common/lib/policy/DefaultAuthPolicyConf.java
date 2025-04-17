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
import java.util.ArrayList;
import java.util.List;

public class DefaultAuthPolicyConf implements AuthPolicyConf {

    private static final long serialVersionUID = -2969836600059025380L;

    private boolean tryAll;

    private boolean bypassEnabled;

    private boolean forceMfaExecution = true;
    
    private String bypassPrincipalAttributeName;

    private String bypassPrincipalAttributeValue;

    private final List<String> authModules = new ArrayList<>();

    public boolean isTryAll() {
        return tryAll;
    }

    public void setTryAll(final boolean tryAll) {
        this.tryAll = tryAll;
    }

    public boolean isBypassEnabled() {
        return bypassEnabled;
    }

    public void setBypassEnabled(final boolean bypassEnabled) {
        this.bypassEnabled = bypassEnabled;
    }

    public boolean isForceMfaExecution() {
        return forceMfaExecution;
    }

    public void setForceMfaExecution(final boolean forceMfaExecution) {
        this.forceMfaExecution = forceMfaExecution;
    }

    public String getBypassPrincipalAttributeName() {
        return bypassPrincipalAttributeName;
    }

    public void setBypassPrincipalAttributeName(final String bypassPrincipalAttributeName) {
        this.bypassPrincipalAttributeName = bypassPrincipalAttributeName;
    }

    public String getBypassPrincipalAttributeValue() {
        return bypassPrincipalAttributeValue;
    }

    public void setBypassPrincipalAttributeValue(final String bypassPrincipalAttributeValue) {
        this.bypassPrincipalAttributeValue = bypassPrincipalAttributeValue;
    }

    @JacksonXmlElementWrapper(localName = "authModules")
    @JacksonXmlProperty(localName = "authModule")
    public List<String> getAuthModules() {
        return authModules;
    }
}
