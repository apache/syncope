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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.provisioning.api.AnyTransformer;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class UserLogic extends AbstractAnyLogic<UserTO, UserMod> {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AnyTransformer anyTransformer;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    protected SyncopeLogic syncopeLogic;

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public String getUsername(final Long key) {
        return binder.getUserTO(key).getUsername();
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public Long getKey(final String username) {
        return binder.getUserTO(username).getKey();
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public int count(final List<String> realms) {
        return userDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_LIST), realms));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> list(
            final int page, final int size, final List<OrderByClause> orderBy,
            final List<String> realms, final boolean details) {

        return CollectionUtils.collect(userDAO.findAll(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_LIST), realms),
                page, size, orderBy),
                new Transformer<User, UserTO>() {

                    @Override
                    public UserTO transform(final User input) {
                        return binder.getUserTO(input, details);
                    }
                }, new ArrayList<UserTO>());
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Pair<String, UserTO> readSelf() {
        return ImmutablePair.of(
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                binder.getAuthenticatedUserTO());
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final Long key) {
        return binder.getUserTO(key);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public int searchCount(final SearchCond searchCondition, final List<String> realms) {
        return searchDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_SEARCH), realms),
                searchCondition, AnyTypeKind.USER);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final List<String> realms, final boolean details) {

        List<User> matchingUsers = searchDAO.search(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_SEARCH), realms),
                searchCondition, page, size, orderBy, AnyTypeKind.USER);
        return CollectionUtils.collect(matchingUsers, new Transformer<User, UserTO>() {

            @Override
            public UserTO transform(final User input) {
                return binder.getUserTO(input, details);
            }
        }, new ArrayList<UserTO>());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    public UserTO selfCreate(final UserTO userTO, final boolean storePassword) {
        return doCreate(userTO, storePassword);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_CREATE + "')")
    @Override
    public UserTO create(final UserTO userTO) {
        return create(userTO, true);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_CREATE + "')")
    public UserTO create(final UserTO userTO, final boolean storePassword) {
        if (userTO.getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }
        // security checks
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_CREATE),
                Collections.singleton(userTO.getRealm()));
        securityChecks(effectiveRealms, userTO.getRealm(), null);

        return doCreate(userTO, storePassword);
    }

    protected UserTO doCreate(final UserTO userTO, final boolean storePassword) {
        // Any transformation (if configured)
        UserTO actual = anyTransformer.transform(userTO);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(actual, storePassword);

        UserTO savedTO = binder.getUserTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    public UserTO selfUpdate(final UserMod userMod) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userMod.setKey(userTO.getKey());
        return doUpdate(userMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO update(final UserMod userMod) {
        // Any transformation (if configured)
        UserMod actual = anyTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        // security checks
        UserTO toUpdate = binder.getUserTO(userMod.getKey());
        Set<String> requestedRealms = new HashSet<>();
        requestedRealms.add(toUpdate.getRealm());
        if (StringUtils.isNotBlank(actual.getRealm())) {
            requestedRealms.add(actual.getRealm());
        }
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                requestedRealms);
        securityChecks(effectiveRealms, toUpdate.getRealm(), toUpdate.getKey());
        if (StringUtils.isNotBlank(actual.getRealm())) {
            securityChecks(effectiveRealms, actual.getRealm(), toUpdate.getKey());
        }

        return doUpdate(actual);
    }

    protected UserTO doUpdate(final UserMod userMod) {
        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(userMod);

        UserTO updatedTO = binder.getUserTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    protected Map.Entry<Long, List<PropagationStatus>> setStatusOnWfAdapter(final StatusMod statusMod) {
        Map.Entry<Long, List<PropagationStatus>> updated;

        switch (statusMod.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(statusMod);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(statusMod);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(statusMod);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    public UserTO status(final StatusMod statusMod) {
        // security checks
        UserTO toUpdate = binder.getUserTO(statusMod.getKey());
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(toUpdate.getRealm()));
        securityChecks(effectiveRealms, toUpdate.getRealm(), toUpdate.getKey());

        Map.Entry<Long, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusMod);
        UserTO savedTO = binder.getUserTO(updated.getKey());
        savedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.MUST_CHANGE_PASSWORD + "')")
    public UserTO changePassword(final String password) {
        UserMod userMod = new UserMod();
        userMod.setPassword(password);
        return selfUpdate(userMod);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    @Transactional
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        User user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (syncopeLogic.isPwdResetRequiringSecurityQuestions()
                && (securityAnswer == null || !securityAnswer.equals(user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        provisioningManager.requestPasswordReset(user.getKey());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        User user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }
        provisioningManager.confirmPasswordReset(user.getKey(), token, password);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    public UserTO selfDelete() {
        UserTO userTO = binder.getAuthenticatedUserTO();

        return doDelete(userTO.getKey());
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_DELETE + "')")
    @Override
    public UserTO delete(final Long key) {
        // security checks
        UserTO toDelete = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_DELETE),
                Collections.singleton(toDelete.getRealm()));
        securityChecks(effectiveRealms, toDelete.getRealm(), toDelete.getKey());

        return doDelete(key);
    }

    protected UserTO doDelete(final Long key) {
        List<Group> ownedGroups = groupDAO.findOwnedByUser(key);
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

        UserTO deletedTO;
        if (userDAO.find(key) == null) {
            deletedTO = new UserTO();
            deletedTO.setKey(key);
        } else {
            deletedTO = binder.getUserTO(key);
        }
        deletedTO.getPropagationStatusTOs().addAll(statuses);

        return deletedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToRemove().addAll(resources);

        return binder.getUserTO(provisioningManager.unlink(userMod));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToAdd().addAll(resources);

        return binder.getUserTO(provisioningManager.link(userMod));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO unassign(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToRemove().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO assign(
            final Long key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToAdd().addAll(resources);

        if (changepwd) {
            StatusMod statusMod = new StatusMod();
            statusMod.setOnSyncope(false);
            statusMod.getResources().addAll(resources);
            userMod.setPwdPropRequest(statusMod);
            userMod.setPassword(password);
        }

        return update(userMod);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO deprovision(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources);

        UserTO updatedTO = binder.getUserTO(key);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO provision(
            final Long key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        user.getPropagationStatusTOs().addAll(provisioningManager.provision(key, changePwd, password, resources));
        return user;
    }

    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Object key = null;

        if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    key = ((UserTO) args[i]).getKey();
                } else if (args[i] instanceof UserMod) {
                    key = ((UserMod) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                return key instanceof Long ? binder.getUserTO((Long) key) : binder.getUserTO((String) key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
