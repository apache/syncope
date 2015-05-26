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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.misc.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.UnauthorizedException;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.AnyTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class AnyObjectLogic extends AbstractAnyLogic<AnyObjectTO, AnyObjectMod> {

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

    @Autowired
    protected AnyTransformer attrTransformer;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Autowired
    protected AnyObjectProvisioningManager provisioningManager;

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO read(final Long anyObjectKey) {
        return binder.getAnyObjectTO(anyObjectKey);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int count(final List<String> realms) {
        return anyObjectDAO.count(getEffectiveRealms(SyncopeConstants.FULL_ADMIN_REALMS, realms));
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public List<AnyObjectTO> list(
            final int page, final int size, final List<OrderByClause> orderBy, final List<String> realms) {

        return CollectionUtils.collect(anyObjectDAO.findAll(
                getEffectiveRealms(SyncopeConstants.FULL_ADMIN_REALMS, realms),
                page, size, orderBy),
                new Transformer<AnyObject, AnyObjectTO>() {

                    @Override
                    public AnyObjectTO transform(final AnyObject input) {
                        return binder.getAnyObjectTO(input);
                    }
                }, new ArrayList<AnyObjectTO>());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_SEARCH + "')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int searchCount(final SearchCond searchCondition, final List<String> realms) {
        return searchDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_SEARCH), realms),
                searchCondition, AnyTypeKind.ANY_OBJECT);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_SEARCH + "')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public List<AnyObjectTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final List<String> realms) {

        final List<AnyObject> matchingAnyObjects = searchDAO.search(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_SEARCH), realms),
                searchCondition, page, size, orderBy, AnyTypeKind.ANY_OBJECT);
        return CollectionUtils.collect(matchingAnyObjects, new Transformer<AnyObject, AnyObjectTO>() {

            @Override
            public AnyObjectTO transform(final AnyObject input) {
                return binder.getAnyObjectTO(input);
            }
        }, new ArrayList<AnyObjectTO>());
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_CREATE + "')")
    public AnyObjectTO create(final AnyObjectTO anyObjectTO) {
        if (anyObjectTO.getRealm() == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            throw sce;
        }
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_CREATE),
                Collections.singleton(anyObjectTO.getRealm()));
        if (effectiveRealms.isEmpty()) {
            throw new UnauthorizedException(AnyTypeKind.ANY_OBJECT, null);
        }

        // Any transformation (if configured)
        AnyObjectTO actual = attrTransformer.transform(anyObjectTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(anyObjectTO);
        AnyObjectTO savedTO = binder.getAnyObjectTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Override
    public AnyObjectTO update(final AnyObjectMod anyObjectMod) {
        AnyObject anyObject = anyObjectDAO.authFind(anyObjectMod.getKey());
        if (anyObject == null) {
            throw new NotFoundException("AnyObject with key " + anyObjectMod.getKey());
        }
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObjectMod.getRealm()));
        if (effectiveRealms.isEmpty()) {
            throw new UnauthorizedException(AnyTypeKind.ANY_OBJECT, anyObject.getKey());
        }

        // Any transformation (if configured)
        AnyObjectMod actual = attrTransformer.transform(anyObjectMod);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(anyObjectMod);

        AnyObjectTO updatedTO = binder.getAnyObjectTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_DELETE + "')")
    @Override
    public AnyObjectTO delete(final Long anyObjectKey) {
        AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);
        if (anyObject == null) {
            throw new NotFoundException("AnyObject with key " + anyObjectKey);
        }
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.ANY_OBJECT_UPDATE),
                Collections.singleton(anyObject.getRealm().getFullPath()));
        if (effectiveRealms.isEmpty()) {
            throw new UnauthorizedException(AnyTypeKind.ANY_OBJECT, anyObject.getKey());
        }

        List<PropagationStatus> statuses = provisioningManager.delete(anyObjectKey);

        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setKey(anyObjectKey);

        anyObjectTO.getPropagationStatusTOs().addAll(statuses);

        return anyObjectTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public AnyObjectTO unlink(final Long anyObjectKey, final Collection<String> resources) {
        final AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(anyObjectKey);
        anyObjectMod.getResourcesToRemove().addAll(resources);
        final Long updatedResult = provisioningManager.unlink(anyObjectMod);

        return binder.getAnyObjectTO(updatedResult);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public AnyObjectTO link(final Long anyObjectKey, final Collection<String> resources) {
        final AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(anyObjectKey);
        anyObjectMod.getResourcesToAdd().addAll(resources);
        return binder.getAnyObjectTO(provisioningManager.link(anyObjectMod));
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public AnyObjectTO unassign(final Long anyObjectKey, final Collection<String> resources) {
        final AnyObjectMod anyObjectMod = new AnyObjectMod();
        anyObjectMod.setKey(anyObjectKey);
        anyObjectMod.getResourcesToRemove().addAll(resources);
        return update(anyObjectMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public AnyObjectTO assign(
            final Long anyObjectKey, final Collection<String> resources, final boolean changePwd, final String password) {

        final AnyObjectMod userMod = new AnyObjectMod();
        userMod.setKey(anyObjectKey);
        userMod.getResourcesToAdd().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public AnyObjectTO deprovision(final Long anyObjectKey, final Collection<String> resources) {
        final AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);

        List<PropagationStatus> statuses = provisioningManager.deprovision(anyObjectKey, resources);

        AnyObjectTO updatedTO = binder.getAnyObjectTO(anyObject);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.ANY_OBJECT_UPDATE + "')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public AnyObjectTO provision(
            final Long anyObjectKey, final Collection<String> resources, final boolean changePwd, final String password) {
        AnyObjectTO original = binder.getAnyObjectTO(anyObjectKey);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(anyObjectKey, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
    }

    @Override
    protected AnyObjectTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof AnyObjectTO) {
                    key = ((AnyObjectTO) args[i]).getKey();
                } else if (args[i] instanceof AnyObjectMod) {
                    key = ((AnyObjectMod) args[i]).getKey();
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
