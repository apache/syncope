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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.ImplementationType;

@XmlRootElement(name = "platformInfo")
@XmlType
public class PlatformInfo implements Serializable {

    private static final long serialVersionUID = -7941853999417673827L;

    @XmlRootElement(name = "provisioningInfo")
    @XmlType
    public static class ProvisioningInfo implements Serializable {

        private static final long serialVersionUID = 533340357732839568L;

        private String propagationTaskExecutor;

        private String virAttrCache;

        private String anyObjectProvisioningManager;

        private String userProvisioningManager;

        private String groupProvisioningManager;

        private String notificationManager;

        private String auditManager;

        public String getPropagationTaskExecutor() {
            return propagationTaskExecutor;
        }

        public void setPropagationTaskExecutor(final String propagationTaskExecutor) {
            this.propagationTaskExecutor = propagationTaskExecutor;
        }

        public String getVirAttrCache() {
            return virAttrCache;
        }

        public void setVirAttrCache(final String virAttrCache) {
            this.virAttrCache = virAttrCache;
        }

        public String getAnyObjectProvisioningManager() {
            return anyObjectProvisioningManager;
        }

        public void setAnyObjectProvisioningManager(final String anyObjectProvisioningManager) {
            this.anyObjectProvisioningManager = anyObjectProvisioningManager;
        }

        public String getUserProvisioningManager() {
            return userProvisioningManager;
        }

        public void setUserProvisioningManager(final String userProvisioningManager) {
            this.userProvisioningManager = userProvisioningManager;
        }

        public String getGroupProvisioningManager() {
            return groupProvisioningManager;
        }

        public void setGroupProvisioningManager(final String groupProvisioningManager) {
            this.groupProvisioningManager = groupProvisioningManager;
        }

        public String getNotificationManager() {
            return notificationManager;
        }

        public void setNotificationManager(final String notificationManager) {
            this.notificationManager = notificationManager;
        }

        public String getAuditManager() {
            return auditManager;
        }

        public void setAuditManager(final String auditManager) {
            this.auditManager = auditManager;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().
                    append(propagationTaskExecutor).
                    append(virAttrCache).
                    append(anyObjectProvisioningManager).
                    append(userProvisioningManager).
                    append(groupProvisioningManager).
                    append(notificationManager).
                    append(auditManager).
                    build();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ProvisioningInfo other = (ProvisioningInfo) obj;
            return new EqualsBuilder().
                    append(propagationTaskExecutor, other.propagationTaskExecutor).
                    append(virAttrCache, other.virAttrCache).
                    append(anyObjectProvisioningManager, other.anyObjectProvisioningManager).
                    append(userProvisioningManager, other.userProvisioningManager).
                    append(groupProvisioningManager, other.groupProvisioningManager).
                    append(notificationManager, other.notificationManager).
                    append(auditManager, other.auditManager).
                    build();
        }
    }

    @XmlRootElement(name = "workflowInfo")
    @XmlType
    public static class WorkflowInfo implements Serializable {

        private static final long serialVersionUID = 6736937721099195324L;

        private String anyObjectWorkflowAdapter;

        private String userWorkflowAdapter;

        private String groupWorkflowAdapter;

        public String getAnyObjectWorkflowAdapter() {
            return anyObjectWorkflowAdapter;
        }

        public void setAnyObjectWorkflowAdapter(final String anyObjectWorkflowAdapter) {
            this.anyObjectWorkflowAdapter = anyObjectWorkflowAdapter;
        }

        public String getUserWorkflowAdapter() {
            return userWorkflowAdapter;
        }

        public void setUserWorkflowAdapter(final String userWorkflowAdapter) {
            this.userWorkflowAdapter = userWorkflowAdapter;
        }

        public String getGroupWorkflowAdapter() {
            return groupWorkflowAdapter;
        }

        public void setGroupWorkflowAdapter(final String groupWorkflowAdapter) {
            this.groupWorkflowAdapter = groupWorkflowAdapter;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().
                    append(anyObjectWorkflowAdapter).
                    append(userWorkflowAdapter).
                    append(groupWorkflowAdapter).
                    build();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final WorkflowInfo other = (WorkflowInfo) obj;
            return new EqualsBuilder().
                    append(anyObjectWorkflowAdapter, other.anyObjectWorkflowAdapter).
                    append(userWorkflowAdapter, other.userWorkflowAdapter).
                    append(groupWorkflowAdapter, other.groupWorkflowAdapter).
                    build();
        }
    }

    @XmlRootElement(name = "persistenceInfo")
    @XmlType
    public static class PersistenceInfo implements Serializable {

        private static final long serialVersionUID = 2902980556801069487L;

        private String entityFactory;

        private String plainSchemaDAO;

        private String plainAttrDAO;

        private String plainAttrValueDAO;

        private String anySearchDAO;

        private String userDAO;

        private String groupDAO;

        private String anyObjectDAO;

        private String confDAO;

        public String getEntityFactory() {
            return entityFactory;
        }

        public void setEntityFactory(final String entityFactory) {
            this.entityFactory = entityFactory;
        }

        public String getPlainSchemaDAO() {
            return plainSchemaDAO;
        }

        public void setPlainSchemaDAO(final String plainSchemaDAO) {
            this.plainSchemaDAO = plainSchemaDAO;
        }

        public String getPlainAttrDAO() {
            return plainAttrDAO;
        }

        public void setPlainAttrDAO(final String plainAttrDAO) {
            this.plainAttrDAO = plainAttrDAO;
        }

        public String getPlainAttrValueDAO() {
            return plainAttrValueDAO;
        }

        public void setPlainAttrValueDAO(final String plainAttrValueDAO) {
            this.plainAttrValueDAO = plainAttrValueDAO;
        }

        public String getAnySearchDAO() {
            return anySearchDAO;
        }

        public void setAnySearchDAO(final String anySearchDAO) {
            this.anySearchDAO = anySearchDAO;
        }

        public String getUserDAO() {
            return userDAO;
        }

        public void setUserDAO(final String userDAO) {
            this.userDAO = userDAO;
        }

        public String getGroupDAO() {
            return groupDAO;
        }

        public void setGroupDAO(final String groupDAO) {
            this.groupDAO = groupDAO;
        }

        public String getAnyObjectDAO() {
            return anyObjectDAO;
        }

        public void setAnyObjectDAO(final String anyObjectDAO) {
            this.anyObjectDAO = anyObjectDAO;
        }

        public String getConfDAO() {
            return confDAO;
        }

        public void setConfDAO(final String confDAO) {
            this.confDAO = confDAO;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().
                    append(entityFactory).
                    append(plainSchemaDAO).
                    append(plainAttrDAO).
                    append(plainAttrValueDAO).
                    append(anySearchDAO).
                    append(userDAO).
                    append(groupDAO).
                    append(anyObjectDAO).
                    append(confDAO).
                    build();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PersistenceInfo other = (PersistenceInfo) obj;
            return new EqualsBuilder().
                    append(entityFactory, other.entityFactory).
                    append(plainSchemaDAO, other.plainSchemaDAO).
                    append(plainAttrDAO, other.plainAttrDAO).
                    append(plainAttrValueDAO, other.plainAttrValueDAO).
                    append(anySearchDAO, other.anySearchDAO).
                    append(userDAO, other.userDAO).
                    append(groupDAO, other.groupDAO).
                    append(anyObjectDAO, other.anyObjectDAO).
                    append(confDAO, other.confDAO).
                    build();
        }
    }

    private String version;

    private String buildNumber;

    private ProvisioningInfo provisioningInfo;

    private WorkflowInfo workflowInfo;

    private PersistenceInfo persistenceInfo;

    private boolean selfRegAllowed;

    private boolean pwdResetAllowed;

    private boolean pwdResetRequiringSecurityQuestions;

    private final Set<String> connIdLocations = new HashSet<>();

    private String passwordGenerator;

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

    public ProvisioningInfo getProvisioningInfo() {
        return provisioningInfo;
    }

    public void setProvisioningInfo(final ProvisioningInfo provisioningInfo) {
        this.provisioningInfo = provisioningInfo;
    }

    public WorkflowInfo getWorkflowInfo() {
        return workflowInfo;
    }

    public void setWorkflowInfo(final WorkflowInfo workflowInfo) {
        this.workflowInfo = workflowInfo;
    }

    public PersistenceInfo getPersistenceInfo() {
        return persistenceInfo;
    }

    public void setPersistenceInfo(final PersistenceInfo persistenceInfo) {
        this.persistenceInfo = persistenceInfo;
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

    public String getPasswordGenerator() {
        return passwordGenerator;
    }

    public void setPasswordGenerator(final String passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(version).
                append(buildNumber).
                append(provisioningInfo).
                append(workflowInfo).
                append(persistenceInfo).
                append(selfRegAllowed).
                append(pwdResetAllowed).
                append(pwdResetRequiringSecurityQuestions).
                append(connIdLocations).
                append(passwordGenerator).
                append(anyTypes).
                append(userClasses).
                append(anyTypeClasses).
                append(resources).
                append(entitlements).
                append(javaImplInfos).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlatformInfo other = (PlatformInfo) obj;
        return new EqualsBuilder().
                append(version, other.version).
                append(buildNumber, other.buildNumber).
                append(provisioningInfo, other.provisioningInfo).
                append(workflowInfo, other.workflowInfo).
                append(persistenceInfo, other.persistenceInfo).
                append(selfRegAllowed, other.selfRegAllowed).
                append(pwdResetAllowed, other.pwdResetAllowed).
                append(pwdResetRequiringSecurityQuestions, other.pwdResetRequiringSecurityQuestions).
                append(connIdLocations, other.connIdLocations).
                append(passwordGenerator, other.passwordGenerator).
                append(anyTypes, other.anyTypes).
                append(userClasses, other.userClasses).
                append(anyTypeClasses, other.anyTypeClasses).
                append(resources, other.resources).
                append(entitlements, other.entitlements).
                append(javaImplInfos, other.javaImplInfos).
                build();
    }
}
