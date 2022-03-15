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
package org.apache.syncope.core.persistence.jpa.entity.resource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.jpa.validation.entity.ExternalResourceCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPropagationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushPolicy;
import org.identityconnectors.framework.common.objects.ObjectClass;

/**
 * Resource for propagation and pull.
 */
@Entity
@Table(name = JPAExternalResource.TABLE)
@ExternalResourceCheck
public class JPAExternalResource extends AbstractProvidedKeyEntity implements ExternalResource {

    private static final long serialVersionUID = -6937712883512073278L;

    public static final String TABLE = "ExternalResource";

    /**
     * Should this resource enforce the mandatory constraints?
     */
    @NotNull
    private Boolean enforceMandatoryCondition = false;

    /**
     * The resource type is identified by the associated connector.
     */
    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE })
    private JPAConnInstance connector;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "resource")
    private List<JPAProvision> provisions = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "resource")
    private JPAOrgUnit orgUnit;

    /**
     * Priority index for propagation ordering.
     */
    private Integer propagationPriority;

    /**
     * Generate random password, if not provided.
     */
    @NotNull
    private Boolean randomPwdIfNotProvided = false;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel createTraceLevel = TraceLevel.FAILURES;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel updateTraceLevel = TraceLevel.FAILURES;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel deleteTraceLevel = TraceLevel.FAILURES;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel provisioningTraceLevel = TraceLevel.FAILURES;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccountPolicy accountPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPropagationPolicy propagationPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPullPolicy pullPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPushPolicy pushPolicy;

    @ManyToOne
    private JPAImplementation provisionSorter;

    /**
     * Configuration properties that are overridden from the connector instance.
     */
    @Lob
    private String jsonConf;

    @NotNull
    private Boolean overrideCapabilities = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "capabilityOverride")
    @CollectionTable(name = "ExternalResource_capOverride",
            joinColumns =
            @JoinColumn(name = "resource_id", referencedColumnName = "id"))
    private Set<ConnectorCapability> capabilitiesOverride = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "PropAction",
            joinColumns =
            @JoinColumn(name = "resource_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "resource_id", "implementation_id" }))
    private List<JPAImplementation> propagationActions = new ArrayList<>();

    @Override
    public boolean isEnforceMandatoryCondition() {
        return enforceMandatoryCondition;
    }

    @Override
    public void setEnforceMandatoryCondition(final boolean enforceMandatoryCondition) {
        this.enforceMandatoryCondition = enforceMandatoryCondition;
    }

    @Override
    public ConnInstance getConnector() {
        return connector;
    }

    @Override
    public void setConnector(final ConnInstance connector) {
        checkType(connector, JPAConnInstance.class);
        this.connector = (JPAConnInstance) connector;
    }

    @Override
    public boolean add(final Provision provision) {
        checkType(provision, JPAProvision.class);
        return this.provisions.add((JPAProvision) provision);
    }

    @Override
    public Optional<? extends Provision> getProvision(final String anyType) {
        return getProvisions().stream().
                filter(provision -> provision.getAnyType().getKey().equals(anyType)).findFirst();
    }

    @Override
    public Optional<? extends Provision> getProvision(final AnyType anyType) {
        return getProvisions().stream().
                filter(provision -> provision.getAnyType().equals(anyType)).findFirst();
    }

    @Override
    public Optional<? extends Provision> getProvision(final ObjectClass objectClass) {
        return getProvisions().stream().
                filter(provision -> provision.getObjectClass().equals(objectClass)).findFirst();
    }

    @Override
    public List<? extends Provision> getProvisions() {
        return provisions == null ? List.of() : provisions;
    }

    @Override
    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    @Override
    public void setOrgUnit(final OrgUnit orgUnit) {
        checkType(orgUnit, JPAOrgUnit.class);
        this.orgUnit = (JPAOrgUnit) orgUnit;
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
    public boolean isRandomPwdIfNotProvided() {
        return randomPwdIfNotProvided;
    }

    @Override
    public void setRandomPwdIfNotProvided(final boolean randomPwdIfNotProvided) {
        this.randomPwdIfNotProvided = randomPwdIfNotProvided;
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
    public AccountPolicy getAccountPolicy() {
        return accountPolicy;
    }

    @Override
    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        checkType(accountPolicy, JPAAccountPolicy.class);
        this.accountPolicy = (JPAAccountPolicy) accountPolicy;
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        checkType(passwordPolicy, JPAPasswordPolicy.class);
        this.passwordPolicy = (JPAPasswordPolicy) passwordPolicy;
    }

    @Override
    public PropagationPolicy getPropagationPolicy() {
        return propagationPolicy;
    }

    @Override
    public void setPropagationPolicy(final PropagationPolicy propagationPolicy) {
        checkType(propagationPolicy, JPAPropagationPolicy.class);
        this.propagationPolicy = (JPAPropagationPolicy) propagationPolicy;
    }

    @Override
    public PullPolicy getPullPolicy() {
        return pullPolicy;
    }

    @Override
    public void setPullPolicy(final PullPolicy pullPolicy) {
        checkType(pullPolicy, JPAPullPolicy.class);
        this.pullPolicy = (JPAPullPolicy) pullPolicy;
    }

    @Override
    public PushPolicy getPushPolicy() {
        return pushPolicy;
    }

    @Override
    public void setPushPolicy(final PushPolicy pushPolicy) {
        checkType(pushPolicy, JPAPushPolicy.class);
        this.pushPolicy = (JPAPushPolicy) pushPolicy;
    }

    @Override
    public Implementation getProvisionSorter() {
        return provisionSorter;
    }

    @Override
    public void setProvisionSorter(final Implementation provisionSorter) {
        checkType(provisionSorter, JPAImplementation.class);
        checkImplementationType(provisionSorter, IdMImplementationType.PROVISION_SORTER);
        this.provisionSorter = (JPAImplementation) provisionSorter;
    }

    @Override
    public Set<ConnConfProperty> getConfOverride() {
        Set<ConnConfProperty> confOverride = new HashSet<>();
        if (!StringUtils.isBlank(jsonConf)) {
            confOverride.addAll(List.of(POJOHelper.deserialize(jsonConf, ConnConfProperty[].class)));
        }

        return confOverride;
    }

    @Override
    public void setConfOverride(final Set<ConnConfProperty> confOverride) {
        jsonConf = POJOHelper.serialize(new HashSet<>(confOverride));
    }

    @Override
    public boolean isOverrideCapabilities() {
        return overrideCapabilities;
    }

    @Override
    public void setOverrideCapabilities(final boolean overrideCapabilities) {
        this.overrideCapabilities = overrideCapabilities;
    }

    @Override
    public Set<ConnectorCapability> getCapabilitiesOverride() {
        return capabilitiesOverride;
    }

    @Override
    public boolean add(final Implementation propagationAction) {
        checkType(propagationAction, JPAImplementation.class);
        checkImplementationType(propagationAction, IdMImplementationType.PROPAGATION_ACTIONS);
        return propagationActions.contains((JPAImplementation) propagationAction)
                || propagationActions.add((JPAImplementation) propagationAction);
    }

    @Override
    public List<? extends Implementation> getPropagationActions() {
        return propagationActions;
    }
}
