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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class AnyObjectLogic extends AbstractAnyLogic<AnyObjectTO, AnyObjectPatch> {

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected AnyObjectDataBinder binder;

    @Autowired
    protected AnyObjectProvisioningManager provisioningManager;

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO read(final String key) {
        return binder.getAnyObjectTO(key);
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<AnyObjectTO>> search(
            final SearchCond searchCond,
            final int page, final int size, final List<OrderByClause> orderBy,
            final String realm,
            final boolean details) {

        if (searchCond.hasAnyTypeCond() == null) {
            throw new UnsupportedOperationException("Need to specify " + AnyType.class.getSimpleName());
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.SEARCH.getFor(searchCond.hasAnyTypeCond())),
                realm);

        int count = searchDAO.count(effectiveRealms, searchCond, AnyTypeKind.ANY_OBJECT);

        List<AnyObject> matching = searchDAO.search(
                effectiveRealms, searchCond, page, size, orderBy, AnyTypeKind.ANY_OBJECT);
        List<AnyObjectTO> result = matching.stream().
                map(anyObject -> binder.getAnyObjectTO(anyObject, details)).collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @Override
    public ProvisioningResult<AnyObjectTO> create(final AnyObjectTO anyObjectTO, final boolean nullPriorityAsync) {
        Pair<AnyObjectTO, List<LogicActions>> before = beforeCreate(anyObjectTO);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }
        if (before.getLeft().getType() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.CREATE.getFor(before.getLeft().getType())),
                before.getLeft().getRealm());
        securityChecks(effectiveRealms, before.getLeft().getRealm(), null);

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(before.getLeft(), nullPriorityAsync);

        return afterCreate(binder.getAnyObjectTO(created.getKey()), created.getRight(), before.getRight());
    }

    @Override
    public ProvisioningResult<AnyObjectTO> update(
            final AnyObjectPatch anyObjectPatch, final boolean nullPriorityAsync) {

        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(anyObjectPatch.getKey());
        Set<String> dynRealmsBefore = new HashSet<>(anyObjectTO.getDynRealms());
        Pair<AnyObjectPatch, List<LogicActions>> before = beforeUpdate(anyObjectPatch, anyObjectTO.getRealm());

        String realm =
                before.getLeft().getRealm() != null && StringUtils.isNotBlank(before.getLeft().getRealm().getValue())
                ? before.getLeft().getRealm().getValue()
                : anyObjectTO.getRealm();
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                realm);
        boolean authDynRealms = securityChecks(effectiveRealms, realm, before.getLeft().getKey());

        Pair<String, List<PropagationStatus>> updated = provisioningManager.update(anyObjectPatch, nullPriorityAsync);

        return afterUpdate(
                binder.getAnyObjectTO(updated.getKey()),
                updated.getRight(),
                before.getRight(),
                authDynRealms,
                dynRealmsBefore);
    }

    @Override
    public ProvisioningResult<AnyObjectTO> delete(final String key, final boolean nullPriorityAsync) {
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Pair<AnyObjectTO, List<LogicActions>> before = beforeDelete(anyObject);

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.DELETE.getFor(before.getLeft().getType())),
                before.getLeft().getRealm());
        securityChecks(effectiveRealms, before.getLeft().getRealm(), before.getLeft().getKey());

        List<PropagationStatus> statuses = provisioningManager.delete(before.getLeft().getKey(), nullPriorityAsync);

        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setKey(before.getLeft().getKey());

        return afterDelete(anyObjectTO, statuses, before.getRight());
    }

    @Override
    public AnyObjectTO unlink(final String key, final Collection<String> resources) {
        // security checks
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());
        securityChecks(effectiveRealms, anyObjectTO.getRealm(), anyObjectTO.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build()).
                collect(Collectors.toList()));

        return binder.getAnyObjectTO(provisioningManager.unlink(patch));
    }

    @Override
    public AnyObjectTO link(final String key, final Collection<String> resources) {
        // security checks
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());
        securityChecks(effectiveRealms, anyObjectTO.getRealm(), anyObjectTO.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
                collect(Collectors.toList()));

        return binder.getAnyObjectTO(provisioningManager.link(patch));
    }

    @Override
    public ProvisioningResult<AnyObjectTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());
        securityChecks(effectiveRealms, anyObjectTO.getRealm(), anyObjectTO.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build()).
                collect(Collectors.toList()));

        return update(patch, nullPriorityAsync);
    }

    @Override
    public ProvisioningResult<AnyObjectTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        // security checks
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());
        securityChecks(effectiveRealms, anyObjectTO.getRealm(), anyObjectTO.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
                collect(Collectors.toList()));

        return update(patch, nullPriorityAsync);
    }

    @Override
    public ProvisioningResult<AnyObjectTO> deprovision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());
        securityChecks(effectiveRealms, anyObjectTO.getRealm(), anyObjectTO.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources, nullPriorityAsync);

        ProvisioningResult<AnyObjectTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getAnyObjectTO(key));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @Override
    public ProvisioningResult<AnyObjectTO> provision(
            final String key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        // security checks
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(AnyEntitlement.UPDATE.getFor(anyObjectTO.getType())),
                anyObjectTO.getRealm());
        securityChecks(effectiveRealms, anyObjectTO.getRealm(), anyObjectTO.getKey());

        List<PropagationStatus> statuses = provisioningManager.provision(key, resources, nullPriorityAsync);

        ProvisioningResult<AnyObjectTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getAnyObjectTO(key));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @Override
    protected AnyObjectTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AnyObjectTO) {
                    key = ((AnyObjectTO) args[i]).getKey();
                } else if (args[i] instanceof AnyObjectPatch) {
                    key = ((AnyObjectPatch) args[i]).getKey();
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
