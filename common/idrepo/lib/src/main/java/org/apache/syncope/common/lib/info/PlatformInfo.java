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

    public static class ProvisioningInfo implements BaseBean {

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
    }

    public static class WorkflowInfo implements BaseBean {

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
    }

    public static class PersistenceInfo implements BaseBean {

        private static final long serialVersionUID = 2902980556801069487L;

        private String entityFactory;

        private String plainSchemaDAO;

        private String plainAttrDAO;

        private String plainAttrValueDAO;

        private String anySearchDAO;

        private String userDAO;

        private String groupDAO;

        private String anyObjectDAO;

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
    }

    private String keymasterConfParamOps;

    private String keymasterServiceOps;

    private final ProvisioningInfo provisioningInfo = new ProvisioningInfo();

    private final WorkflowInfo workflowInfo = new WorkflowInfo();

    private final PersistenceInfo persistenceInfo = new PersistenceInfo();

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

    private final Set<String> implementationTypes = new HashSet<>();

    private final Set<JavaImplInfo> javaImplInfos = new HashSet<>();

    public String getKeymasterConfParamOps() {
        return keymasterConfParamOps;
    }

    public String getKeymasterServiceOps() {
        return keymasterServiceOps;
    }

    public ProvisioningInfo getProvisioningInfo() {
        return provisioningInfo;
    }

    public WorkflowInfo getWorkflowInfo() {
        return workflowInfo;
    }

    public PersistenceInfo getPersistenceInfo() {
        return persistenceInfo;
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

    @JacksonXmlElementWrapper(localName = "connIdLocations")
    @JacksonXmlProperty(localName = "connIdLocation")
    public Set<String> getConnIdLocations() {
        return connIdLocations;
    }

    public String getPasswordGenerator() {
        return passwordGenerator;
    }

    public void setPasswordGenerator(final String passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
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

    public void setKeymasterConfParamOps(final String keymasterConfParamOps) {
        this.keymasterConfParamOps = keymasterConfParamOps;
    }

    public void setKeymasterServiceOps(final String keymasterServiceOps) {
        this.keymasterServiceOps = keymasterServiceOps;
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
