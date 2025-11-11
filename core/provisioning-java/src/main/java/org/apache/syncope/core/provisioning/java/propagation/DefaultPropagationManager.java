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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Manage the data propagation to external resources.
 */
@Transactional(rollbackFor = { Throwable.class })
public class DefaultPropagationManager implements PropagationManager {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    protected final ConnObjectUtils connObjectUtils;

    protected final MappingManager mappingManager;

    protected final DerAttrHandler derAttrHandler;

    protected final AnyUtilsFactory anyUtilsFactory;

    public DefaultPropagationManager(
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory,
            final ConnObjectUtils connObjectUtils,
            final MappingManager mappingManager,
            final DerAttrHandler derAttrHandler,
            final AnyUtilsFactory anyUtilsFactory) {

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
            final Collection<String> excludedResources) {

        return getCreateTasks(
                anyUtilsFactory.getInstance(kind).dao().authFind(key),
                null,
                enable,
                propByRes,
                null,
                excludedResources);
    }

    @Override
    public List<PropagationTaskInfo> getUserCreateTasks(
            final String key,
            final String password,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> excludedResources) {

        return getCreateTasks(
                anyUtilsFactory.getInstance(AnyTypeKind.USER).dao().authFind(key),
                password,
                enable,
                propByRes,
                propByLinkedAccount,
                excludedResources);
    }

    protected List<PropagationTaskInfo> getCreateTasks(
            final Any any,
            final String password,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> excludedResources) {

        if ((propByRes == null || propByRes.isEmpty())
                && (propByLinkedAccount == null || propByLinkedAccount.isEmpty())) {

            return List.of();
        }

        List<String> changePwdRes = new ArrayList<>();
        if (excludedResources != null) {
            if (propByRes != null) {
                propByRes.get(ResourceOperation.CREATE).removeAll(excludedResources);
            }

            if (propByLinkedAccount != null) {
                propByLinkedAccount.get(ResourceOperation.CREATE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
            }
        }

        if (propByRes != null) {
            propByRes.asMap().forEach((resource, resourceOperation) -> changePwdRes.add(resource));
        }
        if (propByLinkedAccount != null) {
            propByLinkedAccount.asMap().forEach((resource, resourceOperation) -> changePwdRes.add(resource.getKey()));
        }
        return createTasks(any, password, changePwdRes, enable, propByRes, propByLinkedAccount);
    }

    @Override
    public List<PropagationTaskInfo> getUpdateTasks(
            final AnyUR anyUR,
            final AnyTypeKind kind,
            final String key,
            final List<String> changePwdRes,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> excludedResources) {

        return getUpdateTasks(
                anyUR,
                anyUtilsFactory.getInstance(kind).dao().authFind(key),
                null,
                changePwdRes,
                enable,
                propByRes,
                propByLinkedAccount,
                excludedResources);
    }

    @Override
    public List<PropagationTaskInfo> getUserUpdateTasks(
            final UserWorkflowResult<Pair<UserUR, Boolean>> wfResult,
            final List<String> changePwdRes,
            final Collection<String> excludedResources) {

        return getUpdateTasks(
                wfResult.getResult().getLeft(),
                anyUtilsFactory.getInstance(AnyTypeKind.USER).dao().authFind(wfResult.getResult().getLeft().getKey()),
                Optional.ofNullable(wfResult.getResult().getLeft().getPassword()).
                        map(PasswordPatch::getValue).orElse(null),
                changePwdRes,
                wfResult.getResult().getRight(),
                wfResult.getPropByRes(),
                wfResult.getPropByLinkedAccount(),
                excludedResources);
    }

    @Override
    public List<PropagationTaskInfo> getUserUpdateTasks(final UserWorkflowResult<Pair<UserUR, Boolean>> wfResult) {
        UserUR userUR = wfResult.getResult().getLeft();

        List<String> urPwdResources = userUR.getPassword() == null
                ? List.of()
                : userUR.getPassword().getResources().stream().distinct().toList();

        List<String> laPwdResources = userUR.getLinkedAccounts().stream().
                filter(laur -> laur.getOperation() == PatchOperation.ADD_REPLACE).
                map(LinkedAccountUR::getLinkedAccountTO).
                filter(la -> la != null && la.getPassword() != null).
                map(LinkedAccountTO::getResource).
                distinct().toList();

        List<String> pwdResources = Stream.concat(urPwdResources.stream(), laPwdResources.stream()).
                distinct().toList();

        // Propagate password update only to requested resources
        List<PropagationTaskInfo> tasks;
        if (pwdResources.isEmpty()) {
            // a. no specific password propagation request: generate propagation tasks for any resource associated
            tasks = getUserUpdateTasks(wfResult, List.of(), null);
        } else {
            // b. generate the propagation task list in two phases: first the ones with no password, then the others
            tasks = new ArrayList<>();

            PropagationByResource<String> urNoPwdPropByRes = new PropagationByResource<>();
            urNoPwdPropByRes.merge(wfResult.getPropByRes());
            urNoPwdPropByRes.purge();
            urNoPwdPropByRes.removeAll(urPwdResources);

            PropagationByResource<Pair<String, String>> laNoPwdPropByRes = new PropagationByResource<>();
            laNoPwdPropByRes.merge(wfResult.getPropByLinkedAccount());
            laNoPwdPropByRes.purge();
            laNoPwdPropByRes.removeIf(p -> laPwdResources.contains(p.getLeft()));

            if (!urNoPwdPropByRes.isEmpty() || !laNoPwdPropByRes.isEmpty()) {
                UserWorkflowResult<Pair<UserUR, Boolean>> noPwdWFResult = new UserWorkflowResult<>(
                        wfResult.getResult(),
                        urNoPwdPropByRes,
                        laNoPwdPropByRes,
                        wfResult.getPerformedTasks());

                tasks.addAll(getUserUpdateTasks(noPwdWFResult, List.of(), null));
            }

            PropagationByResource<String> urPwdPropByRes = new PropagationByResource<>();
            urPwdPropByRes.merge(wfResult.getPropByRes());
            urPwdPropByRes.purge();
            urPwdPropByRes.retainAll(urPwdResources);

            PropagationByResource<Pair<String, String>> laPwdPropByRes = new PropagationByResource<>();
            laPwdPropByRes.merge(wfResult.getPropByLinkedAccount());
            laPwdPropByRes.purge();
            laPwdPropByRes.removeIf(p -> !laPwdResources.contains(p.getLeft()));

            if (!urPwdPropByRes.isEmpty() || !laPwdPropByRes.isEmpty()) {
                UserWorkflowResult<Pair<UserUR, Boolean>> pwdWFResult = new UserWorkflowResult<>(
                        wfResult.getResult(),
                        urPwdPropByRes,
                        laPwdPropByRes,
                        wfResult.getPerformedTasks());

                tasks.addAll(getUserUpdateTasks(pwdWFResult, pwdResources, null));
            }
        }

        return tasks;
    }

    protected List<PropagationTaskInfo> getUpdateTasks(
            final AnyUR anyUR,
            final Any any,
            final String password,
            final List<String> changePwdRes,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> excludedResources) {

        if (excludedResources != null) {
            if (propByRes != null) {
                propByRes.removeAll(excludedResources);
            }

            if (propByLinkedAccount != null) {
                propByLinkedAccount.get(ResourceOperation.CREATE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.UPDATE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.DELETE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
            }
        }

        List<PropagationTaskInfo> tasks = createTasks(
                any,
                password,
                changePwdRes,
                enable,
                Optional.ofNullable(propByRes).orElseGet(PropagationByResource::new),
                propByLinkedAccount);
        tasks.forEach(task -> task.setUpdateRequest(anyUR));
        return tasks;
    }

    @Override
    public List<PropagationTaskInfo> getDeleteTasks(
            final AnyTypeKind kind,
            final String key,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> excludedResources) {

        return getDeleteTasks(
                anyUtilsFactory.getInstance(kind).dao().authFind(key),
                propByRes, propByLinkedAccount, excludedResources);
    }

    protected List<PropagationTaskInfo> getDeleteTasks(
            final Any any,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Collection<String> excludedResources) {

        PropagationByResource<String> localPropByRes = new PropagationByResource<>();

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(
                    ResourceOperation.DELETE,
                    anyUtilsFactory.getInstance(any).dao().findAllResourceKeys(any.getKey()));
        } else {
            localPropByRes.merge(propByRes);
        }

        if (excludedResources != null) {
            localPropByRes.removeAll(excludedResources);

            if (propByLinkedAccount != null) {
                propByLinkedAccount.get(ResourceOperation.CREATE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.UPDATE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
                propByLinkedAccount.get(ResourceOperation.DELETE).
                        removeIf(account -> excludedResources.contains(account.getLeft()));
            }
        }

        return createTasks(any, null, List.of(), false, localPropByRes, propByLinkedAccount);
    }

    @Override
    public PropagationTaskInfo newTask(
            final DerAttrHandler derAttrHandler,
            final Any any,
            final ExternalResource resource,
            final ResourceOperation operation,
            final Provision provision,
            final Stream<Item> mappingItems,
            final MappingManager.PreparedAttrs preparedAttrs) {

        // Check if any of mandatory attributes (in the mapping) is missing or not received any value: 
        // if so, add special attributes that will be evaluated by PropagationTaskExecutor
        List<String> mandatoryMissing = new ArrayList<>();
        List<String> mandatoryNullOrEmpty = new ArrayList<>();
        mappingItems.filter(item -> !item.isConnObjectKey()
                && JexlUtils.evaluateMandatoryCondition(item.getMandatoryCondition(), any, derAttrHandler)).
                forEach(item -> {

                    Attribute attr = AttributeUtil.find(item.getExtAttrName(), preparedAttrs.attributes());
                    if (attr == null) {
                        mandatoryMissing.add(item.getExtAttrName());
                    } else if (CollectionUtils.isEmpty(attr.getValue())) {
                        mandatoryNullOrEmpty.add(item.getExtAttrName());
                    }
                });
        if (!mandatoryMissing.isEmpty()) {
            preparedAttrs.attributes().add(AttributeBuilder.build(
                    MANDATORY_MISSING_ATTR_NAME, mandatoryMissing));
        }
        if (!mandatoryNullOrEmpty.isEmpty()) {
            preparedAttrs.attributes().add(AttributeBuilder.build(
                    MANDATORY_NULL_OR_EMPTY_ATTR_NAME, mandatoryNullOrEmpty));
        }

        return new PropagationTaskInfo(
                resource,
                operation,
                new ObjectClass(provision.getObjectClass()),
                any.getType().getKind(),
                any.getType().getKey(),
                any.getKey(),
                preparedAttrs.connObjectLink(),
                new PropagationData(preparedAttrs.attributes()));
    }

    /**
     * Create propagation tasks.
     *
     * @param any to be provisioned
     * @param password clear text password to be provisioned
     * @param changePwdRes the resources in which the password must be included in the propagation attributes
     * @param enable whether user must be enabled or not
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed on linked accounts
     * @return list of propagation tasks created
     */
    protected List<PropagationTaskInfo> createTasks(
            final Any any,
            final String password,
            final List<String> changePwdRes,
            final Boolean enable,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount) {

        LOG.debug("Provisioning {}:\n{}", any, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge {}:\n{}", any, propByRes);

        List<PropagationTaskInfo> tasks = new ArrayList<>();

        propByRes.asMap().forEach((resourceKey, operation) -> {
            ExternalResource resource = resourceDAO.findById(resourceKey).orElse(null);
            Provision provision = Optional.ofNullable(resource).
                    flatMap(r -> r.getProvisionByAnyType(any.getType().getKey())).orElse(null);
            Stream<Item> mappingItems = provision == null
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
                MappingManager.PreparedAttrs preparedAttrs =
                        mappingManager.prepareAttrsFromAny(any, password, changePwdRes.contains(resourceKey),
                                enable, resource, provision);

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

        if (any instanceof final User user && propByLinkedAccount != null) {
            propByLinkedAccount.asMap().forEach((accountInfo, operation) -> {
                LinkedAccount account = user.getLinkedAccount(accountInfo.getLeft(), accountInfo.getRight()).
                        orElse(null);
                if (account == null && operation == ResourceOperation.DELETE) {
                    account = new DeletingLinkedAccount(
                            user,
                            resourceDAO.findById(accountInfo.getLeft()).
                                    orElseThrow(() -> new NotFoundException("Resource " + accountInfo.getLeft())),
                            accountInfo.getRight());
                }

                Provision provision = account == null || account.getResource() == null
                        ? null
                        : account.getResource().getProvisionByAnyType(AnyTypeKind.USER.name()).orElse(null);
                Stream<Item> mappingItems = provision == null
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
                            new MappingManager.PreparedAttrs(account.getConnObjectKeyValue(),
                                    mappingManager.prepareAttrsFromLinkedAccount(
                                            user, account, password,
                                            changePwdRes.contains(account.getResource().getKey()),
                                            provision)));
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
            final Collection<String> excludedResources) {

        if (excludedResources != null) {
            propByRes.removeAll(excludedResources);
        }

        LOG.debug("Provisioning {}:\n{}", realm, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge {}:\n{}", realm, propByRes);

        List<PropagationTaskInfo> tasks = new ArrayList<>();

        propByRes.asMap().forEach((resourceKey, operation) -> {
            ExternalResource resource = resourceDAO.findById(resourceKey).
                    orElseThrow(() -> new NotFoundException("Resource " + resourceKey));
            OrgUnit orgUnit = Optional.ofNullable(resource).map(ExternalResource::getOrgUnit).orElse(null);

            if (resource == null) {
                LOG.error("Invalid resource name specified: {}, ignoring...", resourceKey);
            } else if (orgUnit == null) {
                LOG.error("No orgUnit specified on resource {}, ignoring...", resource);
            } else if (StringUtils.isBlank(orgUnit.getConnObjectLink())) {
                LOG.warn("Requesting propagation for {} but no ConnObjectLink provided for {}",
                        realm.getFullPath(), resource);
            } else {
                MappingManager.PreparedAttrs preparedAttrs = mappingManager.prepareAttrsFromRealm(realm, resource);

                PropagationTaskInfo task = new PropagationTaskInfo(
                        resource,
                        operation,
                        new ObjectClass(orgUnit.getObjectClass()),
                        null,
                        null,
                        realm.getKey(),
                        preparedAttrs.connObjectLink(),
                        new PropagationData(preparedAttrs.attributes()));
                task.setOldConnObjectKey(propByRes.getOldConnObjectKey(resource.getKey()));

                tasks.add(task);

                LOG.debug("PropagationTask created: {}", task);
            }
        });

        return tasks;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public Map<Pair<String, String>, Set<Attribute>> prepareAttrs(
            final AnyTypeKind kind,
            final String key,
            final String password,
            final List<String> changePwdRes,
            final Boolean enable,
            final Collection<String> excludedResources) {

        Map<Pair<String, String>, Set<Attribute>> attrs = new HashMap<>();

        Any any = anyUtilsFactory.getInstance(kind).dao().authFind(key);

        anyUtilsFactory.getInstance(kind).dao().findAllResourceKeys(key).stream().
                filter(Predicate.not(excludedResources::contains)).
                map(resourceDAO::findById).
                flatMap(Optional::stream).
                filter(resource -> Optional.ofNullable(resource.getPropagationPolicy()).
                map(PropagationPolicy::isUpdateDelta).orElse(false)).
                forEach(resource -> resource.getProvisionByAnyType(any.getType().getKey()).
                ifPresent(provision -> {
                    MappingManager.PreparedAttrs preparedAttrs = mappingManager.prepareAttrsFromAny(
                            any,
                            password,
                            changePwdRes.contains(resource.getKey()),
                            enable,
                            resource,
                            provision);
                    attrs.put(
                            Pair.of(resource.getKey(), preparedAttrs.connObjectLink()),
                            preparedAttrs.attributes());
                }));

        if (any instanceof User user) {
            user.getLinkedAccounts().stream().
                    filter(account -> !excludedResources.contains(account.getResource().getKey())
                    && account.getResource().getProvisionByAnyType(user.getType().getKey()).isPresent()
                    && Optional.ofNullable(account.getResource().getPropagationPolicy()).
                            map(PropagationPolicy::isUpdateDelta).orElse(false)).
                    forEach(account -> account.getResource().getProvisionByAnyType(user.getType().getKey()).
                    ifPresent(provision -> {
                        Set<Attribute> preparedAttrs = mappingManager.prepareAttrsFromLinkedAccount(
                                user,
                                account,
                                password,
                                true,
                                provision);
                        attrs.put(
                                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue()),
                                preparedAttrs);
                    }));
        }

        LOG.debug("Prepared attrs for {} {}: {}", kind, key, attrs);
        return attrs;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public Map<Pair<String, String>, Set<Attribute>> prepareAttrs(final Realm realm) {
        Map<Pair<String, String>, Set<Attribute>> attrs = new HashMap<>();

        realm.getResources().stream().
                filter(resource -> resource.getOrgUnit() != null
                && Optional.ofNullable(resource.getPropagationPolicy()).
                        map(PropagationPolicy::isUpdateDelta).orElse(false)).
                forEach(resource -> {
                    MappingManager.PreparedAttrs preparedAttrs = mappingManager.prepareAttrsFromRealm(
                            realm,
                            resource);
                    attrs.put(
                            Pair.of(resource.getKey(), preparedAttrs.connObjectLink()),
                            preparedAttrs.attributes());
                });

        LOG.debug("Prepared attrs for Realm {}: {}", realm.getKey(), attrs);
        return attrs;
    }

    /**
     * Checks whether the given attribute shall be treated as an ordinary attribute or not, for purpose of building
     * AttributeDelta instances.
     *
     * @param attr ConnId attribute
     * @return whether the condition is matched or not
     */
    protected boolean isOrdinaryForAttrForDelta(final Attribute attr) {
        return !attr.is(Name.NAME) && !OperationalAttributes.isOperationalAttribute(attr);
    }

    @Override
    public List<PropagationTaskInfo> setAttributeDeltas(
            final List<PropagationTaskInfo> tasks,
            final Map<Pair<String, String>, Set<Attribute>> beforeAttrs) {

        if (beforeAttrs.isEmpty()) {
            return tasks;
        }

        for (PropagationTaskInfo task : tasks) {
            // rename is not supported by updateDelta
            if (task.getOldConnObjectKey() != null && !task.getConnObjectKey().equals(task.getOldConnObjectKey())) {
                continue;
            }

            Pair<String, String> key = Pair.of(task.getResource().getKey(), task.getConnObjectKey());
            if (!beforeAttrs.containsKey(key)) {
                continue;
            }

            Set<Attribute> attrs = new HashSet<>(beforeAttrs.get(key));

            // purge unwanted attributes, even though prepared
            attrs.removeIf(
                    attr -> MANDATORY_MISSING_ATTR_NAME.equals(attr.getName())
                    || MANDATORY_NULL_OR_EMPTY_ATTR_NAME.equals(attr.getName()));

            PropagationData propagationData = task.getPropagationData();

            Set<AttributeDelta> attributeDeltas = new HashSet<>();

            // build delta for updated attributes
            propagationData.getAttributes().forEach(next -> {
                Set<Object> valuesToAdd = new HashSet<>();
                Set<Object> valuesToRemove = new HashSet<>();

                Optional.ofNullable(AttributeUtil.find(next.getName(), attrs)).ifPresent(prev -> {
                    // password is unchanged from beforeAttrs but needs to be taken into account anyway
                    if (next.is(OperationalAttributes.PASSWORD_NAME)) {
                        valuesToAdd.addAll(next.getValue());
                    } else {
                        if (next.getValue() == null && prev.getValue() != null) {
                            valuesToRemove.addAll(prev.getValue());
                        } else if (next.getValue() != null && prev.getValue() == null) {
                            valuesToAdd.addAll(next.getValue());
                        } else if (next.getValue() != null && prev.getValue() != null) {
                            next.getValue().stream().
                                    filter(value -> !prev.getValue().contains(value)).
                                    forEach(valuesToAdd::add);

                            prev.getValue().stream().
                                    filter(value -> !next.getValue().contains(value)).
                                    forEach(valuesToRemove::add);
                        }
                    }
                });

                // Following org.identityconnectors.framework.impl.api.local.operations.UpdateDeltaImpl#updateDelta
                // we create AttributeDelta instances with (valuesToAdd, valuesToRemove) or (valuesToReplace)
                // depending on the attribute name
                if (isOrdinaryForAttrForDelta(next)) {
                    if (!valuesToAdd.isEmpty() || !valuesToRemove.isEmpty()) {
                        attributeDeltas.add(AttributeDeltaBuilder.build(next.getName(), valuesToAdd, valuesToRemove));
                    }
                } else {
                    if (!valuesToAdd.isEmpty()) {
                        attributeDeltas.add(AttributeDeltaBuilder.build(next.getName(), valuesToAdd));
                    }
                }
            });

            // build delta for new or removed attributes
            Set<String> nextNames = propagationData.getAttributes().stream().
                    filter(this::isOrdinaryForAttrForDelta).
                    map(Attribute::getName).
                    collect(Collectors.toSet());
            Set<String> prevNames = attrs.stream().
                    filter(this::isOrdinaryForAttrForDelta).
                    map(Attribute::getName).
                    collect(Collectors.toSet());

            nextNames.stream().filter(name -> !prevNames.contains(name)).
                    forEach(toAdd -> Optional.ofNullable(AttributeUtil.find(toAdd, propagationData.getAttributes())).
                    ifPresent(attr -> attributeDeltas.add(
                    AttributeDeltaBuilder.build(attr.getName(), attr.getValue(), Set.of()))));
            prevNames.stream().filter(name -> !nextNames.contains(name)).
                    forEach(toRemove -> Optional.ofNullable(AttributeUtil.find(toRemove, attrs)).
                    ifPresent(attr -> attributeDeltas.add(
                    AttributeDeltaBuilder.build(attr.getName(), Set.of(), attr.getValue()))));

            if (!attributeDeltas.isEmpty()) {
                propagationData.setAttributeDeltas(attributeDeltas);
            }
        }

        return tasks;
    }
}
