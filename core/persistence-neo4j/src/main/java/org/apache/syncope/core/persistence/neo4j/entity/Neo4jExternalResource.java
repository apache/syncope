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
package org.apache.syncope.core.persistence.neo4j.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.common.validation.ExternalResourceCheck;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jInboundPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPropagationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushPolicy;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jExternalResource.NODE)
@ExternalResourceCheck
public class Neo4jExternalResource extends AbstractProvidedKeyNode implements ExternalResource {

    private static final long serialVersionUID = -6937712883512073278L;

    public static final String NODE = "ExternalResource";

    public static final String RESOURCE_CONNECTOR_REL = "RESOURCE_CONNECTOR";

    public static final String RESOURCE_PASSWORD_POLICY_REL = "RESOURCE_PASSWORD_POLICY";

    public static final String RESOURCE_ACCOUNT_POLICY_REL = "RESOURCE_ACCOUNT_POLICY";

    public static final String RESOURCE_PROPAGATION_POLICY_REL = "RESOURCE_PROPAGATION_POLICY";

    public static final String RESOURCE_INBOUND_POLICY_REL = "RESOURCE_INBOUND_POLICY";

    public static final String RESOURCE_PUSH_POLICY_REL = "RESOURCE_PUSH_POLICY";

    public static final String RESOURCE_PROVISION_SORTER_REL = "RESOURCE_PROVISION_SORTER";

    public static final String RESOURCE_PROPAGATION_ACTIONS_REL = "RESOURCE_PROPAGATION_ACTIONS";

    protected static final TypeReference<List<ConnConfProperty>> CONN_CONF_PROPS_TYPEREF =
            new TypeReference<List<ConnConfProperty>>() {
    };

    protected static final TypeReference<Set<ConnectorCapability>> CONNECTOR_CAPABILITY_TYPEREF =
            new TypeReference<Set<ConnectorCapability>>() {
    };

    protected static final TypeReference<List<Provision>> PROVISION_TYPEREF =
            new TypeReference<List<Provision>>() {
    };

    /**
     * Should this resource enforce the mandatory constraints?
     */
    @NotNull
    private Boolean enforceMandatoryCondition = false;

    /**
     * Priority index for propagation ordering.
     */
    private Integer propagationPriority;

    @NotNull
    private TraceLevel createTraceLevel = TraceLevel.FAILURES;

    @NotNull
    private TraceLevel updateTraceLevel = TraceLevel.FAILURES;

    @NotNull
    private TraceLevel deleteTraceLevel = TraceLevel.FAILURES;

    @NotNull
    private TraceLevel provisioningTraceLevel = TraceLevel.FAILURES;

    /**
     * Configuration properties that are override from the connector instance.
     */
    private String jsonConf;

    private String capabilitiesOverride;

    private String provisions;

    @Transient
    private final List<Provision> provisionList = new ArrayList<>();

    private String orgUnit;

    /**
     * The resource type is identified by the associated connector.
     */
    @Relationship(type = RESOURCE_CONNECTOR_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jConnInstance connector;

    @Relationship(type = RESOURCE_PASSWORD_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jPasswordPolicy passwordPolicy;

    @Relationship(type = RESOURCE_ACCOUNT_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAccountPolicy accountPolicy;

    @Relationship(type = RESOURCE_PROPAGATION_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jPropagationPolicy propagationPolicy;

    @Relationship(type = RESOURCE_INBOUND_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jInboundPolicy inboundPolicy;

    @Relationship(type = RESOURCE_PUSH_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jPushPolicy pushPolicy;

    @Relationship(type = RESOURCE_PROVISION_SORTER_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation provisionSorter;

    @Relationship(type = RESOURCE_PROPAGATION_ACTIONS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jImplementationRelationship> propagationActions = new TreeSet<>();

    @Transient
    private List<Neo4jImplementation> sortedPropagationActions = new SortedSetList<>(
            propagationActions, Neo4jImplementationRelationship.builder());

    @Override
    public boolean isEnforceMandatoryCondition() {
        return enforceMandatoryCondition;
    }

    @Override
    public void setEnforceMandatoryCondition(final boolean enforceMandatoryCondition) {
        this.enforceMandatoryCondition = enforceMandatoryCondition;
    }

    @Override
    public Optional<Provision> getProvisionByAnyType(final String anyType) {
        return getProvisions().stream().filter(provision -> provision.getAnyType().equals(anyType)).findFirst();
    }

    @Override
    public Optional<Provision> getProvisionByObjectClass(final String objectClass) {
        return getProvisions().stream().filter(provision -> provision.getObjectClass().equals(objectClass)).findFirst();
    }

    @Override
    public List<Provision> getProvisions() {
        return provisionList;
    }

    @Override
    public OrgUnit getOrgUnit() {
        return Optional.ofNullable(orgUnit).map(ou -> POJOHelper.deserialize(ou, OrgUnit.class)).orElse(null);
    }

    @Override
    public void setOrgUnit(final OrgUnit orgUnit) {
        this.orgUnit = orgUnit == null ? null : POJOHelper.serialize(orgUnit);
    }

    @Override
    public Integer getPropagationPriority() {
        return propagationPriority;
    }

    @Override
    public void setPropagationPriority(final Integer propagationPriority) {
        this.propagationPriority = propagationPriority;
    }

    @Override
    public TraceLevel getCreateTraceLevel() {
        return createTraceLevel;
    }

    @Override
    public void setCreateTraceLevel(final TraceLevel createTraceLevel) {
        this.createTraceLevel = createTraceLevel;
    }

    @Override

    public TraceLevel getDeleteTraceLevel() {
        return deleteTraceLevel;
    }

    @Override
    public void setDeleteTraceLevel(final TraceLevel deleteTraceLevel) {
        this.deleteTraceLevel = deleteTraceLevel;
    }

    @Override
    public TraceLevel getUpdateTraceLevel() {
        return updateTraceLevel;
    }

    @Override
    public void setUpdateTraceLevel(final TraceLevel updateTraceLevel) {
        this.updateTraceLevel = updateTraceLevel;
    }

    @Override
    public TraceLevel getProvisioningTraceLevel() {
        return provisioningTraceLevel;
    }

    @Override
    public void setProvisioningTraceLevel(final TraceLevel provisioningTraceLevel) {
        this.provisioningTraceLevel = provisioningTraceLevel;
    }

    @Override
    public ConnInstance getConnector() {
        return connector;
    }

    @Override
    public void setConnector(final ConnInstance connector) {
        checkType(connector, Neo4jConnInstance.class);
        this.connector = (Neo4jConnInstance) connector;
    }

    @Override
    public AccountPolicy getAccountPolicy() {
        return accountPolicy;
    }

    @Override
    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        checkType(accountPolicy, Neo4jAccountPolicy.class);
        this.accountPolicy = (Neo4jAccountPolicy) accountPolicy;
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        checkType(passwordPolicy, Neo4jPasswordPolicy.class);
        this.passwordPolicy = (Neo4jPasswordPolicy) passwordPolicy;
    }

    @Override
    public PropagationPolicy getPropagationPolicy() {
        return propagationPolicy;
    }

    @Override
    public void setPropagationPolicy(final PropagationPolicy propagationPolicy) {
        checkType(propagationPolicy, Neo4jPropagationPolicy.class);
        this.propagationPolicy = (Neo4jPropagationPolicy) propagationPolicy;
    }

    @Override
    public InboundPolicy getInboundPolicy() {
        return inboundPolicy;
    }

    @Override
    public void setInboundPolicy(final InboundPolicy inboundPolicy) {
        checkType(inboundPolicy, Neo4jInboundPolicy.class);
        this.inboundPolicy = (Neo4jInboundPolicy) inboundPolicy;
    }

    @Override
    public PushPolicy getPushPolicy() {
        return pushPolicy;
    }

    @Override
    public void setPushPolicy(final PushPolicy pushPolicy) {
        checkType(pushPolicy, Neo4jPushPolicy.class);
        this.pushPolicy = (Neo4jPushPolicy) pushPolicy;
    }

    @Override
    public Implementation getProvisionSorter() {
        return provisionSorter;
    }

    @Override
    public void setProvisionSorter(final Implementation provisionSorter) {
        checkType(provisionSorter, Neo4jImplementation.class);
        checkImplementationType(provisionSorter, IdMImplementationType.PROVISION_SORTER);
        this.provisionSorter = (Neo4jImplementation) provisionSorter;
    }

    @Override
    public Optional<List<ConnConfProperty>> getConfOverride() {
        return StringUtils.isBlank(jsonConf)
                ? Optional.empty()
                : Optional.of(POJOHelper.deserialize(jsonConf, CONN_CONF_PROPS_TYPEREF));
    }

    @Override
    public void setConfOverride(final Optional<List<ConnConfProperty>> confOverride) {
        confOverride.ifPresentOrElse(
                conf -> jsonConf = POJOHelper.serialize(conf),
                () -> jsonConf = null);
    }

    @Override
    public Optional<Set<ConnectorCapability>> getCapabilitiesOverride() {
        return StringUtils.isBlank(capabilitiesOverride)
                ? Optional.empty()
                : Optional.of(POJOHelper.deserialize(capabilitiesOverride, CONNECTOR_CAPABILITY_TYPEREF));
    }

    @Override
    public void setCapabilitiesOverride(final Optional<Set<ConnectorCapability>> capabilitiesOverride) {
        capabilitiesOverride.ifPresentOrElse(
                override -> this.capabilitiesOverride = POJOHelper.serialize(override),
                () -> this.capabilitiesOverride = null);
    }

    @Override
    public boolean add(final Implementation propagationAction) {
        checkType(propagationAction, Neo4jImplementation.class);
        checkImplementationType(propagationAction, IdMImplementationType.PROPAGATION_ACTIONS);
        return sortedPropagationActions.contains((Neo4jImplementation) propagationAction)
                || sortedPropagationActions.add((Neo4jImplementation) propagationAction);
    }

    @Override
    public List<? extends Implementation> getPropagationActions() {
        return sortedPropagationActions;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getProvisions().clear();
        }
        if (provisions != null) {
            getProvisions().addAll(POJOHelper.deserialize(provisions, PROVISION_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        sortedPropagationActions = new SortedSetList<>(propagationActions, Neo4jImplementationRelationship.builder());
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        provisions = POJOHelper.serialize(getProvisions());
    }
}
