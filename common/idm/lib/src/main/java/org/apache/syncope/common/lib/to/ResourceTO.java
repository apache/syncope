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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.PathParam;
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

    private final List<ProvisionTO> provisions = new ArrayList<>();

    private OrgUnitTO orgUnit;

    private Integer propagationPriority;

    private boolean randomPwdIfNotProvided;

    private boolean enforceMandatoryCondition;

    private TraceLevel createTraceLevel = TraceLevel.ALL;

    private TraceLevel updateTraceLevel = TraceLevel.ALL;

    private TraceLevel deleteTraceLevel = TraceLevel.ALL;

    private TraceLevel provisioningTraceLevel = TraceLevel.ALL;

    private String passwordPolicy;

    private String accountPolicy;

    private String propagationPolicy;

    private String pullPolicy;

    private String pushPolicy;

    private String provisionSorter;

    private String authPolicy;

    private String accessPolicy;

    private final List<ConnConfProperty> confOverride = new ArrayList<>();

    private boolean overrideCapabilities = false;

    private final Set<ConnectorCapability> capabilitiesOverride = EnumSet.noneOf(ConnectorCapability.class);

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

    public boolean isRandomPwdIfNotProvided() {
        return randomPwdIfNotProvided;
    }

    public void setRandomPwdIfNotProvided(final boolean randomPwdIfNotProvided) {
        this.randomPwdIfNotProvided = randomPwdIfNotProvided;
    }

    public TraceLevel getCreateTraceLevel() {
        return createTraceLevel;
    }

    public void setCreateTraceLevel(final TraceLevel createTraceLevel) {
        this.createTraceLevel = createTraceLevel;
    }

    public TraceLevel getDeleteTraceLevel() {
        return deleteTraceLevel;
    }

    public void setDeleteTraceLevel(final TraceLevel deleteTraceLevel) {
        this.deleteTraceLevel = deleteTraceLevel;
    }

    public TraceLevel getUpdateTraceLevel() {
        return updateTraceLevel;
    }

    public void setUpdateTraceLevel(final TraceLevel updateTraceLevel) {
        this.updateTraceLevel = updateTraceLevel;
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

    public String getPullPolicy() {
        return pullPolicy;
    }

    public void setPullPolicy(final String pullPolicy) {
        this.pullPolicy = pullPolicy;
    }

    public String getPushPolicy() {
        return pushPolicy;
    }

    public void setPushPolicy(final String pushPolicy) {
        this.pushPolicy = pushPolicy;
    }

    public String getProvisionSorter() {
        return provisionSorter;
    }

    public void setProvisionSorter(final String provisionSorter) {
        this.provisionSorter = provisionSorter;
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

    @JsonIgnore
    public Optional<ProvisionTO> getProvision(final String anyType) {
        return provisions.stream().filter(
                provision -> anyType != null && anyType.equals(provision.getAnyType())).
                findFirst();
    }

    public List<ProvisionTO> getProvisions() {
        return provisions;
    }

    public OrgUnitTO getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(final OrgUnitTO orgUnit) {
        this.orgUnit = orgUnit;
    }

    public List<ConnConfProperty> getConfOverride() {
        return confOverride;
    }

    public boolean isOverrideCapabilities() {
        return overrideCapabilities;
    }

    public void setOverrideCapabilities(final boolean overrideCapabilities) {
        this.overrideCapabilities = overrideCapabilities;
    }

    public Set<ConnectorCapability> getCapabilitiesOverride() {
        return capabilitiesOverride;
    }

    public TraceLevel getProvisioningTraceLevel() {
        return provisioningTraceLevel;
    }

    public void setProvisioningTraceLevel(final TraceLevel provisioningTraceLevel) {
        this.provisioningTraceLevel = provisioningTraceLevel;
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
                append(randomPwdIfNotProvided, other.randomPwdIfNotProvided).
                append(enforceMandatoryCondition, other.enforceMandatoryCondition).
                append(overrideCapabilities, other.overrideCapabilities).
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
                append(pullPolicy, other.pullPolicy).
                append(pushPolicy, other.pushPolicy).
                append(authPolicy, other.authPolicy).
                append(accessPolicy, other.accessPolicy).
                append(confOverride, other.confOverride).
                append(capabilitiesOverride, other.capabilitiesOverride).
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
                append(randomPwdIfNotProvided).
                append(enforceMandatoryCondition).
                append(createTraceLevel).
                append(updateTraceLevel).
                append(deleteTraceLevel).
                append(provisioningTraceLevel).
                append(passwordPolicy).
                append(accountPolicy).
                append(propagationPolicy).
                append(pullPolicy).
                append(pushPolicy).
                append(authPolicy).
                append(accessPolicy).
                append(confOverride).
                append(overrideCapabilities).
                append(capabilitiesOverride).
                append(propagationActions).
                append(provisionSorter).
                build();
    }
}
