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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ImplementationType;

@XmlRootElement(name = "platformInfo")
@XmlType
public class PlatformInfo extends AbstractBaseBean {

    private static final long serialVersionUID = -7941853999417673827L;

    private String version;

    private String buildNumber;

    private boolean selfRegAllowed;

    private boolean pwdResetAllowed;

    private boolean pwdResetRequiringSecurityQuestions;

    private final Set<String> connIdLocations = new HashSet<>();

    private String propagationTaskExecutor;

    private String anyObjectWorkflowAdapter;

    private String userWorkflowAdapter;

    private String groupWorkflowAdapter;

    private String anyObjectProvisioningManager;

    private String userProvisioningManager;

    private String groupProvisioningManager;

    private String virAttrCache;

    private String passwordGenerator;

    private String anySearchDAO;

    private final List<String> anyTypes = new ArrayList<>();

    private final List<String> userClasses = new ArrayList<>();

    private final List<String> anyTypeClasses = new ArrayList<>();

    private final List<String> resources = new ArrayList<>();

    private final Set<String> entitlements = new HashSet<>();

    private final Set<JavaImplInfo> javaImplInfos = new HashSet<>();

    public String getVersion() {
        return version;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public boolean isSelfRegAllowed() {
        return selfRegAllowed;
    }

    public boolean isPwdResetAllowed() {
        return pwdResetAllowed;
    }

    public boolean isPwdResetRequiringSecurityQuestions() {
        return pwdResetRequiringSecurityQuestions;
    }

    @XmlElementWrapper(name = "connIdLocations")
    @XmlElement(name = "connIdLocation")
    @JsonProperty("connIdLocations")
    public Set<String> getConnIdLocations() {
        return connIdLocations;
    }

    public String getPropagationTaskExecutor() {
        return propagationTaskExecutor;
    }

    public String getAnyObjectWorkflowAdapter() {
        return anyObjectWorkflowAdapter;
    }

    public String getUserWorkflowAdapter() {
        return userWorkflowAdapter;
    }

    public String getGroupWorkflowAdapter() {
        return groupWorkflowAdapter;
    }

    public String getAnyObjectProvisioningManager() {
        return anyObjectProvisioningManager;
    }

    public String getUserProvisioningManager() {
        return userProvisioningManager;
    }

    public String getGroupProvisioningManager() {
        return groupProvisioningManager;
    }

    public String getVirAttrCache() {
        return virAttrCache;
    }

    public String getPasswordGenerator() {
        return passwordGenerator;
    }

    public void setPasswordGenerator(final String passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    public String getAnySearchDAO() {
        return anySearchDAO;
    }

    public void setAnySearchDAO(final String anySearchDAO) {
        this.anySearchDAO = anySearchDAO;
    }

    @XmlElementWrapper(name = "anyTypes")
    @XmlElement(name = "anyType")
    @JsonProperty("anyTypes")
    public List<String> getAnyTypes() {
        return anyTypes;
    }

    @XmlElementWrapper(name = "userClasses")
    @XmlElement(name = "userClass")
    @JsonProperty("userClasses")
    public List<String> getUserClasses() {
        return userClasses;
    }

    @XmlElementWrapper(name = "anyTypeClasses")
    @XmlElement(name = "anyTypeClass")
    @JsonProperty("anyTypeClasses")
    public List<String> getAnyTypeClasses() {
        return anyTypeClasses;
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public List<String> getResources() {
        return resources;
    }

    @XmlElementWrapper(name = "entitlements")
    @XmlElement(name = "entitlement")
    @JsonProperty("entitlements")
    public Set<String> getEntitlements() {
        return entitlements;
    }

    @JsonIgnore
    public Optional<JavaImplInfo> getJavaImplInfo(final ImplementationType type) {
        return javaImplInfos.stream().filter(javaImplInfo -> javaImplInfo.getType() == type).findFirst();
    }

    @XmlElementWrapper(name = "javaImplInfos")
    @XmlElement(name = "javaImplInfo")
    @JsonProperty("javaImplInfos")
    public Set<JavaImplInfo> getJavaImplInfos() {
        return javaImplInfos;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setBuildNumber(final String buildNumber) {
        this.buildNumber = buildNumber;
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

    public void setPropagationTaskExecutor(final String propagationTaskExecutor) {
        this.propagationTaskExecutor = propagationTaskExecutor;
    }

    public void setAnyObjectWorkflowAdapter(final String anyObjectWorkflowAdapter) {
        this.anyObjectWorkflowAdapter = anyObjectWorkflowAdapter;
    }

    public void setUserWorkflowAdapter(final String userWorkflowAdapter) {
        this.userWorkflowAdapter = userWorkflowAdapter;
    }

    public void setGroupWorkflowAdapter(final String groupWorkflowAdapter) {
        this.groupWorkflowAdapter = groupWorkflowAdapter;
    }

    public void setAnyObjectProvisioningManager(final String anyObjectProvisioningManager) {
        this.anyObjectProvisioningManager = anyObjectProvisioningManager;
    }

    public void setUserProvisioningManager(final String userProvisioningManager) {
        this.userProvisioningManager = userProvisioningManager;
    }

    public void setGroupProvisioningManager(final String groupProvisioningManager) {
        this.groupProvisioningManager = groupProvisioningManager;
    }

    public void setVirAttrCache(final String virAttrCache) {
        this.virAttrCache = virAttrCache;
    }
}
