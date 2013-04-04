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
package org.apache.syncope.core.persistence.beans;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.core.persistence.beans.role.RMapping;
import org.apache.syncope.core.persistence.beans.user.UMapping;
import org.apache.syncope.core.persistence.validation.entity.ExternalResourceCheck;
import org.apache.syncope.core.util.XMLSerializer;
import org.identityconnectors.framework.common.objects.SyncToken;

/**
 * Resource for propagation and synchronization.
 */
@Entity
@ExternalResourceCheck
public class ExternalResource extends AbstractBaseBean {

    private static final long serialVersionUID = -6937712883512073278L;

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
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE})
    @NotNull
    private ConnInstance connector;

    /**
     * Mapping for user objects.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "resource")
    private UMapping umapping;

    /**
     * Mapping for role objects.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "resource")
    private RMapping rmapping;

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
    private PasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private AccountPolicy accountPolicy;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    private SyncPolicy syncPolicy;

    /**
     * Configuration properties that are overridden from the connector instance.
     */
    @Lob
    private String xmlConfiguration;

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
     * (Optional) class for PropagationAction.
     */
    private String propagationActionsClassName;

    /**
     * Default constructor.
     */
    public ExternalResource() {
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

    public boolean isEnforceMandatoryCondition() {
        return isBooleanAsInteger(enforceMandatoryCondition);
    }

    public void setEnforceMandatoryCondition(boolean enforceMandatoryCondition) {
        this.enforceMandatoryCondition = getBooleanAsInteger(enforceMandatoryCondition);
    }

    public ConnInstance getConnector() {
        return connector;
    }

    public void setConnector(ConnInstance connector) {
        this.connector = connector;
    }

    public UMapping getUmapping() {
        return umapping;
    }

    public void setUmapping(final UMapping umapping) {
        this.umapping = umapping;
    }

    public RMapping getRmapping() {
        return rmapping;
    }

    public void setRmapping(final RMapping rmapping) {
        this.rmapping = rmapping;
    }

    public boolean isPropagationPrimary() {
        return isBooleanAsInteger(propagationPrimary);
    }

    public void setPropagationPrimary(boolean propagationPrimary) {
        this.propagationPrimary = getBooleanAsInteger(propagationPrimary);
    }

    public Integer getPropagationPriority() {
        return propagationPriority;
    }

    public void setPropagationPriority(Integer propagationPriority) {
        if (propagationPriority != null) {
            this.propagationPriority = propagationPriority;
        }
    }

    public boolean isRandomPwdIfNotProvided() {
        return isBooleanAsInteger(randomPwdIfNotProvided);
    }

    public void setRandomPwdIfNotProvided(boolean randomPwdIfNotProvided) {
        this.randomPwdIfNotProvided = getBooleanAsInteger(randomPwdIfNotProvided);
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    public TraceLevel getSyncTraceLevel() {
        return syncTraceLevel;
    }

    public void setSyncTraceLevel(final TraceLevel syncTraceLevel) {
        this.syncTraceLevel = syncTraceLevel;
    }

    public AccountPolicy getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public SyncPolicy getSyncPolicy() {
        return syncPolicy;
    }

    public void setSyncPolicy(final SyncPolicy syncPolicy) {
        this.syncPolicy = syncPolicy;
    }

    public Set<ConnConfProperty> getConnInstanceConfiguration() {
        Set<ConnConfProperty> result = null;
        if (StringUtils.isNotBlank(xmlConfiguration)) {
            result = XMLSerializer.<HashSet<ConnConfProperty>>deserialize(xmlConfiguration);
        }
        if (result == null) {
            result = Collections.emptySet();
        }

        return result;
    }

    public void setConnInstanceConfiguration(final Set<ConnConfProperty> properties) {
        xmlConfiguration = XMLSerializer.serialize(new HashSet<ConnConfProperty>(properties));
    }

    public String getUserializedSyncToken() {
        return userializedSyncToken;
    }

    public SyncToken getUsyncToken() {
        return userializedSyncToken == null
                ? null
                : XMLSerializer.<SyncToken>deserialize(userializedSyncToken);
    }

    public void setUserializedSyncToken(final String serializedSyncToken) {
        this.userializedSyncToken = serializedSyncToken;
    }

    public void setUsyncToken(final SyncToken syncToken) {
        this.userializedSyncToken = XMLSerializer.serialize(syncToken);
    }

    public String getRserializedSyncToken() {
        return rserializedSyncToken;
    }

    public SyncToken getRsyncToken() {
        return rserializedSyncToken == null
                ? null
                : XMLSerializer.<SyncToken>deserialize(rserializedSyncToken);
    }

    public void setRserializedSyncToken(final String serializedSyncToken) {
        this.rserializedSyncToken = serializedSyncToken;
    }

    public void setRsyncToken(final SyncToken syncToken) {
        this.rserializedSyncToken = XMLSerializer.serialize(syncToken);
    }

    public String getPropagationActionsClassName() {
        return propagationActionsClassName;
    }

    public void setPropagationActionsClassName(final String propagationActionsClassName) {
        this.propagationActionsClassName = propagationActionsClassName;
    }
}
