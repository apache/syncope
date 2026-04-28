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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
public class UserLogic extends AbstractUserLogic implements UserLogicOp {

    protected final GroupDAO groupDAO;

    protected final AnySearchDAO searchDAO;

    public UserLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final UserDataBinder binder,
            final UserProvisioningManager provisioningManager,
            final EncryptorManager encryptorManager,
            final ConfParamOps confParamOps,
            final GroupDAO groupDAO,
            final AnySearchDAO searchDAO) {

        super(realmSearchDAO,
                anyTypeDAO,
                templateUtils,
                userDAO,
                binder,
                provisioningManager,
                encryptorManager,
                confParamOps);
        this.groupDAO = groupDAO;
        this.searchDAO = searchDAO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final String key) {
        return binder.getUserTO(key);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public Page<UserTO> search(
            final SearchCond searchCond,
            final Pageable pageable,
            final String realm,
            final boolean recursive,
            final boolean details) {

        Realm base = realmSearchDAO.findByFullPath(realm).
                orElseThrow(() -> new NotFoundException("Realm " + realm));

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_SEARCH), realm);

        SearchCond effectiveCond = searchCond == null ? searchDAO.getAllMatchingCond() : searchCond;

        long count = searchDAO.count(base, recursive, authRealms, effectiveCond, AnyTypeKind.USER);

        List<User> matching = searchDAO.search(
                base, recursive, authRealms, effectiveCond, pageable, AnyTypeKind.USER);
        List<UserTO> result = matching.stream().
                map(user -> binder.getUserTO(user, details)).
                toList();

        return new SyncopePage<>(result, pageable, count);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_CREATE + "')")
    @Override
    public ProvisioningResult<UserTO> create(final UserCR createReq, final boolean nullPriorityAsync) {
        return doCreate(createReq, false, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> update(final UserUR userUR, final boolean nullPriorityAsync) {
        return doUpdate(userUR, false, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> status(final StatusR statusR, final boolean nullPriorityAsync) {
        // security checks
        UserTO toUpdate = binder.getUserTO(statusR.getKey());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE),
                toUpdate.getRealm());
        userDAO.securityChecks(
                authRealms,
                toUpdate.getKey(),
                toUpdate.getRealm(),
                groups(toUpdate));

        // ensures the actual user key is effectively on the request - as the binder.getUserTO(statusR.getKey())
        // call above works with username as well
        statusR.setKey(toUpdate.getKey());

        return doStatus(statusR, nullPriorityAsync);
    }

    protected void updateChecks(final String key) {
        UserTO userTO = binder.getUserTO(key);

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE),
                userTO.getRealm());
        userDAO.securityChecks(
                authRealms,
                userTO.getKey(),
                userTO.getRealm(),
                userTO.getMemberships().stream().
                        map(MembershipTO::getGroupKey).
                        collect(Collectors.toSet()));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final String key, final Collection<String> resources) {
        updateChecks(key);

        UserUR req = new UserUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                        toList()).
                build();

        return binder.getUserTO(provisioningManager.unlink(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final String key, final Collection<String> resources) {
        updateChecks(key);

        UserUR req = new UserUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                        toList()).
                build();

        return binder.getUserTO(provisioningManager.link(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        updateChecks(key);

        UserUR req = new UserUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                        toList()).
                build();

        return update(req, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        UserUR req = new UserUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                        toList()).
                build();

        if (changepwd) {
            req.setPassword(new PasswordPatch.Builder().value(password).onSyncope(false).resources(resources).build());
        }

        return update(req, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> deprovision(
            final String key,
            final List<String> resources,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.deprovision(
                key, resources, nullPriorityAsync, AuthContextUtils.getUsername());

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getUserTO(key));
        result.getPropagationStatuses().addAll(statuses);
        result.getPropagationStatuses().sort(Comparator.comparing(item -> resources.indexOf(item.getResource())));
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> provision(
            final String key,
            final List<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.provision(
                key, changePwd, password, resources, nullPriorityAsync, AuthContextUtils.getUsername());

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getUserTO(key));
        result.getPropagationStatuses().addAll(statuses);
        result.getPropagationStatuses().sort(Comparator.comparing(item -> resources.indexOf(item.getResource())));
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_DELETE + "')")
    @Override
    public ProvisioningResult<UserTO> delete(final String key, final boolean nullPriorityAsync) {
        return doDelete(binder.getUserTO(key), false, nullPriorityAsync);
    }
}
