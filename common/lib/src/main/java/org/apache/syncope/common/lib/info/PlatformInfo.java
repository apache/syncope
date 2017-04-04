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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "platformInfo")
@XmlType
public class PlatformInfo extends AbstractBaseBean {

    private static final long serialVersionUID = -7941853999417673827L;

    private String version;

    private boolean selfRegAllowed;

    private boolean pwdResetAllowed;

    private boolean pwdResetRequiringSecurityQuestions;

    private final Set<String> connIdLocations = new HashSet<>();

    private String anyObjectWorkflowAdapter;

    private boolean anyObjectWorkflowAdapterSupportEdit;

    private String userWorkflowAdapter;

    private boolean userWorkflowAdapterSupportEdit;

    private String groupWorkflowAdapter;

    private boolean groupWorkflowAdapterSupportEdit;

    private String anyObjectProvisioningManager;

    private String userProvisioningManager;

    private String groupProvisioningManager;

    private String virAttrCache;

    private String passwordGenerator;

    private final Set<String> entitlements = new HashSet<>();

    private final Set<String> reportletConfs = new HashSet<>();

    private final Set<String> accountRules = new HashSet<>();

    private final Set<String> passwordRules = new HashSet<>();

    private final Set<String> mappingItemTransformers = new HashSet<>();

    private final Set<String> taskJobs = new HashSet<>();

    private final Set<String> reconciliationFilterBuilders = new HashSet<>();

    private final Set<String> logicActions = new HashSet<>();

    private final Set<String> propagationActions = new HashSet<>();

    private final Set<String> pullActions = new HashSet<>();

    private final Set<String> pushActions = new HashSet<>();

    private final Set<String> pullCorrelationRules = new HashSet<>();

    private final Set<String> pushCorrelationRules = new HashSet<>();

    private final Set<String> validators = new HashSet<>();

    private final Set<String> notificationRecipientsProviders = new HashSet<>();

    public String getVersion() {
        return version;
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

    public String getAnyObjectWorkflowAdapter() {
        return anyObjectWorkflowAdapter;
    }

    public String getUserWorkflowAdapter() {
        return userWorkflowAdapter;
    }

    public boolean isUserWorkflowAdapterSupportEdit() {
        return userWorkflowAdapterSupportEdit;
    }

    public String getGroupWorkflowAdapter() {
        return groupWorkflowAdapter;
    }

    public boolean isGroupWorkflowAdapterSupportEdit() {
        return groupWorkflowAdapterSupportEdit;
    }

    public String getAnyObjectProvisioningManager() {
        return anyObjectProvisioningManager;
    }

    public boolean isAnyObjectWorkflowAdapterSupportEdit() {
        return anyObjectWorkflowAdapterSupportEdit;
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

    @XmlElementWrapper(name = "entitlements")
    @XmlElement(name = "entitlement")
    @JsonProperty("entitlements")
    public Set<String> getEntitlements() {
        return entitlements;
    }

    @XmlElementWrapper(name = "reportletConfs")
    @XmlElement(name = "reportletConf")
    @JsonProperty("reportletConfs")
    public Set<String> getReportletConfs() {
        return reportletConfs;
    }

    @XmlElementWrapper(name = "accountRules")
    @XmlElement(name = "accountRule")
    @JsonProperty("accountRules")
    public Set<String> getAccountRules() {
        return accountRules;
    }

    @XmlElementWrapper(name = "passwordRules")
    @XmlElement(name = "passwordRule")
    @JsonProperty("passwordRules")
    public Set<String> getPasswordRules() {
        return passwordRules;
    }

    @XmlElementWrapper(name = "mappingItemTransformers")
    @XmlElement(name = "mappingItemTransformer")
    @JsonProperty("mappingItemTransformers")
    public Set<String> getMappingItemTransformers() {
        return mappingItemTransformers;
    }

    @XmlElementWrapper(name = "taskJobs")
    @XmlElement(name = "taskJob")
    @JsonProperty("taskJobs")
    public Set<String> getTaskJobs() {
        return taskJobs;
    }

    @XmlElementWrapper(name = "reconciliationFilterBuilders")
    @XmlElement(name = "reconciliationFilterBuilder")
    @JsonProperty("reconciliationFilterBuilders")
    public Set<String> getReconciliationFilterBuilders() {
        return reconciliationFilterBuilders;
    }

    @XmlElementWrapper(name = "logicActions")
    @XmlElement(name = "logicAction")
    @JsonProperty("logicActions")
    public Set<String> getLogicActions() {
        return logicActions;
    }

    @XmlElementWrapper(name = "propagationActions")
    @XmlElement(name = "propagationAction")
    @JsonProperty("propagationActions")
    public Set<String> getPropagationActions() {
        return propagationActions;
    }

    @XmlElementWrapper(name = "pullActions")
    @XmlElement(name = "pullAction")
    @JsonProperty("pullActions")
    public Set<String> getPullActions() {
        return pullActions;
    }

    @XmlElementWrapper(name = "pushActions")
    @XmlElement(name = "pushAction")
    @JsonProperty("pushActions")
    public Set<String> getPushActions() {
        return pushActions;
    }

    @XmlElementWrapper(name = "pullCorrelationRules")
    @XmlElement(name = "pullCorrelationRule")
    @JsonProperty("pullCorrelationRules")
    public Set<String> getPullCorrelationRules() {
        return pullCorrelationRules;
    }

    @XmlElementWrapper(name = "pushCorrelationRules")
    @XmlElement(name = "pushCorrelationRule")
    @JsonProperty("pushCorrelationRules")
    public Set<String> getPushCorrelationRules() {
        return pushCorrelationRules;
    }

    @XmlElementWrapper(name = "validators")
    @XmlElement(name = "validator")
    @JsonProperty("validators")
    public Set<String> getValidators() {
        return validators;
    }

    @XmlElementWrapper(name = "notificationRecipientsProviders")
    @XmlElement(name = "notificationRecipientsProvider")
    @JsonProperty("notificationRecipientsProviders")
    public Set<String> getNotificationRecipientsProviders() {
        return notificationRecipientsProviders;
    }

    public void setVersion(final String version) {
        this.version = version;
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

    public void setAnyObjectWorkflowAdapter(final String anyObjectWorkflowAdapter) {
        this.anyObjectWorkflowAdapter = anyObjectWorkflowAdapter;
    }

    public void setAnyObjectWorkflowAdapterSupportEdit(final boolean anyObjectWorkflowAdapterSupportEdit) {
        this.anyObjectWorkflowAdapterSupportEdit = anyObjectWorkflowAdapterSupportEdit;
    }

    public void setUserWorkflowAdapter(final String userWorkflowAdapter) {
        this.userWorkflowAdapter = userWorkflowAdapter;
    }

    public void setUserWorkflowAdapterSupportEdit(final boolean userWorkflowAdapterSupportEdit) {
        this.userWorkflowAdapterSupportEdit = userWorkflowAdapterSupportEdit;
    }

    public void setGroupWorkflowAdapter(final String groupWorkflowAdapter) {
        this.groupWorkflowAdapter = groupWorkflowAdapter;
    }

    public void setGroupWorkflowAdapterSupportEdit(final boolean groupWorkflowAdapterSupportEdit) {
        this.groupWorkflowAdapterSupportEdit = groupWorkflowAdapterSupportEdit;
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
