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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.TraceLevel;

public class ResourceTO implements EntityTO {

    private static final long serialVersionUID = -9193551354041698963L;

    private String key;

    /**
     * The resource type is identified by the associated connector.
     */
    private String connector;

    /**
     * Convenience information: display name for the connector id.
     */
    private String connectorDisplayName;

    private final List<Provision> provisions = new ArrayList<>();

    private OrgUnit orgUnit;

    private Integer propagationPriority;

    private boolean enforceMandatoryCondition;

    private TraceLevel createTraceLevel = TraceLevel.ALL;

    private TraceLevel updateTraceLevel = TraceLevel.ALL;

    private TraceLevel deleteTraceLevel = TraceLevel.ALL;

    private TraceLevel provisioningTraceLevel = TraceLevel.ALL;

    private String passwordPolicy;

    private String accountPolicy;

    private String propagationPolicy;

    private String inboundPolicy;

    private String pushPolicy;

    private String provisionSorter;

    private String authPolicy;

    private String accessPolicy;

    @JsonProperty
    private boolean confOverrideFlag = false;

    @JsonProperty
    private List<ConnConfProperty> confOverrideValue;

    @JsonProperty
    private boolean capabilitiesOverrideFlag = false;

    @JsonProperty
    private Set<ConnectorCapability> capabilitiesOverrideValue;

    private final List<String> propagationActions = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public boolean isEnforceMandatoryCondition() {
        return enforceMandatoryCondition;
    }

    public void setEnforceMandatoryCondition(final boolean enforceMandatoryCondition) {
        this.enforceMandatoryCondition = enforceMandatoryCondition;
    }

    public String getConnector() {
        return connector;
    }

    public void setConnector(final String connector) {
        this.connector = connector;
    }

    public String getConnectorDisplayName() {
        return connectorDisplayName;
    }

    public void setConnectorDisplayName(final String connectorDisplayName) {
        this.connectorDisplayName = connectorDisplayName;
    }

    public Integer getPropagationPriority() {
        return propagationPriority;
    }

    public void setPropagationPriority(final Integer propagationPriority) {
        this.propagationPriority = propagationPriority;
    }

    public TraceLevel getCreateTraceLevel() {
        return createTraceLevel;
    }

    public void setCreateTraceLevel(final TraceLevel createTraceLevel) {
        this.createTraceLevel = createTraceLevel;
    }

    public TraceLevel getUpdateTraceLevel() {
        return updateTraceLevel;
    }

    public void setUpdateTraceLevel(final TraceLevel updateTraceLevel) {
        this.updateTraceLevel = updateTraceLevel;
    }

    public TraceLevel getDeleteTraceLevel() {
        return deleteTraceLevel;
    }

    public void setDeleteTraceLevel(final TraceLevel deleteTraceLevel) {
        this.deleteTraceLevel = deleteTraceLevel;
    }

    public TraceLevel getProvisioningTraceLevel() {
        return provisioningTraceLevel;
    }

    public void setProvisioningTraceLevel(final TraceLevel provisioningTraceLevel) {
        this.provisioningTraceLevel = provisioningTraceLevel;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public String getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final String accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public String getPropagationPolicy() {
        return propagationPolicy;
    }

    public void setPropagationPolicy(final String propagationPolicy) {
        this.propagationPolicy = propagationPolicy;
    }

    public String getInboundPolicy() {
        return inboundPolicy;
    }

    public void setInboundPolicy(final String inboundPolicy) {
        this.inboundPolicy = inboundPolicy;
    }

    public String getPushPolicy() {
        return pushPolicy;
    }

    public void setPushPolicy(final String pushPolicy) {
        this.pushPolicy = pushPolicy;
    }

    public String getAuthPolicy() {
        return authPolicy;
    }

    public void setAuthPolicy(final String authPolicy) {
        this.authPolicy = authPolicy;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(final String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getProvisionSorter() {
        return provisionSorter;
    }

    public void setProvisionSorter(final String provisionSorter) {
        this.provisionSorter = provisionSorter;
    }

    @JsonIgnore
    public Optional<Provision> getProvision(final String anyType) {
        return provisions.stream().filter(
                provision -> anyType != null && anyType.equals(provision.getAnyType())).
                findFirst();
    }

    public List<Provision> getProvisions() {
        return provisions;
    }

    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(final OrgUnit orgUnit) {
        this.orgUnit = orgUnit;
    }

    @JsonIgnore
    public Optional<List<ConnConfProperty>> getConfOverride() {
        return confOverrideFlag ? Optional.ofNullable(confOverrideValue) : Optional.empty();
    }

    @JsonIgnore
    public void setConfOverride(final Optional<List<ConnConfProperty>> confOverride) {
        if (confOverride == null || confOverride.isEmpty()) {
            confOverrideFlag = false;
            confOverrideValue = null;
        } else {
            confOverrideFlag = true;
            confOverrideValue = new ArrayList<>(confOverride.orElseThrow());
        }
    }

    @JsonIgnore
    public Optional<Set<ConnectorCapability>> getCapabilitiesOverride() {
        return capabilitiesOverrideFlag ? Optional.ofNullable(capabilitiesOverrideValue) : Optional.empty();
    }

    @JsonIgnore
    public void setCapabilitiesOverride(final Optional<Set<ConnectorCapability>> capabilitiesOverride) {
        if (capabilitiesOverride == null || capabilitiesOverride.isEmpty()) {
            capabilitiesOverrideFlag = false;
            capabilitiesOverrideValue = null;
        } else {
            capabilitiesOverrideFlag = true;
            capabilitiesOverrideValue = new HashSet<>(capabilitiesOverride.orElseThrow());
        }
    }

    public List<String> getPropagationActions() {
        return propagationActions;
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
        ResourceTO other = (ResourceTO) obj;
        return new EqualsBuilder().
                append(enforceMandatoryCondition, other.enforceMandatoryCondition).
                append(key, other.key).
                append(connector, other.connector).
                append(connectorDisplayName, other.connectorDisplayName).
                append(provisions, other.provisions).
                append(orgUnit, other.orgUnit).
                append(propagationPriority, other.propagationPriority).
                append(createTraceLevel, other.createTraceLevel).
                append(updateTraceLevel, other.updateTraceLevel).
                append(deleteTraceLevel, other.deleteTraceLevel).
                append(provisioningTraceLevel, other.provisioningTraceLevel).
                append(passwordPolicy, other.passwordPolicy).
                append(accountPolicy, other.accountPolicy).
                append(propagationPolicy, other.propagationPolicy).
                append(inboundPolicy, other.inboundPolicy).
                append(pushPolicy, other.pushPolicy).
                append(authPolicy, other.authPolicy).
                append(accessPolicy, other.accessPolicy).
                append(confOverrideFlag, other.confOverrideFlag).
                append(confOverrideValue, other.confOverrideValue).
                append(capabilitiesOverrideFlag, other.capabilitiesOverrideFlag).
                append(capabilitiesOverrideValue, other.capabilitiesOverrideValue).
                append(propagationActions, other.propagationActions).
                append(provisionSorter, other.provisionSorter).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(connector).
                append(connectorDisplayName).
                append(provisions).
                append(orgUnit).
                append(propagationPriority).
                append(enforceMandatoryCondition).
                append(createTraceLevel).
                append(updateTraceLevel).
                append(deleteTraceLevel).
                append(provisioningTraceLevel).
                append(passwordPolicy).
                append(accountPolicy).
                append(propagationPolicy).
                append(inboundPolicy).
                append(pushPolicy).
                append(authPolicy).
                append(accessPolicy).
                append(confOverrideFlag).
                append(confOverrideValue).
                append(capabilitiesOverrideFlag).
                append(capabilitiesOverrideValue).
                append(propagationActions).
                append(provisionSorter).
                build();
    }
}
