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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.AbstractPatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Manage the data propagation to external resources.
 */
@Transactional(rollbackFor = { Throwable.class })
public class DefaultPropagationManager implements PropagationManager {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    protected final VirSchemaDAO virSchemaDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    protected final ConnObjectUtils connObjectUtils;

    protected final MappingManager mappingManager;

    protected final DerAttrHandler derAttrHandler;

    protected final AnyUtilsFactory anyUtilsFactory;

    public DefaultPropagationManager(
            final VirSchemaDAO virSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory,
            final ConnObjectUtils connObjectUtils,
            final MappingManager mappingManager,
            final DerAttrHandler derAttrHandler,
            final AnyUtilsFactory anyUtilsFactory) {

        this.virSchemaDAO = virSchemaDAO;
        this.resourceDAO = resourceDAO;
        this.entityFactory = entityFactory;
        this.connObjectUtils = connObjectUtils;
        this.mappingManager = mappingManager;
        this.derAttrHandler = derAttrHandler;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    @Override
    public List<PropagationTaskInfo> getCreateTasks(
            final AnyTypeKind kind,
            final String key,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final Collection<Attr> vAttrs,
            final Collection<String> noPropResourceKeys) {

        return getCreateTasks(
                anyUtilsFactory.getInstance(kind).dao().authFind(key),
                null,
                enable,
                propByRes,
                null,
                vAttrs,
                noPropResourceKeys);
    }

    @Override
    public List<PropagationTaskInfo> getUserCreateTasks(
            final String key,
            final String password,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<Attr> vAttrs,
            final Collection<String> noPropResourceKeys) {

        return getCreateTasks(
                anyUtilsFactory.getInstance(AnyTypeKind.USER).dao().authFind(key),
                password,
                enable,
                propByRes,
                propByLinkedAccount,
                vAttrs,
                noPropResourceKeys);
    }

    protected List<PropagationTaskInfo> getCreateTasks(
            final Any<?> any,
            final String password,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<Attr> vAttrs,
            final Collection<String> noPropResourceKeys) {

        if ((propByRes == null || propByRes.isEmpty())
                && (propByLinkedAccount == null || propByLinkedAccount.isEmpty())) {

            return List.of();
        }

        if (noPropResourceKeys != null) {
            if (propByRes != null) {
                propByRes.get(ResourceOperation.CREATE).removeAll(noPropResourceKeys);
            }

            if (propByLinkedAccount != null) {
                propByLinkedAccount.get(ResourceOperation.CREATE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
            }
        }

        return createTasks(any, password, true, enable, propByRes, propByLinkedAccount, vAttrs);
    }

    @Override
    public List<PropagationTaskInfo> getUpdateTasks(
            final AnyTypeKind kind,
            final String key,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<Attr> vAttrs,
            final Collection<String> noPropResourceKeys) {

        return getUpdateTasks(
                anyUtilsFactory.getInstance(kind).dao().authFind(key),
                null,
                changePwd,
                enable,
                propByRes,
                propByLinkedAccount,
                vAttrs,
                noPropResourceKeys);
    }

    @Override
    public List<PropagationTaskInfo> getUserUpdateTasks(
            final UserWorkflowResult<Pair<UserUR, Boolean>> wfResult,
            final boolean changePwd,
            final Collection<String> noPropResourceKeys) {

        return getUpdateTasks(
                anyUtilsFactory.getInstance(AnyTypeKind.USER).dao().authFind(wfResult.getResult().getLeft().getKey()),
                wfResult.getResult().getLeft().getPassword() == null
                ? null
                : wfResult.getResult().getLeft().getPassword().getValue(),
                changePwd,
                wfResult.getResult().getRight(),
                wfResult.getPropByRes(),
                wfResult.getPropByLinkedAccount(),
                wfResult.getResult().getLeft().getVirAttrs(),
                noPropResourceKeys);
    }

    @Override
    public List<PropagationTaskInfo> getUserUpdateTasks(final UserWorkflowResult<Pair<UserUR, Boolean>> wfResult) {
        UserUR userUR = wfResult.getResult().getLeft();

        // Propagate password update only to requested resources
        List<PropagationTaskInfo> tasks;
        if (userUR.getPassword() == null) {
            // a. no specific password propagation request: generate propagation tasks for any resource associated
            tasks = getUserUpdateTasks(wfResult, false, null);
        } else {
            tasks = new ArrayList<>();

            // b. generate the propagation task list in two phases: first the ones containing password,
            // then the rest (with no password)
            UserWorkflowResult<Pair<UserUR, Boolean>> pwdWFResult = new UserWorkflowResult<>(
                    wfResult.getResult(),
                    new PropagationByResource<>(),
                    wfResult.getPropByLinkedAccount(),
                    wfResult.getPerformedTasks());

            Set<String> pwdResourceNames = new HashSet<>(userUR.getPassword().getResources());
            Collection<String> allResourceNames = anyUtilsFactory.getInstance(AnyTypeKind.USER).
                    dao().findAllResourceKeys(userUR.getKey());
            pwdResourceNames.retainAll(allResourceNames);

            pwdWFResult.getPropByRes().addAll(ResourceOperation.UPDATE, pwdResourceNames);
            if (!pwdWFResult.getPropByRes().isEmpty()) {
                Set<String> toBeExcluded = new HashSet<>(allResourceNames);
                toBeExcluded.addAll(userUR.getResources().stream().
                        map(AbstractPatchItem::getValue).collect(Collectors.toList()));
                toBeExcluded.removeAll(pwdResourceNames);

                tasks.addAll(getUserUpdateTasks(pwdWFResult, true, toBeExcluded));
            }

            UserWorkflowResult<Pair<UserUR, Boolean>> noPwdWFResult = new UserWorkflowResult<>(
                    wfResult.getResult(),
                    new PropagationByResource<>(),
                    new PropagationByResource<>(),
                    wfResult.getPerformedTasks());

            noPwdWFResult.getPropByRes().merge(wfResult.getPropByRes());
            noPwdWFResult.getPropByRes().removeAll(pwdResourceNames);
            noPwdWFResult.getPropByRes().purge();
            if (!noPwdWFResult.getPropByRes().isEmpty()) {
                tasks.addAll(getUserUpdateTasks(noPwdWFResult, false, pwdResourceNames));
            }

            tasks = tasks.stream().distinct().collect(Collectors.toList());
        }

        return tasks;
    }

    protected List<PropagationTaskInfo> getUpdateTasks(
            final Any<?> any,
            final String password,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<Attr> vAttrs,
            final Collection<String> noPropResourceKeys) {

        if (noPropResourceKeys != null) {
            if (propByRes != null) {
                propByRes.removeAll(noPropResourceKeys);
            }

            if (propByLinkedAccount != null) {
                propByLinkedAccount.get(ResourceOperation.CREATE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.UPDATE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.DELETE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
            }
        }

        return createTasks(
                any,
                password,
                changePwd,
                enable,
                Optional.ofNullable(propByRes).orElseGet(PropagationByResource::new),
                propByLinkedAccount,
                vAttrs);
    }

    @Override
    public List<PropagationTaskInfo> getDeleteTasks(
            final AnyTypeKind kind,
            final String key,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> noPropResourceKeys) {

        return getDeleteTasks(
                anyUtilsFactory.getInstance(kind).dao().authFind(key),
                propByRes, propByLinkedAccount, noPropResourceKeys);
    }

    protected List<PropagationTaskInfo> getDeleteTasks(
            final Any<?> any,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> noPropResourceKeys) {

        PropagationByResource<String> localPropByRes = new PropagationByResource<>();

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(
                    ResourceOperation.DELETE,
                    anyUtilsFactory.getInstance(any).dao().findAllResourceKeys(any.getKey()));
        } else {
            localPropByRes.merge(propByRes);
        }

        if (noPropResourceKeys != null) {
            localPropByRes.removeAll(noPropResourceKeys);

            if (propByLinkedAccount != null) {
                propByLinkedAccount.get(ResourceOperation.CREATE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.UPDATE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.DELETE).
                        removeIf(account -> noPropResourceKeys.contains(account.getLeft()));
            }
        }

        return createTasks(any, null, false, false, localPropByRes, propByLinkedAccount, null);
    }

    @Override
    public PropagationTaskInfo newTask(
            final DerAttrHandler derAttrHandler,
            final Any<?> any,
            final ExternalResource resource,
            final ResourceOperation operation,
            final Provision provision,
            final Stream<? extends Item> mappingItems,
            final Pair<String, Set<Attribute>> preparedAttrs) {

        PropagationTaskInfo task = new PropagationTaskInfo(resource);
        task.setObjectClassName(provision.getObjectClass().getObjectClassValue());
        task.setAnyTypeKind(any.getType().getKind());
        task.setAnyType(any.getType().getKey());
        task.setEntityKey(any.getKey());
        task.setOperation(operation);
        task.setConnObjectKey(preparedAttrs.getLeft());

        // Check if any of mandatory attributes (in the mapping) is missing or not received any value: 
        // if so, add special attributes that will be evaluated by PropagationTaskExecutor
        List<String> mandatoryMissing = new ArrayList<>();
        List<String> mandatoryNullOrEmpty = new ArrayList<>();
        mappingItems.filter(item -> (!item.isConnObjectKey()
                && JexlUtils.evaluateMandatoryCondition(item.getMandatoryCondition(), any, derAttrHandler))).
                forEach(item -> {

                    Attribute attr = AttributeUtil.find(item.getExtAttrName(), preparedAttrs.getRight());
                    if (attr == null) {
                        mandatoryMissing.add(item.getExtAttrName());
                    } else if (CollectionUtils.isEmpty(attr.getValue())) {
                        mandatoryNullOrEmpty.add(item.getExtAttrName());
                    }
                });
        if (!mandatoryMissing.isEmpty()) {
            preparedAttrs.getRight().add(AttributeBuilder.build(
                    PropagationTaskExecutor.MANDATORY_MISSING_ATTR_NAME, mandatoryMissing));
        }
        if (!mandatoryNullOrEmpty.isEmpty()) {
            preparedAttrs.getRight().add(AttributeBuilder.build(
                    PropagationTaskExecutor.MANDATORY_NULL_OR_EMPTY_ATTR_NAME, mandatoryNullOrEmpty));
        }

        task.setAttributes(POJOHelper.serialize(preparedAttrs.getRight()));

        return task;
    }

    /**
     * Create propagation tasks.
     *
     * @param any to be provisioned
     * @param password clear text password to be provisioned
     * @param changePwd whether password should be included for propagation attributes or not
     * @param enable whether user must be enabled or not
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed on linked accounts
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks created
     */
    protected List<PropagationTaskInfo> createTasks(
            final Any<?> any,
            final String password,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<Attr> vAttrs) {

        LOG.debug("Provisioning {}:\n{}", any, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge {}:\n{}", any, propByRes);

        // Virtual attributes
        Set<String> virtualResources = new HashSet<>();
        virtualResources.addAll(propByRes.get(ResourceOperation.CREATE));
        virtualResources.addAll(propByRes.get(ResourceOperation.UPDATE));
        virtualResources.addAll(anyUtilsFactory.getInstance(any).dao().findAllResourceKeys(any.getKey()));

        Map<String, Set<Attribute>> vAttrMap = new HashMap<>();
        if (vAttrs != null) {
            vAttrs.forEach(vAttr -> {
                VirSchema schema = virSchemaDAO.find(vAttr.getSchema());
                if (schema == null) {
                    LOG.warn("Ignoring invalid {} {}", VirSchema.class.getSimpleName(), vAttr.getSchema());
                } else if (schema.isReadonly()) {
                    LOG.warn("Ignoring read-only {} {}", VirSchema.class.getSimpleName(), vAttr.getSchema());
                } else if (anyUtilsFactory.getInstance(any).dao().
                        findAllowedSchemas(any, VirSchema.class).contains(schema)
                        && virtualResources.contains(schema.getProvision().getResource().getKey())) {

                    Set<Attribute> values = vAttrMap.get(schema.getProvision().getResource().getKey());
                    if (values == null) {
                        values = new HashSet<>();
                        vAttrMap.put(schema.getProvision().getResource().getKey(), values);
                    }
                    values.add(AttributeBuilder.build(schema.getExtAttrName(), vAttr.getValues()));

                    if (!propByRes.contains(ResourceOperation.CREATE, schema.getProvision().getResource().getKey())) {
                        propByRes.add(ResourceOperation.UPDATE, schema.getProvision().getResource().getKey());
                    }
                } else {
                    LOG.warn("{} not owned by or {} not allowed for {}",
                            schema.getProvision().getResource(), schema, any);
                }
            });
        }
        LOG.debug("With virtual attributes {}:\n{}\n{}", any, propByRes, vAttrMap);

        List<PropagationTaskInfo> tasks = new ArrayList<>();

        propByRes.asMap().forEach((resourceKey, operation) -> {
            ExternalResource resource = resourceDAO.find(resourceKey);
            Provision provision = Optional.ofNullable(resource).
                    flatMap(externalResource -> externalResource.getProvision(any.getType())).orElse(null);
            Stream<? extends Item> mappingItems = provision == null
                    ? Stream.empty()
                    : MappingUtils.getPropagationItems(provision.getMapping().getItems().stream());

            if (resource == null) {
                LOG.error("Invalid resource name specified: {}, ignoring...", resourceKey);
            } else if (provision == null) {
                LOG.error("No provision specified on resource {} for type {}, ignoring...",
                        resource, any.getType());
            } else if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                        any.getType(), resource);
            } else {
                Pair<String, Set<Attribute>> preparedAttrs =
                        mappingManager.prepareAttrsFromAny(any, password, changePwd, enable, provision);
                if (vAttrMap.containsKey(resourceKey)) {
                    preparedAttrs.getRight().addAll(vAttrMap.get(resourceKey));
                }

                PropagationTaskInfo task = newTask(
                        derAttrHandler,
                        any,
                        resource,
                        operation,
                        provision,
                        mappingItems,
                        preparedAttrs);
                task.setOldConnObjectKey(propByRes.getOldConnObjectKey(resourceKey));
                tasks.add(task);
                LOG.debug("PropagationTask created: {}", task);
            }
        });

        if (any instanceof User && propByLinkedAccount != null) {
            User user = (User) any;
            propByLinkedAccount.asMap().forEach((accountInfo, operation) -> {
                LinkedAccount account = user.getLinkedAccount(accountInfo.getLeft(), accountInfo.getRight()).
                        orElse(null);
                if (account == null && operation == ResourceOperation.DELETE) {
                    account = new DeletingLinkedAccount(
                            user, resourceDAO.find(accountInfo.getLeft()), accountInfo.getRight());
                }

                Provision provision = account == null || account.getResource() == null
                        ? null
                        : account.getResource().getProvision(AnyTypeKind.USER.name()).orElse(null);
                Stream<? extends Item> mappingItems = provision == null
                        ? Stream.empty()
                        : MappingUtils.getPropagationItems(provision.getMapping().getItems().stream());

                if (account == null) {
                    LOG.error("Invalid operation {} on deleted account {} on resource {}, ignoring...",
                            operation, accountInfo.getRight(), accountInfo.getLeft());
                } else if (account.getResource() == null) {
                    LOG.error("Invalid resource name specified: {}, ignoring...", accountInfo.getLeft());
                } else if (provision == null) {
                    LOG.error("No provision specified on resource {} for type {}, ignoring...",
                            account.getResource(), AnyTypeKind.USER.name());
                } else if (provision.getMapping() == null || provision.getMapping().getItems().isEmpty()) {
                    LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                            AnyTypeKind.USER.name(), account.getResource());
                } else {
                    PropagationTaskInfo accountTask = newTask(
                            derAttrHandler,
                            user,
                            account.getResource(),
                            operation,
                            provision,
                            mappingItems,
                            Pair.of(account.getConnObjectKeyValue(),
                                    mappingManager.prepareAttrsFromLinkedAccount(
                                            user, account, password, true, provision)));
                    tasks.add(accountTask);

                    LOG.debug("PropagationTask created for Linked Account {}: {}",
                            account.getConnObjectKeyValue(), accountTask);
                }
            });
        }

        return tasks;
    }

    @Override
    public List<PropagationTaskInfo> createTasks(
            final Realm realm,
            final PropagationByResource<String> propByRes,
            final Collection<String> noPropResourceKeys) {

        if (noPropResourceKeys != null) {
            propByRes.removeAll(noPropResourceKeys);
        }

        LOG.debug("Provisioning {}:\n{}", realm, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge {}:\n{}", realm, propByRes);

        List<PropagationTaskInfo> tasks = new ArrayList<>();

        propByRes.asMap().forEach((resourceKey, operation) -> {
            ExternalResource resource = resourceDAO.find(resourceKey);
            OrgUnit orgUnit = Optional.ofNullable(resource).map(ExternalResource::getOrgUnit).orElse(null);

            if (resource == null) {
                LOG.error("Invalid resource name specified: {}, ignoring...", resourceKey);
            } else if (orgUnit == null) {
                LOG.error("No orgUnit specified on resource {}, ignoring...", resource);
            } else if (StringUtils.isBlank(orgUnit.getConnObjectLink())) {
                LOG.warn("Requesting propagation for {} but no ConnObjectLink provided for {}",
                        realm.getFullPath(), resource);
            } else {
                PropagationTaskInfo task = new PropagationTaskInfo(resource);
                task.setObjectClassName(orgUnit.getObjectClass().getObjectClassValue());
                task.setEntityKey(realm.getKey());
                task.setOperation(operation);
                task.setOldConnObjectKey(propByRes.getOldConnObjectKey(resource.getKey()));

                Pair<String, Set<Attribute>> preparedAttrs = mappingManager.prepareAttrsFromRealm(realm, orgUnit);
                task.setConnObjectKey(preparedAttrs.getLeft());
                task.setAttributes(POJOHelper.serialize(preparedAttrs.getRight()));

                tasks.add(task);

                LOG.debug("PropagationTask created: {}", task);
            }
        });

        return tasks;
    }
}
