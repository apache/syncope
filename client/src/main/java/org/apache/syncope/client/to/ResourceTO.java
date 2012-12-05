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
package org.apache.syncope.client.to;

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.client.AbstractBaseBean;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.TraceLevel;

public class ResourceTO extends AbstractBaseBean {

    private static final long serialVersionUID = -9193551354041698963L;

    /**
     * The resource identifier is the name.
     */
    private String name;

    /**
     * The resource type is identified by the associated connector.
     */
    private Long connectorId;

    private MappingTO umapping;

    private MappingTO rmapping;

    private boolean propagationPrimary;

    private int propagationPriority;

    private PropagationMode propagationMode;

    private boolean enforceMandatoryCondition;

    private TraceLevel createTraceLevel;

    private TraceLevel updateTraceLevel;

    private TraceLevel deleteTraceLevel;

    private TraceLevel syncTraceLevel;

    private Long passwordPolicy;

    private Long accountPolicy;

    private Long syncPolicy;

    private Set<ConnConfProperty> connConfProperties;

    private String usyncToken;

    private String rsyncToken;

    private String propagationActionsClassName;

    public ResourceTO() {
        super();

        connConfProperties = new HashSet<ConnConfProperty>();
        propagationMode = PropagationMode.TWO_PHASES;
        propagationPriority = 0;

        createTraceLevel = TraceLevel.ALL;
        updateTraceLevel = TraceLevel.ALL;
        deleteTraceLevel = TraceLevel.ALL;
        syncTraceLevel = TraceLevel.ALL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnforceMandatoryCondition() {
        return enforceMandatoryCondition;
    }

    public void setEnforceMandatoryCondition(boolean enforceMandatoryCondition) {
        this.enforceMandatoryCondition = enforceMandatoryCondition;
    }

    public Long getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Long connectorId) {
        this.connectorId = connectorId;
    }

    public MappingTO getUmapping() {
        return umapping;
    }

    public void setUmapping(MappingTO umapping) {
        this.umapping = umapping;
    }

    public MappingTO getRmapping() {
        return rmapping;
    }

    public void setRmapping(MappingTO rmapping) {
        this.rmapping = rmapping;
    }

    public boolean isPropagationPrimary() {
        return propagationPrimary;
    }

    public void setPropagationPrimary(boolean propagationPrimary) {
        this.propagationPrimary = propagationPrimary;
    }

    public int getPropagationPriority() {
        return propagationPriority;
    }

    public void setPropagationPriority(int propagationPriority) {
        this.propagationPriority = propagationPriority;
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public TraceLevel getCreateTraceLevel() {
        return createTraceLevel;
    }

    public void setCreateTraceLevel(TraceLevel createTraceLevel) {
        this.createTraceLevel = createTraceLevel;
    }

    public TraceLevel getDeleteTraceLevel() {
        return deleteTraceLevel;
    }

    public void setDeleteTraceLevel(TraceLevel deleteTraceLevel) {
        this.deleteTraceLevel = deleteTraceLevel;
    }

    public TraceLevel getUpdateTraceLevel() {
        return updateTraceLevel;
    }

    public void setUpdateTraceLevel(TraceLevel updateTraceLevel) {
        this.updateTraceLevel = updateTraceLevel;
    }

    public Long getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(Long passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public Long getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(Long accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public Long getSyncPolicy() {
        return syncPolicy;
    }

    public void setSyncPolicy(Long syncPolicy) {
        this.syncPolicy = syncPolicy;
    }

    public Set<ConnConfProperty> getConnConfProperties() {
        return connConfProperties;
    }

    public void setConnectorConfigurationProperties(final Set<ConnConfProperty> connConfProperties) {
        this.connConfProperties = connConfProperties;
    }

    public TraceLevel getSyncTraceLevel() {
        return syncTraceLevel;
    }

    public void setSyncTraceLevel(final TraceLevel syncTraceLevel) {
        this.syncTraceLevel = syncTraceLevel;
    }

    public String getUsyncToken() {
        return usyncToken;
    }

    public void setUsyncToken(final String syncToken) {
        this.usyncToken = syncToken;
    }

    public String getRsyncToken() {
        return rsyncToken;
    }

    public void setRsyncToken(final String syncToken) {
        this.rsyncToken = syncToken;
    }

    public String getPropagationActionsClassName() {
        return propagationActionsClassName;
    }

    public void setPropagationActionsClassName(final String propagationActionsClassName) {
        this.propagationActionsClassName = propagationActionsClassName;
    }
}
