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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class AnyObjectLogic extends AbstractAnyLogic<AnyObjectTO, AnyObjectPatch> {

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected AnyObjectDataBinder binder;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Autowired
    protected AnyObjectProvisioningManager provisioningManager;

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO read(final Long key) {
        return binder.getAnyObjectTO(key);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public int count(final List<String> realms) {
        return anyObjectDAO.count(getEffectiveRealms(SyncopeConstants.FULL_ADMIN_REALMS, realms));
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public List<AnyObjectTO> list(
            final int page, final int size, final List<OrderByClause> orderBy,
            final List<String> realms, final boolean details) {

        return list(null, page, size, orderBy, realms, details);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_LIST + "')")
    @Transactional(readOnly = true)
    public List<AnyObjectTO> list(final String type,
            final int page, final int size, final List<OrderByClause> orderBy,
            final List<String> realms, final boolean details) {

        Set<String> effectiveRealms = getEffectiveRealms(SyncopeConstants.FULL_ADMIN_REALMS, realms);

        return CollectionUtils.collect(StringUtils.isBlank(type)
                ? anyObjectDAO.findAll(effectiveRealms, page, size, orderBy)
                : anyObjectDAO.findAll(type, effectiveRealms, page, size, orderBy),
                new Transformer<AnyObject, AnyObjectTO>() {

                    @Override
                    public AnyObjectTO transform(final AnyObject input) {
                        return binder.getAnyObjectTO(input, details);
                    }
                }, new ArrayList<AnyObjectTO>());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public int searchCount(final SearchCond searchCondition, final List<String> realms) {
        return searchDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_SEARCH), realms),
                searchCondition, AnyTypeKind.ANY_OBJECT);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public List<AnyObjectTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final List<String> realms, final boolean details) {

        List<AnyObject> matchingAnyObjects = searchDAO.search(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_SEARCH), realms),
                searchCondition, page, size, orderBy, AnyTypeKind.ANY_OBJECT);
        return CollectionUtils.collect(matchingAnyObjects, new Transformer<AnyObject, AnyObjectTO>() {

            @Override
            public AnyObjectTO transform(final AnyObject input) {
                return binder.getAnyObjectTO(input, details);
            }
        }, new ArrayList<AnyObjectTO>());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_CREATE + "')")
    @Override
    public AnyObjectTO create(final AnyObjectTO anyObjectTO) {
        Pair<AnyObjectTO, List<LogicActions>> before = beforeCreate(anyObjectTO);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_CREATE),
                Collections.singleton(before.getLeft().getRealm()));
        securityChecks(effectiveRealms, before.getLeft().getRealm(), null);

        if (before.getLeft().getType() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
        }

        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(before.getLeft());
        AnyObjectTO savedTO = binder.getAnyObjectTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());

        return afterCreate(savedTO, before.getValue());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO update(final AnyObjectPatch anyObjectPatch) {
        AnyObjectTO anyObjectTO = binder.getAnyObjectTO(anyObjectPatch.getKey());
        Pair<AnyObjectPatch, List<LogicActions>> before = beforeUpdate(anyObjectPatch, anyObjectTO.getRealm());

        if (before.getLeft().getRealm() != null && StringUtils.isNotBlank(before.getLeft().getRealm().getValue())) {
            Set<String> requestedRealms = new HashSet<>();
            requestedRealms.add(before.getLeft().getRealm().getValue());
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                    requestedRealms);
            securityChecks(effectiveRealms, before.getLeft().getRealm().getValue(), before.getLeft().getKey());
        }

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(anyObjectPatch);

        AnyObjectTO updatedTO = binder.getAnyObjectTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());

        return afterUpdate(updatedTO, before.getRight());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_DELETE + "')")
    @Override
    public AnyObjectTO delete(final Long key) {
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Pair<AnyObjectTO, List<LogicActions>> before = beforeDelete(anyObject);

        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_DELETE),
                Collections.singleton(before.getLeft().getRealm()));
        securityChecks(effectiveRealms, before.getLeft().getRealm(), before.getLeft().getKey());

        List<PropagationStatus> statuses = provisioningManager.delete(before.getLeft().getKey());

        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setKey(before.getLeft().getKey());
        anyObjectTO.getPropagationStatusTOs().addAll(statuses);

        return afterDelete(anyObjectTO, before.getRight());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO unlink(final Long key, final Collection<String> resources) {
        // security checks
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm()));
        securityChecks(effectiveRealms, anyObject.getRealm(), anyObject.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build();
            }
        }));

        return binder.getAnyObjectTO(provisioningManager.unlink(patch));
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO link(final Long key, final Collection<String> resources) {
        // security checks
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm()));
        securityChecks(effectiveRealms, anyObject.getRealm(), anyObject.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        return binder.getAnyObjectTO(provisioningManager.link(patch));
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO unassign(final Long key, final Collection<String> resources) {
        // security checks
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm()));
        securityChecks(effectiveRealms, anyObject.getRealm(), anyObject.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build();
            }
        }));

        return update(patch);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO assign(
            final Long key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {

        // security checks
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm()));
        securityChecks(effectiveRealms, anyObject.getRealm(), anyObject.getKey());

        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        return update(patch);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO deprovision(final Long key, final Collection<String> resources) {
        // security checks
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm()));
        securityChecks(effectiveRealms, anyObject.getRealm(), anyObject.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources);

        AnyObjectTO updatedTO = binder.getAnyObjectTO(key);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO provision(
            final Long key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        // security checks
        AnyObjectTO anyObject = binder.getAnyObjectTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm()));
        securityChecks(effectiveRealms, anyObject.getRealm(), anyObject.getKey());

        anyObject.getPropagationStatusTOs().addAll(provisioningManager.provision(key, resources));
        return anyObject;
    }

    @Override
    protected AnyObjectTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof AnyObjectTO) {
                    key = ((AnyObjectTO) args[i]).getKey();
                } else if (args[i] instanceof AnyObjectPatch) {
                    key = ((AnyObjectPatch) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
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
