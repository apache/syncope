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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "syncope")
@XmlType
public class SyncopeTO extends AbstractBaseBean {

    private static final long serialVersionUID = -7941853999417673827L;

    private String version;

    private boolean selfRegAllowed;

    private boolean pwdResetAllowed;

    private boolean pwdResetRequiringSecurityQuestions;

    private final List<String> connIdLocations = new ArrayList<>();

    private String attributableTransformer;

    private String userWorkflowAdapter;

    private String groupWorkflowAdapter;

    private String userProvisioningManager;

    private String groupProvisioningManager;

    private String virAttrCache;

    private final List<String> reportlets = new ArrayList<>();

    private final List<String> taskJobs = new ArrayList<>();

    private final List<String> propagationActions = new ArrayList<>();

    private final List<String> syncActions = new ArrayList<>();

    private final List<String> pushActions = new ArrayList<>();

    private final List<String> syncCorrelationRules = new ArrayList<>();

    private final List<String> pushCorrelationRules = new ArrayList<>();

    private final List<String> validators = new ArrayList<>();

    private final List<String> mailTemplates = new ArrayList<>();

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
    public List<String> getConnIdLocations() {
        return connIdLocations;
    }

    public String getAttributableTransformer() {
        return attributableTransformer;
    }

    public String getUserWorkflowAdapter() {
        return userWorkflowAdapter;
    }

    public String getGroupWorkflowAdapter() {
        return groupWorkflowAdapter;
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

    @XmlElementWrapper(name = "reportlets")
    @XmlElement(name = "reportlet")
    @JsonProperty("reportlets")
    public List<String> getReportlets() {
        return reportlets;
    }

    @XmlElementWrapper(name = "taskJobs")
    @XmlElement(name = "taskJob")
    @JsonProperty("taskJobs")
    public List<String> getTaskJobs() {
        return taskJobs;
    }

    @XmlElementWrapper(name = "propagationActions")
    @XmlElement(name = "propagationAction")
    @JsonProperty("propagationActions")
    public List<String> getPropagationActions() {
        return propagationActions;
    }

    @XmlElementWrapper(name = "syncActions")
    @XmlElement(name = "syncAction")
    @JsonProperty("syncActions")
    public List<String> getSyncActions() {
        return syncActions;
    }

    @XmlElementWrapper(name = "pushActions")
    @XmlElement(name = "pushAction")
    @JsonProperty("pushActions")
    public List<String> getPushActions() {
        return pushActions;
    }

    @XmlElementWrapper(name = "syncCorrelationRules")
    @XmlElement(name = "syncCorrelationRule")
    @JsonProperty("syncCorrelationRules")
    public List<String> getSyncCorrelationRules() {
        return syncCorrelationRules;
    }

    @XmlElementWrapper(name = "pushCorrelationRules")
    @XmlElement(name = "pushCorrelationRule")
    @JsonProperty("pushCorrelationRules")
    public List<String> getPushCorrelationRules() {
        return pushCorrelationRules;
    }

    @XmlElementWrapper(name = "validators")
    @XmlElement(name = "validator")
    @JsonProperty("validators")
    public List<String> getValidators() {
        return validators;
    }

    @XmlElementWrapper(name = "mailTemplates")
    @XmlElement(name = "mailTemplate")
    @JsonProperty("mailTemplates")
    public List<String> getMailTemplates() {
        return mailTemplates;
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

    public void setAttributableTransformer(final String attributableTransformer) {
        this.attributableTransformer = attributableTransformer;
    }

    public void setUserWorkflowAdapter(final String userWorkflowAdapter) {
        this.userWorkflowAdapter = userWorkflowAdapter;
    }

    public void setGroupWorkflowAdapter(final String groupWorkflowAdapter) {
        this.groupWorkflowAdapter = groupWorkflowAdapter;
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
