/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
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

    /**
     * Propagation mode to be used when not mandatory in propagation.
     */
    private PropagationMode optionalPropagationMode;

    /**
     * Force mandatory constraint.
     */
    private boolean forceMandatoryConstraint;

    private TraceLevel createTraceLevel;

    private TraceLevel deleteTraceLevel;

    private TraceLevel updateTraceLevel;

    private Long passwordPolicy;

    private Long accountPolicy;

    private Set<ConnConfProperty> connectorConfigurationProperties;

    public ResourceTO() {
        mappings = new ArrayList<SchemaMappingTO>();
        connectorConfigurationProperties = new HashSet<ConnConfProperty>();
        optionalPropagationMode = PropagationMode.ASYNC;
        createTraceLevel = TraceLevel.ALL;
        deleteTraceLevel = TraceLevel.ALL;
        updateTraceLevel = TraceLevel.ALL;
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

    public PropagationMode getOptionalPropagationMode() {
        return optionalPropagationMode;
    }

    public void setOptionalPropagationMode(
            PropagationMode optionalPropagationMode) {

        this.optionalPropagationMode = optionalPropagationMode;
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

    public Set<ConnConfProperty> getConnectorConfigurationProperties() {
        return connectorConfigurationProperties;
    }

    public void setConnectorConfigurationProperties(
            final Set<ConnConfProperty> connectorConfigurationProperties) {
        this.connectorConfigurationProperties =
                connectorConfigurationProperties;
    }
}
