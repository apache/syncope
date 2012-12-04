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
package org.apache.syncope.core.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.core.util.SchemaMappingUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.mod.AttributeMod;
import org.apache.syncope.to.AttributeTO;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.PropagationOperation;
import org.apache.syncope.types.SchemaType;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manage the data propagation to external resources.
 */
@Transactional(rollbackFor = {Throwable.class})
public class PropagationManager {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    /**
     * User DataBinder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Resource DAO.
     */
    @Autowired
    private ResourceDAO resourceDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    private SchemaDAO schemaDAO;

    /**
     * JEXL engine for evaluating connector's account link.
     */
    @Autowired
    private JexlUtil jexlUtil;

    protected SyncopeUser getSyncopeUser(final Long userId)
            throws NotFoundException {

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return user;
    }

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getCreateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final List<AttributeTO> vAttrs)
            throws NotFoundException {

        return getCreateTaskIds(wfResult, password, vAttrs, null);
    }

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow.
     * @param password to be set.
     * @param vAttrs virtual attributes to be set.
     * @param syncResourceNames external resources performing sync, hence not to be considered for propagation.
     * @return list of propagation tasks.
     * @throws NotFoundException if userId is not found.
     */
    public List<PropagationTask> getCreateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final List<AttributeTO> vAttrs, final Set<String> syncResourceNames)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(wfResult.getResult().getKey());
        if (vAttrs != null && !vAttrs.isEmpty()) {
            userDataBinder.fillVirtual(user, vAttrs, AttributableUtil.getInstance(AttributableType.USER));
        }

        final PropagationByResource propByRes = wfResult.getPropByRes();
        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.emptyList();
        }

        if (syncResourceNames != null) {
            propByRes.get(PropagationOperation.CREATE).removeAll(syncResourceNames);
        }

        return createTasks(user, password, wfResult.getResult().getValue(), false, propByRes);
    }

    /**
     * Performs update on each resource associated to the user excluding the specified into 'resourceNames' parameter.
     *
     * @param user to be propagated.
     * @param enable whether user must be enabled or not.
     * @param syncResourceNames external resource names not to be considered for propagation. Use this during sync and
     * disable/enable actions limited to the external resources only.
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(final SyncopeUser user, final Boolean enable,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        return getUpdateTaskIds(user, // SyncopeUser to be updated on external resources
                null, // no propagation by resources
                null, // no password
                null, // no virtual attributes to be managed
                null, // no virtual attributes to be managed
                enable, // status to be propagated
                syncResourceNames);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow.
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult)
            throws NotFoundException {

        return getUpdateTaskIds(wfResult, null, null, null, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow.
     * @param password to be updated.
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be added.
     * @return list of propagation tasks.
     * @throws NotFoundException if userId is not found.
     */
    public List<PropagationTask> getUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated)
            throws NotFoundException {

        return getUpdateTaskIds(wfResult, password, vAttrsToBeRemoved, vAttrsToBeUpdated, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow.
     * @param password to be updated.
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be added.
     * @param syncResourceNames external resource names not to be considered for propagation. Use this during sync and
     * disable/enable actions limited to the external resources only.
     * @return list of propagation tasks.
     * @throws NotFoundException if userId is not found.
     */
    public List<PropagationTask> getUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(wfResult.getResult().getKey());

        return getUpdateTaskIds(user, wfResult.getPropByRes(), password, vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.
                getResult().getValue(), syncResourceNames);
    }

    private List<PropagationTask> getUpdateTaskIds(final SyncopeUser user, final PropagationByResource propByRes,
            final String password, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final Boolean enable, final Set<String> syncResourceNames)
            throws NotFoundException {

        PropagationByResource localPropByRes = userDataBinder.fillVirtual(user, vAttrsToBeRemoved == null
                ? Collections.EMPTY_SET
                : vAttrsToBeRemoved, vAttrsToBeUpdated == null
                ? Collections.EMPTY_SET
                : vAttrsToBeUpdated, AttributableUtil.getInstance(AttributableType.USER));

        if (propByRes != null && !propByRes.isEmpty()) {
            localPropByRes.merge(propByRes);
        } else {
            localPropByRes.addAll(PropagationOperation.UPDATE, user.getResourceNames());
        }

        if (syncResourceNames != null) {
            localPropByRes.get(PropagationOperation.CREATE).removeAll(syncResourceNames);
            localPropByRes.get(PropagationOperation.UPDATE).removeAll(syncResourceNames);
            localPropByRes.get(PropagationOperation.DELETE).removeAll(syncResourceNames);
        }

        return createTasks(user, password, enable, false, localPropByRes);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     */
    public List<PropagationTask> getDeleteTaskIds(final Long userId)
            throws NotFoundException {

        return getDeleteTaskIds(userId, null);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @param syncResourceName name of external resource performing sync, hence not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     */
    public List<PropagationTask> getDeleteTaskIds(final Long userId, final String syncResourceName)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(userId);

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.DELETE, user.getResourceNames());
        if (syncResourceName != null) {
            propByRes.get(PropagationOperation.DELETE).remove(syncResourceName);
        }

        return createTasks(user, null, false, true, propByRes);
    }

    /**
     * Prepare an attribute for sending to a connector instance.
     *
     * @param mapping schema mapping for the given attribute
     * @param user given user
     * @param password clear-text password
     * @return account link + prepared attribute
     * @throws ClassNotFoundException if schema type for given mapping does not exists in current class loader
     */
    private Map.Entry<String, Attribute> prepareAttribute(final SchemaMapping mapping, final SyncopeUser user,
            final String password)
            throws ClassNotFoundException {

        final List<AbstractAttributable> attributables = new ArrayList<AbstractAttributable>();

        switch (mapping.getIntMappingType().getAttributableType()) {
            case USER:
                attributables.addAll(Collections.singleton(user));
                break;
            case ROLE:
                final List<Membership> memberships = user.getMemberships();
                for (Membership membership : memberships) {
                    attributables.add(membership.getSyncopeRole());
                }
                break;
            case MEMBERSHIP:
                attributables.addAll(user.getMemberships());
                break;
            default:
        }

        final Entry<AbstractSchema, List<AbstractAttrValue>> entry =
                SchemaMappingUtil.getIntValues(mapping, attributables, password, schemaDAO);

        final List<AbstractAttrValue> values = entry.getValue();
        final AbstractSchema schema = entry.getKey();
        final SchemaType schemaType = schema == null ? SchemaType.String : schema.getType();

        final String extAttrName = SchemaMappingUtil.getExtAttrName(mapping);

        LOG.debug("Define mapping for: "
                + "\n* ExtAttrName " + extAttrName
                + "\n* is accountId " + mapping.isAccountid()
                + "\n* is password " + (mapping.isPassword() || mapping.getIntMappingType().equals(
                IntMappingType.Password))
                + "\n* mandatory condition " + mapping.getMandatoryCondition()
                + "\n* Schema " + mapping.getIntAttrName()
                + "\n* IntMappingType " + mapping.getIntMappingType().toString()
                + "\n* ClassType " + schemaType.getClassName()
                + "\n* Values " + values);

        List<Object> objValues = new ArrayList<Object>();

        for (AbstractAttrValue value : values) {
            if (FrameworkUtil.isSupportedAttributeType(Class.forName(schemaType.getClassName()))) {
                objValues.add(value.getValue());
            } else {
                objValues.add(value.getValueAsString());
            }
        }

        Map.Entry<String, Attribute> res;

        if (mapping.isAccountid()) {
            res = new DefaultMapEntry(objValues.iterator().next().toString(), null);
        } else if (mapping.isPassword()) {
            res = new DefaultMapEntry(null,
                    AttributeBuilder.buildPassword(objValues.iterator().next().toString().toCharArray()));
        } else {
            if (schema != null && schema.isMultivalue()) {
                res = new DefaultMapEntry(null, AttributeBuilder.build(extAttrName, objValues));
            } else {
                res = new DefaultMapEntry(null, objValues.isEmpty()
                        ? AttributeBuilder.build(extAttrName)
                        : AttributeBuilder.build(extAttrName, objValues.iterator().next()));
            }
        }

        return res;
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param user given user
     * @param password clear-text password
     * @param enable whether user must be enabled or not
     * @param resource target resource
     * @return account link + prepared attributes
     */
    private Map.Entry<String, Set<Attribute>> prepareAttributes(final SyncopeUser user, final String password,
            final Boolean enable, final ExternalResource resource) {

        LOG.debug("Preparing resource attributes for {} on resource {} with attributes {}",
                new Object[]{user, resource, user.getAttributes()});

        Set<Attribute> attributes = new HashSet<Attribute>();
        String accountId = null;

        for (SchemaMapping mapping : resource.getMappings()) {
            LOG.debug("Processing schema {}", SchemaMappingUtil.getIntAttrName(mapping));

            try {
                Map.Entry<String, Attribute> preparedAttribute = prepareAttribute(mapping, user, password);

                if (preparedAttribute.getKey() != null) {
                    accountId = preparedAttribute.getKey();
                }

                if (preparedAttribute.getValue() != null) {
                    final Attribute alreadyAdded = AttributeUtil.find(preparedAttribute.getValue().getName(),
                            attributes);

                    if (alreadyAdded == null) {
                        attributes.add(preparedAttribute.getValue());
                    } else {
                        attributes.remove(alreadyAdded);

                        Set values = new HashSet(alreadyAdded.getValue());
                        values.addAll(preparedAttribute.getValue().getValue());

                        attributes.add(AttributeBuilder.build(preparedAttribute.getValue().getName(), values));
                    }

                }
            } catch (Exception e) {
                LOG.debug("Attribute '{}' processing failed", SchemaMappingUtil.getIntAttrName(mapping), e);
            }
        }

        if (StringUtils.isNotBlank(accountId)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing accountId for '{}': ", resource.getName());
        }

        // Evaluate AccountLink expression
        String evalAccountLink = null;
        if (StringUtils.isNotBlank(resource.getAccountLink())) {
            final JexlContext jexlContext = new MapContext();
            jexlUtil.addFieldsToContext(user, jexlContext);
            jexlUtil.addAttrsToContext(user.getAttributes(), jexlContext);
            jexlUtil.addDerAttrsToContext(user.getDerivedAttributes(), user.getAttributes(), jexlContext);
            evalAccountLink = jexlUtil.evaluate(resource.getAccountLink(), jexlContext);
        }

        // If AccountLink evaluates to an empty string, just use the provided AccountId as Name(),
        // otherwise evaluated AccountLink expression is taken as Name().
        if (StringUtils.isBlank(evalAccountLink)) {
            // add AccountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", accountId);
            attributes.add(new Name(accountId));
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evalAccountLink);
            attributes.add(new Name(evalAccountLink));

            // AccountId not propagated: it will be used to set the value for __UID__ attribute
            LOG.debug("AccountId will be used just as __UID__ attribute");
        }

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }

        return new DefaultMapEntry(accountId, attributes);
    }

    /**
     * Create propagation tasks.
     *
     * @param user user to be provisioned
     * @param password cleartext password to be provisioned
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether user must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     */
    protected List<PropagationTask> createTasks(final SyncopeUser user, final String password, final Boolean enable,
            final boolean deleteOnResource, final PropagationByResource propByRes) {

        LOG.debug("Provisioning with user {}:\n{}", user, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        final List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        for (PropagationOperation operation : PropagationOperation.values()) {
            for (String resourceName : propByRes.get(operation)) {
                final ExternalResource resource = resourceDAO.find(resourceName);
                if (resource == null) {
                    LOG.error("Invalid resource name specified: {}, ignoring...", resourceName);
                } else {
                    PropagationTask task = new PropagationTask();
                    task.setResource(resource);
                    if (!deleteOnResource) {
                        task.setSyncopeUser(user);
                    }
                    task.setPropagationOperation(operation);
                    task.setPropagationMode(resource.getPropagationMode());
                    task.setOldAccountId(propByRes.getOldAccountId(resource.getName()));

                    Map.Entry<String, Set<Attribute>> preparedAttrs =
                            prepareAttributes(user, password, enable, resource);
                    task.setAccountId(preparedAttrs.getKey());
                    task.setAttributes(preparedAttrs.getValue());

                    tasks.add(task);

                    LOG.debug("PropagationTasl created: {}", task);
                }
            }
        }

        return tasks;
    }
}
