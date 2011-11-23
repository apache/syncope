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
package org.syncope.core.persistence.beans;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Type;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.util.XmlSerializer;
import org.syncope.core.persistence.validation.entity.ExternalResourceCheck;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.PropagationMode;
import org.syncope.types.IntMappingType;
import org.syncope.types.TraceLevel;
import com.thoughtworks.xstream.XStream;

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
    private Integer forceMandatoryConstraint;

    /**
     * The resource type is identified by the associated connector.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private ConnInstance connector;

    /**
     * Users associated to this resource.
     */
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "externalResources")
    private Set<SyncopeUser> users;

    /**
     * Roles associated to this resource.
     */
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "externalResources")
    private Set<SyncopeRole> roles;

    /**
     * Attribute mappings.
     */
    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true,
    mappedBy = "resource")
    @Valid
    private List<SchemaMapping> mappings;

    /**
     * A JEXL expression for determining how to link user account id in
     * Syncope DB to user account id in target resource's DB.
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
    @Type(type = "org.hibernate.type.StringClobType")
    private String xmlConfiguration;

    /**
     * SyncToken for calling ConnId's sync().
     */
    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String serializedSyncToken;

    /**
     * Default constructor.
     */
    public ExternalResource() {
        super();

        forceMandatoryConstraint = getBooleanAsInteger(false);
        users = new HashSet<SyncopeUser>();
        roles = new HashSet<SyncopeRole>();
        mappings = new ArrayList<SchemaMapping>();
        propagationPrimary = 0;
        propagationPriority = 0;
        propagationMode = PropagationMode.ASYNC;

        createTraceLevel = TraceLevel.FAILURES;
        updateTraceLevel = TraceLevel.FAILURES;
        deleteTraceLevel = TraceLevel.FAILURES;
        syncTraceLevel = TraceLevel.FAILURES;
    }

    public boolean isForceMandatoryConstraint() {
        return isBooleanAsInteger(forceMandatoryConstraint);
    }

    public void setForceMandatoryConstraint(boolean forceMandatoryConstraint) {
        this.forceMandatoryConstraint =
                getBooleanAsInteger(forceMandatoryConstraint);
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

    public List<SchemaMapping> getMappings() {
        return mappings;
    }

    public List<SchemaMapping> getMappings(final String intAttrName,
            final IntMappingType intMappingType) {

        List<SchemaMapping> result = new ArrayList<SchemaMapping>();
        for (SchemaMapping mapping : mappings) {
            if (intAttrName.equals(mapping.getIntAttrName())
                    && mapping.getIntMappingType() == intMappingType) {

                result.add(mapping);
            }
        }

        return result;
    }

    public SchemaMapping getAccountIdMapping() {
        SchemaMapping result = null;

        for (SchemaMapping mapping : mappings) {
            if (mapping.isAccountid()) {
                result = mapping;
            }
        }

        return result;
    }

    public boolean removeMapping(SchemaMapping mapping) {
        return mappings == null || mappings.remove(mapping);
    }

    public boolean addMapping(SchemaMapping mapping) {
        return mappings.contains(mapping) || mappings.add(mapping);
    }

    public void setMappings(List<SchemaMapping> mappings) {
        this.mappings.clear();
        if (mappings != null) {
            this.mappings.addAll(mappings);
        }
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

    public boolean addRole(SyncopeRole role) {
        return roles.add(role);
    }

    public boolean removeRole(SyncopeRole role) {
        return roles.remove(role);
    }

    public Set<SyncopeRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles.clear();
        if (roles != null && !roles.isEmpty()) {
            this.roles.addAll(roles);
        }
    }

    public boolean addUser(SyncopeUser user) {
        return users.add(user);
    }

    public boolean removeUser(SyncopeUser user) {
        return users.remove(user);
    }

    public Set<SyncopeUser> getUsers() {
        return users;
    }

    public void setUsers(Set<SyncopeUser> users) {
        this.users.clear();
        if (users != null && !users.isEmpty()) {
            this.users.addAll(users);
        }
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

    public TraceLevel getSyncTraceLevel() {
        return syncTraceLevel;
    }

    public void setSyncTraceLevel(TraceLevel syncTraceLevel) {
        this.syncTraceLevel = syncTraceLevel;
    }

    public AccountPolicy getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(AccountPolicy accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public SyncPolicy getSyncPolicy() {
        return syncPolicy;
    }

    public void setSyncPolicy(SyncPolicy syncPolicy) {
        this.syncPolicy = syncPolicy;
    }

    public void setConnectorConfigurationProperties(
            final Set<ConnConfProperty> properties) {

        // create new set to make sure it's a serializable set implementation.
        xmlConfiguration = XmlSerializer.serialize(
                new HashSet<ConnConfProperty>(properties));
    }

    public Set<ConnConfProperty> getConfiguration() {
        Set<ConnConfProperty> result = Collections.EMPTY_SET;

        Set<ConnConfProperty> deserializedSet;
        if (StringUtils.isNotBlank(xmlConfiguration)) {
            deserializedSet = XmlSerializer.<HashSet<ConnConfProperty>>
                    deserialize(xmlConfiguration);
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
        SyncToken result = null;

        if (serializedSyncToken != null) {
            ConfigurableApplicationContext context =
                    ApplicationContextManager.getApplicationContext();
            XStream xStream = context.getBean(XStream.class);
            try {
                result = (SyncToken) xStream.fromXML(
                        URLDecoder.decode(serializedSyncToken, "UTF-8"));
            } catch (Throwable t) {
                LOG.error("During attribute deserialization", t);
            }
        }

        return result;
    }

    public void setSerializedSyncToken(final String serializedSyncToken) {
        this.serializedSyncToken = serializedSyncToken;
    }

    public void setSyncToken(final SyncToken syncToken) {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        XStream xStream = context.getBean(XStream.class);
        try {
            serializedSyncToken = URLEncoder.encode(
                    xStream.toXML(syncToken), "UTF-8");
        } catch (Throwable t) {
            LOG.error("During attribute serialization", t);
        }
    }
}
