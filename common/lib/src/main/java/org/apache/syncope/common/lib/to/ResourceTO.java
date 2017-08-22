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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.TraceLevel;

@XmlRootElement(name = "resource")
@XmlType
public class ResourceTO extends AbstractBaseBean implements EntityTO {

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

    private String pullPolicy;

    private final List<ConnConfProperty> confOverride = new ArrayList<>();

    private boolean overrideCapabilities = false;

    private final Set<ConnectorCapability> capabilitiesOverride = EnumSet.noneOf(ConnectorCapability.class);

    private final List<String> propagationActionsClassNames = new ArrayList<>();

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

    public String getPullPolicy() {
        return pullPolicy;
    }

    public void setPullPolicy(final String pullPolicy) {
        this.pullPolicy = pullPolicy;
    }

    @JsonIgnore
    public Optional<ProvisionTO> getProvision(final String anyType) {
        return provisions.stream().filter(
                provision -> anyType != null && anyType.equals(provision.getAnyType())).
                findFirst();
    }

    @XmlElementWrapper(name = "provisions")
    @XmlElement(name = "provision")
    @JsonProperty("provisions")
    public List<ProvisionTO> getProvisions() {
        return provisions;
    }

    public OrgUnitTO getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(final OrgUnitTO orgUnit) {
        this.orgUnit = orgUnit;
    }

    @XmlElementWrapper(name = "confOverride")
    @XmlElement(name = "property")
    @JsonProperty("confOverride")
    public List<ConnConfProperty> getConfOverride() {
        return confOverride;
    }

    public boolean isOverrideCapabilities() {
        return overrideCapabilities;
    }

    public void setOverrideCapabilities(final boolean overrideCapabilities) {
        this.overrideCapabilities = overrideCapabilities;
    }

    @XmlElementWrapper(name = "capabilitiesOverride")
    @XmlElement(name = "capability")
    @JsonProperty("capabilitiesOverride")
    public Set<ConnectorCapability> getCapabilitiesOverride() {
        return capabilitiesOverride;
    }

    public TraceLevel getProvisioningTraceLevel() {
        return provisioningTraceLevel;
    }

    public void setProvisioningTraceLevel(final TraceLevel provisioningTraceLevel) {
        this.provisioningTraceLevel = provisioningTraceLevel;
    }

    @XmlElementWrapper(name = "propagationActionsClassNames")
    @XmlElement(name = "propagationActionsClassName")
    @JsonProperty("propagationActionsClassNames")
    public List<String> getPropagationActionsClassNames() {
        return propagationActionsClassNames;
    }

}
