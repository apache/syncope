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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.PropagationMode;
import org.syncope.types.TraceLevel;

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

    /**
     * Attribute mappings.
     */
    private List<SchemaMappingTO> mappings;

    private String accountLink;

    private boolean propagationPrimary;

    private int propagationPriority;

    private PropagationMode propagationMode;

    /**
     * Force mandatory constraint.
     */
    private boolean forceMandatoryConstraint;

    private TraceLevel createTraceLevel;

    private TraceLevel updateTraceLevel;

    private TraceLevel deleteTraceLevel;

    private TraceLevel syncTraceLevel;

    private Long passwordPolicy;

    private Long accountPolicy;

    private Long syncPolicy;

    private Set<ConnConfProperty> connConfProperties;

    private String syncToken;

    public ResourceTO() {
        mappings = new ArrayList<SchemaMappingTO>();
        connConfProperties = new HashSet<ConnConfProperty>();
        propagationMode = PropagationMode.TWO_PHASES;
        propagationPriority = 0;

        createTraceLevel = TraceLevel.ALL;
        updateTraceLevel = TraceLevel.ALL;
        deleteTraceLevel = TraceLevel.ALL;
        syncTraceLevel = TraceLevel.ALL;
    }

    public boolean isForceMandatoryConstraint() {
        return forceMandatoryConstraint;
    }

    public void setForceMandatoryConstraint(boolean forceMandatoryConstraint) {
        this.forceMandatoryConstraint = forceMandatoryConstraint;
    }

    public Long getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Long connectorId) {
        this.connectorId = connectorId;
    }

    public boolean addMapping(SchemaMappingTO mapping) {
        return mappings.add(mapping);
    }

    public boolean removeMapping(SchemaMappingTO mapping) {
        return mappings.remove(mapping);
    }

    public List<SchemaMappingTO> getMappings() {
        return mappings;
    }

    public void setMappings(List<SchemaMappingTO> mappings) {
        this.mappings = mappings;
    }

    public String getAccountLink() {
        return accountLink;
    }

    public void setAccountLink(String accountLink) {
        this.accountLink = accountLink;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setConnectorConfigurationProperties(
            final Set<ConnConfProperty> connConfProperties) {
        this.connConfProperties = connConfProperties;
    }

    public TraceLevel getSyncTraceLevel() {
        return syncTraceLevel;
    }

    public void setSyncTraceLevel(TraceLevel syncTraceLevel) {
        this.syncTraceLevel = syncTraceLevel;
    }

    public String getSyncToken() {
        return syncToken;
    }

    public void setSyncToken(String syncToken) {
        this.syncToken = syncToken;
    }
}
