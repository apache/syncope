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
package org.apache.syncope.common.lib.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.BaseBean;

public class PlatformInfo implements BaseBean {

    private static final long serialVersionUID = -7941853999417673827L;

    private boolean selfRegAllowed;

    private boolean pwdResetAllowed;

    private boolean pwdResetRequiringSecurityQuestions;

    private final Set<String> connIdLocations = new HashSet<>();

    private final List<String> anyTypes = new ArrayList<>();

    private final List<String> userClasses = new ArrayList<>();

    private final List<String> anyTypeClasses = new ArrayList<>();

    private final List<String> resources = new ArrayList<>();

    private final Set<String> entitlements = new HashSet<>();

    private final Set<String> implementationTypes = new HashSet<>();

    private final Set<JavaImplInfo> javaImplInfos = new HashSet<>();

    public boolean isSelfRegAllowed() {
        return selfRegAllowed;
    }

    public boolean isPwdResetAllowed() {
        return pwdResetAllowed;
    }

    public boolean isPwdResetRequiringSecurityQuestions() {
        return pwdResetRequiringSecurityQuestions;
    }

    @JacksonXmlElementWrapper(localName = "connIdLocations")
    @JacksonXmlProperty(localName = "connIdLocation")
    public Set<String> getConnIdLocations() {
        return connIdLocations;
    }

    @JacksonXmlElementWrapper(localName = "anyTypes")
    @JacksonXmlProperty(localName = "anyType")
    public List<String> getAnyTypes() {
        return anyTypes;
    }

    @JacksonXmlElementWrapper(localName = "userClasses")
    @JacksonXmlProperty(localName = "userClass")
    public List<String> getUserClasses() {
        return userClasses;
    }

    @JacksonXmlElementWrapper(localName = "anyTypeClasses")
    @JacksonXmlProperty(localName = "anyTypeClass")
    public List<String> getAnyTypeClasses() {
        return anyTypeClasses;
    }

    @JacksonXmlElementWrapper(localName = "resources")
    @JacksonXmlProperty(localName = "resource")
    public List<String> getResources() {
        return resources;
    }

    @JacksonXmlElementWrapper(localName = "entitlements")
    @JacksonXmlProperty(localName = "entitlement")
    public Set<String> getEntitlements() {
        return entitlements;
    }

    @JacksonXmlElementWrapper(localName = "implementationTypes")
    @JacksonXmlProperty(localName = "implementationType")
    public Set<String> getImplementationTypes() {
        return implementationTypes;
    }

    @JsonIgnore
    public Optional<JavaImplInfo> getJavaImplInfo(final String type) {
        return javaImplInfos.stream().filter(javaImplInfo -> javaImplInfo.getType().equals(type)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "javaImplInfos")
    @JacksonXmlProperty(localName = "javaImplInfo")
    public Set<JavaImplInfo> getJavaImplInfos() {
        return javaImplInfos;
    }

    public void setSelfRegAllowed(final boolean selfRegAllowed) {
        this.selfRegAllowed = selfRegAllowed;
    }

    public void setPwdResetAllowed(final boolean pwdResetAllowed) {
        this.pwdResetAllowed = pwdResetAllowed;
    }

    public void setPwdResetRequiringSecurityQuestions(final boolean pwdResetRequiringSecurityQuestions) {
        this.pwdResetRequiringSecurityQuestions = pwdResetRequiringSecurityQuestions;
    }
}
