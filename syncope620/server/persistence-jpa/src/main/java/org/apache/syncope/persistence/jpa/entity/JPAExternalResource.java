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
package org.apache.syncope.persistence.jpa.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.PropagationMode;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.persistence.api.entity.AccountPolicy;
import org.apache.syncope.persistence.api.entity.ConnInstance;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.persistence.api.entity.SyncPolicy;
import org.apache.syncope.persistence.api.entity.role.RMapping;
import org.apache.syncope.persistence.api.entity.user.UMapping;
import org.apache.syncope.persistence.jpa.validation.entity.ExternalResourceCheck;
import org.apache.syncope.persistence.jpa.entity.role.JPARMapping;
import org.apache.syncope.persistence.jpa.entity.user.JPAUMapping;
import org.apache.syncope.server.utils.serialization.POJOHelper;
import org.identityconnectors.framework.common.objects.SyncToken;

/**
 * Resource for propagation and synchronization.
 */
@Entity
@Table(name = JPAExternalResource.TABLE)
@ExternalResourceCheck
public class JPAExternalResource extends AbstractAnnotatedEntity<String> implements ExternalResource {

    private static final long serialVersionUID = -6937712883512073278L;

    public static final String TABLE = "ExternalResource";

    /**
     * The resource identifier is the name.
     */
    @Id
    private String name;

    /**
     * Should this resource enforce the mandatory constraints?
     */
    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer enforceMandatoryCondition;

    /**
     * The resource type is identified by the associated connector.
     */
    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE })
    @NotNull
    private JPAConnInstance connector;

    /**
     * Mapping for user objects.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "resource")
    private JPAUMapping umapping;

    /**
     * Mapping for role objects.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "resource")
    private JPARMapping rmapping;

    /**
     * Is this resource primary, for propagations?
     */
    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer propagationPrimary;

    /**
     * Priority index for propagation ordering.
     */
    @Column(nullable = false)
    private Integer propagationPriority;

    /**
     * Generate random password for propagation, if not provided?
     */
    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer randomPwdIfNotProvided;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropagationMode propagationMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TraceLevel createTraceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TraceLevel updateTraceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TraceLevel deleteTraceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TraceLevel syncTraceLevel;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPAPasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPAAccountPolicy accountPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private JPASyncPolicy syncPolicy;

    /**
     * Configuration properties that are overridden from the connector instance.
     */
    @Lob
    private String jsonConf;

    /**
     * SyncToken for calling ConnId's sync() on users.
     */
    @Lob
    private String userializedSyncToken;

    /**
     * SyncToken for calling ConnId's sync() on roles.
     */
    @Lob
    private String rserializedSyncToken;

    /**
     * (Optional) classes for PropagationAction.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "action")
    @CollectionTable(name = "ExternalResource_PropActions",
            joinColumns =
            @JoinColumn(name = "ExternalResource_name", referencedColumnName = "name"))
    private List<String> propagationActionsClassNames = new ArrayList<>();

    /**
     * Default constructor.
     */
    public JPAExternalResource() {
        super();

        enforceMandatoryCondition = getBooleanAsInteger(false);
        propagationPrimary = 0;
        propagationPriority = 0;
        randomPwdIfNotProvided = 0;
        propagationMode = PropagationMode.TWO_PHASES;

        createTraceLevel = TraceLevel.FAILURES;
        updateTraceLevel = TraceLevel.FAILURES;
        deleteTraceLevel = TraceLevel.FAILURES;
        syncTraceLevel = TraceLevel.FAILURES;
    }

    @Override
    public boolean isEnforceMandatoryCondition() {
        return isBooleanAsInteger(enforceMandatoryCondition);
    }

    @Override
    public void setEnforceMandatoryCondition(boolean enforceMandatoryCondition) {
        this.enforceMandatoryCondition = getBooleanAsInteger(enforceMandatoryCondition);
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
    public UMapping getUmapping() {
        return umapping;
    }

    @Override
    public void setUmapping(final UMapping umapping) {
        checkType(umapping, JPAUMapping.class);
        this.umapping = (JPAUMapping) umapping;
    }

    @Override
    public RMapping getRmapping() {
        return rmapping;
    }

    @Override
    public void setRmapping(final RMapping rmapping) {
        checkType(rmapping, JPARMapping.class);
        this.rmapping = (JPARMapping) rmapping;
    }

    @Override
    public boolean isPropagationPrimary() {
        return isBooleanAsInteger(propagationPrimary);
    }

    @Override
    public void setPropagationPrimary(boolean propagationPrimary) {
        this.propagationPrimary = getBooleanAsInteger(propagationPrimary);
    }

    @Override
    public Integer getPropagationPriority() {
        return propagationPriority;
    }

    @Override
    public void setPropagationPriority(Integer propagationPriority) {
        if (propagationPriority != null) {
            this.propagationPriority = propagationPriority;
        }
    }

    @Override
    public boolean isRandomPwdIfNotProvided() {
        return isBooleanAsInteger(randomPwdIfNotProvided);
    }

    @Override
    public void setRandomPwdIfNotProvided(boolean randomPwdIfNotProvided) {
        this.randomPwdIfNotProvided = getBooleanAsInteger(randomPwdIfNotProvided);
    }

    @Override
    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    @Override
    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public void setKey(final String name) {
        this.name = name;
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
    public TraceLevel getSyncTraceLevel() {
        return syncTraceLevel;
    }

    @Override
    public void setSyncTraceLevel(final TraceLevel syncTraceLevel) {
        this.syncTraceLevel = syncTraceLevel;
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
    public SyncPolicy getSyncPolicy() {
        return syncPolicy;
    }

    @Override
    public void setSyncPolicy(final SyncPolicy syncPolicy) {
        checkType(syncPolicy, JPASyncPolicy.class);
        this.syncPolicy = (JPASyncPolicy) syncPolicy;
    }

    @Override
    public Set<ConnConfProperty> getConnInstanceConfiguration() {
        return StringUtils.isBlank(jsonConf)
                ? Collections.<ConnConfProperty>emptySet()
                : new HashSet<>(Arrays.asList(POJOHelper.deserialize(jsonConf, ConnConfProperty[].class)));
    }

    @Override
    public void setConnInstanceConfiguration(final Set<ConnConfProperty> properties) {
        jsonConf = POJOHelper.serialize(new HashSet<>(properties));
    }

    @Override
    public String getSerializedUSyncToken() {
        return userializedSyncToken;
    }

    @Override
    public SyncToken getUsyncToken() {
        return userializedSyncToken == null
                ? null
                : POJOHelper.deserialize(userializedSyncToken, SyncToken.class);
    }

    @Override
    public void setUsyncToken(final SyncToken syncToken) {
        this.userializedSyncToken = syncToken == null ? null : POJOHelper.serialize(syncToken);
    }

    @Override
    public String getSerializedRSyncToken() {
        return rserializedSyncToken;
    }

    @Override
    public SyncToken getRsyncToken() {
        return rserializedSyncToken == null
                ? null
                : POJOHelper.deserialize(rserializedSyncToken, SyncToken.class);
    }

    @Override
    public void setRsyncToken(final SyncToken syncToken) {
        this.rserializedSyncToken = syncToken == null ? null : POJOHelper.serialize(syncToken);
    }

    @Override
    public List<String> getPropagationActionsClassNames() {
        return propagationActionsClassNames;
    }
}
