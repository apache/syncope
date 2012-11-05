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
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.persistence.validation.entity.ExternalResourceCheck;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.TraceLevel;
import org.apache.syncope.util.XMLSerializer;
import org.identityconnectors.framework.common.objects.SyncToken;

/**
 * A resource to which propagation occurs.
 */
@Entity
@Cacheable
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
     * Attribute mappings.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "resource")
    @Valid
    private Set<SchemaMapping> mappings;

    /**
     * A JEXL expression for determining how to link user account id in Syncope DB to user account id in target
     * resource's DB.
     */
    private String accountLink;

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
     * SyncToken for calling ConnId's sync().
     */
    @Lob
    private String serializedSyncToken;

    /**
     * (Optional) class for PropagationAction.
     */
    private String actionsClassName;

    /**
     * Default constructor.
     */
    public ExternalResource() {
        super();

        enforceMandatoryCondition = getBooleanAsInteger(false);
        mappings = new HashSet<SchemaMapping>();
        propagationPrimary = 0;
        propagationPriority = 0;
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

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public Set<SchemaMapping> getMappings() {
        return mappings;
    }

    public Set<SchemaMapping> getMappings(final String intAttrName, final IntMappingType intMappingType) {
        Set<SchemaMapping> result = new HashSet<SchemaMapping>();
        for (SchemaMapping mapping : mappings) {
            if (intAttrName.equals(mapping.getIntAttrName()) && mapping.getIntMappingType() == intMappingType) {
                result.add(mapping);
            }
        }

        return result;
    }

    public boolean removeMapping(final SchemaMapping mapping) {
        return mappings.remove(mapping);
    }

    public boolean addMapping(final SchemaMapping mapping) {
        return mappings.contains(mapping) || mappings.add(mapping);
    }

    public void setMappings(final Set<SchemaMapping> mappings) {
        for (SchemaMapping mapping : this.mappings) {
            mapping.setResource(null);
        }
        this.mappings.clear();

        if (mappings != null) {
            this.mappings.addAll(mappings);
        }
    }

    public String getAccountLink() {
        return accountLink;
    }

    public void setAccountLink(final String accountLink) {
        this.accountLink = accountLink;
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

    public void setConnectorConfigurationProperties(final Set<ConnConfProperty> properties) {
        // create new set to make sure it's a serializable set implementation.
        xmlConfiguration = XMLSerializer.serialize(new HashSet<ConnConfProperty>(properties));
    }

    public Set<ConnConfProperty> getConfiguration() {
        Set<ConnConfProperty> result = Collections.emptySet();

        Set<ConnConfProperty> deserializedSet;
        if (StringUtils.isNotBlank(xmlConfiguration)) {
            deserializedSet = XMLSerializer.<HashSet<ConnConfProperty>>deserialize(xmlConfiguration);
            if (deserializedSet != null) {
                result = deserializedSet;
            }
        }

        return result;
    }

    public String getSerializedSyncToken() {
        return serializedSyncToken;
    }

    public SyncToken getSyncToken() {
        return serializedSyncToken == null
                ? null
                : XMLSerializer.<SyncToken>deserialize(serializedSyncToken);
    }

    public void setSerializedSyncToken(final String serializedSyncToken) {
        this.serializedSyncToken = serializedSyncToken;
    }

    public void setSyncToken(final SyncToken syncToken) {
        serializedSyncToken = XMLSerializer.serialize(syncToken);
    }

    public String getActionsClassName() {
        return actionsClassName;
    }

    public void setActionsClassName(final String actionsClassName) {
        this.actionsClassName = actionsClassName;
    }
}
