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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.logic.api.LogicActions;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
public class AnyObjectLogic extends AbstractAnyLogic<AnyObjectTO, AnyObjectCR, AnyObjectUR> {

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO searchDAO;

    protected final AnyObjectDataBinder binder;

    protected final AnyObjectProvisioningManager provisioningManager;

    public AnyObjectLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final AnyObjectDataBinder binder,
            final AnyObjectProvisioningManager provisioningManager) {

        super(realmSearchDAO, anyTypeDAO, templateUtils);

        this.anyObjectDAO = anyObjectDAO;
        this.searchDAO = searchDAO;
        this.binder = binder;
        this.provisioningManager = provisioningManager;
    }

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO read(final String key) {
        return binder.getAnyObjectTO(key);
    }

    @Transactional(readOnly = true)
    public AnyObjectTO read(final String type, final String name) {
        return anyObjectDAO.findKey(type, name).
                map(binder::getAnyObjectTO).
                orElseThrow(() -> new NotFoundException("AnyObject " + type + " " + name));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AnyObjectTO> search(
            final SearchCond searchCond,
            final Pageable pageable,
            final String realm,
            final boolean recursive,
            final boolean details) {

        if (searchCond.hasAnyTypeCond() == null) {
            throw new UnsupportedOperationException("Need to specify " + AnyType.class.getSimpleName());
        }

        Realm base = realmSearchDAO.findByFullPath(realm).
                orElseThrow(() -> new NotFoundException("Realm " + realm));

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.SEARCH.getFor(searchCond.hasAnyTypeCond())),
                realm);

        long count = searchDAO.count(base, recursive, authRealms, searchCond, AnyTypeKind.ANY_OBJECT);

        List<AnyObject> matching = searchDAO.search(
                base, recursive, authRealms, searchCond, pageable, AnyTypeKind.ANY_OBJECT);
        List<AnyObjectTO> result = matching.stream().
                map(anyObject -> binder.getAnyObjectTO(anyObject, details)).
                toList();

        return new SyncopePage<>(result, pageable, count);
    }

    public ProvisioningResult<AnyObjectTO> create(final AnyObjectCR createReq, final boolean nullPriorityAsync) {
        Pair<AnyObjectCR, List<LogicActions>> before = beforeCreate(createReq);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }
        if (before.getLeft().getType() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
        }

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.CREATE.getFor(before.getLeft().getType())),
                before.getLeft().getRealm());
        anyObjectDAO.securityChecks(
                authRealms,
                null,
                before.getLeft().getRealm(),
                before.getLeft().getMemberships().stream().filter(Objects::nonNull).
                        map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                        collect(Collectors.toSet()));

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(
                before.getLeft(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        return afterCreate(binder.getAnyObjectTO(created.getKey()), created.getRight(), before.getRight());
    }

    protected Set<String> groups(final AnyObjectTO anyObjectTO) {
        return anyObjectTO.getMemberships().stream().filter(Objects::nonNull).
                map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                collect(Collectors.toSet());
    }

    @Override
    public ProvisioningResult<AnyObjectTO> update(final AnyObjectUR req, final boolean nullPriorityAsync) {
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(req.getKey());
        Pair<AnyObjectUR, List<LogicActions>> before = beforeUpdate(req, anyObjectTO.getRealm());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());

        Set<String> groups = groups(anyObjectTO);
        groups.removeAll(req.getMemberships().stream().filter(Objects::nonNull).
                filter(m -> m.getOperation() == PatchOperation.DELETE).
                map(MembershipUR::getGroup).filter(Objects::nonNull).
                collect(Collectors.toSet()));

        anyObjectDAO.securityChecks(
                authRealms,
                before.getLeft().getKey(),
                anyObjectTO.getRealm(),
                groups);

        Pair<AnyObjectUR, List<PropagationStatus>> after = provisioningManager.update(
                req, Set.of(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        ProvisioningResult<AnyObjectTO> result = afterUpdate(
                binder.getAnyObjectTO(after.getLeft().getKey()),
                after.getRight(),
                before.getRight());

        return result;
    }

    @Override
    public ProvisioningResult<AnyObjectTO> delete(final String key, final boolean nullPriorityAsync) {
        Pair<AnyObjectTO, List<LogicActions>> before = beforeDelete(binder.getAnyObjectTO(key));

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.DELETE.getFor(before.getLeft().getType())),
                before.getLeft().getRealm());
        anyObjectDAO.securityChecks(
                authRealms,
                before.getLeft().getKey(),
                before.getLeft().getRealm(),
                groups(before.getLeft()));

        List<PropagationStatus> statuses = provisioningManager.delete(
                before.getLeft().getKey(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        AnyObjectTO deletedTO;
        if (anyObjectDAO.existsById(before.getLeft().getKey())) {
            deletedTO = binder.getAnyObjectTO(before.getLeft().getKey());
        } else {
            deletedTO = new AnyObjectTO();
            deletedTO.setKey(before.getLeft().getKey());
        }

        return afterDelete(deletedTO, statuses, before.getRight());
    }

    protected void updateChecks(final String key) {
        AnyObject anyObject = anyObjectDAO.authFind(key);

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObject.getType().getKey())),
                anyObject.getRealm().getFullPath());
        anyObjectDAO.securityChecks(
                authRealms,
                anyObject.getKey(),
                anyObject.getRealm().getFullPath(),
                anyObject.getMemberships().stream().
                        map(m -> m.getRightEnd().getKey()).
                        collect(Collectors.toSet()));
    }

    @Override
    public AnyObjectTO unlink(final String key, final Collection<String> resources) {
        updateChecks(key);

        AnyObjectUR req = new AnyObjectUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                        toList()).
                build();

        return binder.getAnyObjectTO(provisioningManager.unlink(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @Override
    public AnyObjectTO link(final String key, final Collection<String> resources) {
        updateChecks(key);

        AnyObjectUR req = new AnyObjectUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                        toList()).
                build();

        return binder.getAnyObjectTO(provisioningManager.link(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @Override
    public ProvisioningResult<AnyObjectTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        updateChecks(key);

        AnyObjectUR req = new AnyObjectUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                        toList()).
                build();

        return update(req, nullPriorityAsync);
    }

    @Override
    public ProvisioningResult<AnyObjectTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        AnyObjectUR req = new AnyObjectUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                        toList()).
                build();
        return update(req, nullPriorityAsync);
    }

    @Override
    public ProvisioningResult<AnyObjectTO> deprovision(
            final String key,
            final List<String> resources,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.deprovision(
                key, resources, nullPriorityAsync, AuthContextUtils.getUsername());

        ProvisioningResult<AnyObjectTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getAnyObjectTO(key));
        result.getPropagationStatuses().addAll(statuses);
        result.getPropagationStatuses().sort(Comparator.comparing(item -> resources.indexOf(item.getResource())));
        return result;
    }

    @Override
    public ProvisioningResult<AnyObjectTO> provision(
            final String key,
            final List<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.provision(
                key, resources, nullPriorityAsync, AuthContextUtils.getUsername());

        ProvisioningResult<AnyObjectTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getAnyObjectTO(key));
        result.getPropagationStatuses().addAll(statuses);
        result.getPropagationStatuses().sort(Comparator.comparing(item -> resources.indexOf(item.getResource())));
        return result;
    }

    @Override
    protected AnyObjectTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof AnyObjectTO anyObjectTO) {
                    key = anyObjectTO.getKey();
                } else if (args[i] instanceof AnyObjectUR anyObjectUR) {
                    key = anyObjectUR.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAnyObjectTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
