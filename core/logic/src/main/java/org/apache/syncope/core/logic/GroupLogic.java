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
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.misc.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.UnauthorizedException;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.provisioning.api.AnyTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class GroupLogic extends AbstractAnyLogic<GroupTO, GroupMod> {

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected GroupDataBinder binder;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AnyTransformer attrTransformer;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Autowired
    protected GroupProvisioningManager provisioningManager;

    @Override
    protected void securityChecks(final Set<String> effectiveRealms, final String realm, final Long key) {
        if (!CollectionUtils.exists(effectiveRealms, new Predicate<String>() {

            @Override
            public boolean evaluate(final String ownedRealm) {
                return realm.startsWith(ownedRealm) || ownedRealm.equals(RealmUtils.getGroupOwnerRealm(realm, key));
            }
        })) {

            throw new UnauthorizedException(AnyTypeKind.GROUP, key);
        }
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public GroupTO read(final Long key) {
        return binder.getGroupTO(key);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    @Transactional(readOnly = true)
    public List<GroupTO> own() {
        return CollectionUtils.collect(
                userDAO.findAllGroups(userDAO.find(AuthContextUtils.getUsername())),
                new Transformer<Group, GroupTO>() {

                    @Override
                    public GroupTO transform(final Group input) {
                        return binder.getGroupTO(input, true);
                    }
                }, new ArrayList<GroupTO>());
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public int count(final List<String> realms) {
        return groupDAO.count(getEffectiveRealms(SyncopeConstants.FULL_ADMIN_REALMS, realms));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public List<GroupTO> list(
            final int page, final int size, final List<OrderByClause> orderBy,
            final List<String> realms, final boolean details) {

        return CollectionUtils.collect(groupDAO.findAll(
                getEffectiveRealms(SyncopeConstants.FULL_ADMIN_REALMS, realms),
                page, size, orderBy),
                new Transformer<Group, GroupTO>() {

                    @Override
                    public GroupTO transform(final Group input) {
                        return binder.getGroupTO(input, details);
                    }
                }, new ArrayList<GroupTO>());
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public int searchCount(final SearchCond searchCondition, final List<String> realms) {
        return searchDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_SEARCH), realms),
                searchCondition, AnyTypeKind.GROUP);
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public List<GroupTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final List<String> realms, final boolean details) {

        List<Group> matchingGroups = searchDAO.search(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_SEARCH), realms),
                searchCondition, page, size, orderBy, AnyTypeKind.GROUP);
        return CollectionUtils.collect(matchingGroups, new Transformer<Group, GroupTO>() {

            @Override
            public GroupTO transform(final Group input) {
                return binder.getGroupTO(input, details);
            }
        }, new ArrayList<GroupTO>());
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_CREATE + "')")
    @Override
    public GroupTO create(final GroupTO groupTO) {
        if (groupTO.getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }
        // security checks
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_CREATE),
                Collections.singleton(groupTO.getRealm()));
        securityChecks(effectiveRealms, groupTO.getRealm(), null);

        // Any transformation (if configured)
        GroupTO actual = attrTransformer.transform(groupTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(groupTO);
        GroupTO savedTO = binder.getGroupTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO update(final GroupMod groupMod) {
        // Any transformation (if configured)
        GroupMod actual = attrTransformer.transform(groupMod);
        LOG.debug("Transformed: {}", actual);

        // security checks
        GroupTO toUpdate = binder.getGroupTO(groupMod.getKey());
        Set<String> requestedRealms = new HashSet<>();
        requestedRealms.add(toUpdate.getRealm());
        if (StringUtils.isNotBlank(actual.getRealm())) {
            requestedRealms.add(actual.getRealm());
        }
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                requestedRealms);
        securityChecks(effectiveRealms, toUpdate.getRealm(), toUpdate.getKey());
        if (StringUtils.isNotBlank(actual.getRealm())) {
            securityChecks(effectiveRealms, actual.getRealm(), toUpdate.getKey());
        }

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(groupMod);

        GroupTO updatedTO = binder.getGroupTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_DELETE + "')")
    @Override
    public GroupTO delete(final Long key) {
        // security checks
        GroupTO toDelete = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_DELETE),
                Collections.singleton(toDelete.getRealm()));
        securityChecks(effectiveRealms, toDelete.getRealm(), toDelete.getKey());

        List<Group> ownedGroups = groupDAO.findOwnedByGroup(key);
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(CollectionUtils.collect(ownedGroups, new Transformer<Group, String>() {

                @Override
                public String transform(final Group group) {
                    return group.getKey() + " " + group.getName();
                }
            }, new ArrayList<String>()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(key);

        GroupTO groupTO = new GroupTO();
        groupTO.setKey(key);

        groupTO.getPropagationStatusTOs().addAll(statuses);

        return groupTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO unlink(final Long key, final Collection<String> resources) {
        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                Collections.singleton(group.getRealm()));
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupMod groupMod = new GroupMod();
        groupMod.setKey(key);
        groupMod.getResourcesToRemove().addAll(resources);

        return binder.getGroupTO(provisioningManager.unlink(groupMod));
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO link(final Long key, final Collection<String> resources) {
        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                Collections.singleton(group.getRealm()));
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupMod groupMod = new GroupMod();
        groupMod.setKey(key);
        groupMod.getResourcesToAdd().addAll(resources);

        return binder.getGroupTO(provisioningManager.link(groupMod));
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO unassign(final Long key, final Collection<String> resources) {
        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                Collections.singleton(group.getRealm()));
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupMod groupMod = new GroupMod();
        groupMod.setKey(key);
        groupMod.getResourcesToRemove().addAll(resources);
        return update(groupMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO assign(
            final Long key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {

        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                Collections.singleton(group.getRealm()));
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupMod groupMod = new GroupMod();
        groupMod.setKey(key);
        groupMod.getResourcesToAdd().addAll(resources);

        return update(groupMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO deprovision(final Long key, final Collection<String> resources) {
        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                Collections.singleton(group.getRealm()));
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources);

        GroupTO updatedTO = binder.getGroupTO(key);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO provision(
            final Long key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_UPDATE),
                Collections.singleton(group.getRealm()));
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        group.getPropagationStatusTOs().addAll(provisioningManager.provision(key, resources));
        return group;
    }

    @Override
    protected GroupTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof GroupTO) {
                    key = ((GroupTO) args[i]).getKey();
                } else if (args[i] instanceof GroupMod) {
                    key = ((GroupMod) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                return binder.getGroupTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
