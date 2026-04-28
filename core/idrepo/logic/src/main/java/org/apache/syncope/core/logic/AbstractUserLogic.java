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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;

abstract class AbstractUserLogic extends AbstractAnyLogic<UserTO, UserCR, UserUR> {

    protected final UserDAO userDAO;

    protected final UserDataBinder binder;

    protected final UserProvisioningManager provisioningManager;

    protected final EncryptorManager encryptorManager;

    protected final ConfParamOps confParamOps;

    protected AbstractUserLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final UserDataBinder binder,
            final UserProvisioningManager provisioningManager,
            final EncryptorManager encryptorManager,
            final ConfParamOps confParamOps) {

        super(realmSearchDAO, anyTypeDAO, templateUtils);
        this.userDAO = userDAO;
        this.binder = binder;
        this.provisioningManager = provisioningManager;
        this.encryptorManager = encryptorManager;
        this.confParamOps = confParamOps;
    }

    protected ProvisioningResult<UserTO> doCreate(
            final UserCR userCR,
            final boolean self,
            final boolean nullPriorityAsync) {

        BeforeResult<UserCR> before = beforeCreate(userCR);

        if (before.key().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        if (!self) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_CREATE),
                    before.key().getRealm());
            userDAO.securityChecks(
                    authRealms,
                    null,
                    before.key().getRealm(),
                    before.key().getMemberships().stream().filter(Objects::nonNull).
                            map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                            collect(Collectors.toSet()));
        }

        ProvisioningManager.ProvisioningResult<String> created = provisioningManager.create(
                before.key(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        return afterCreate(binder.getUserTO(created.key()), created.statuses(), before.actions());
    }

    protected Set<String> groups(final UserTO userTO) {
        return userTO.getMemberships().stream().filter(Objects::nonNull).
                map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                collect(Collectors.toSet());
    }

    protected ProvisioningResult<UserTO> doUpdate(
            final UserUR userUR, final boolean self, final boolean nullPriorityAsync) {

        UserTO userTO = binder.getUserTO(userUR.getKey());
        BeforeResult<UserUR> before = beforeUpdate(userUR, userTO.getRealm());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE),
                userTO.getRealm());
        if (!self) {
            Set<String> groups = groups(userTO);
            groups.removeAll(userUR.getMemberships().stream().filter(Objects::nonNull).
                    filter(m -> m.getOperation() == PatchOperation.DELETE).
                    map(MembershipUR::getGroup).filter(Objects::nonNull).
                    collect(Collectors.toSet()));

            userDAO.securityChecks(
                    authRealms,
                    before.key().getKey(),
                    userTO.getRealm(),
                    groups);
        }

        ProvisioningManager.ProvisioningResult<UserUR> after = provisioningManager.update(
                before.key(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        ProvisioningResult<UserTO> result = afterUpdate(
                binder.getUserTO(after.key().getKey()),
                after.statuses(),
                before.actions());

        return result;
    }

    protected ProvisioningManager.ProvisioningResult<String> setStatusOnWfAdapter(
            final StatusR statusR, final boolean nullPriorityAsync) {

        ProvisioningManager.ProvisioningResult<String> updated;

        switch (statusR.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(
                        statusR, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(
                        statusR, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(
                        statusR, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);
                break;

        }

        return updated;
    }

    protected ProvisioningResult<UserTO> doStatus(final StatusR statusR, final boolean nullPriorityAsync) {
        ProvisioningManager.ProvisioningResult<String> updated = setStatusOnWfAdapter(statusR, nullPriorityAsync);

        return afterUpdate(
                binder.getUserTO(updated.key()),
                updated.statuses(),
                List.of());
    }

    protected ProvisioningResult<UserTO> doDelete(
            final UserTO userTO, final boolean self, final boolean nullPriorityAsync) {

        BeforeResult<UserTO> before = beforeDelete(userTO);

        if (!self) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_DELETE),
                    before.key().getRealm());
            userDAO.securityChecks(
                    authRealms,
                    before.key().getKey(),
                    before.key().getRealm(),
                    groups(before.key()));
        }

        if (userDAO.isManager(before.key().getKey())) {
            throw SyncopeClientException.build(ClientExceptionType.Management);
        }

        List<PropagationStatus> statuses = provisioningManager.delete(
                before.key().getKey(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        UserTO deletedTO;
        if (userDAO.existsById(before.key().getKey())) {
            deletedTO = binder.getUserTO(before.key().getKey());
        } else {
            deletedTO = new UserTO();
            deletedTO.setKey(before.key().getKey());
        }

        return afterDelete(deletedTO, statuses, before.actions());
    }

    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        String key = null;

        if ("requestPasswordReset".equals(method.getName())) {
            key = userDAO.findKey((String) args[0]).orElse(null);
        } else if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string && SyncopeConstants.UUID_PATTERN.matcher(string).matches()) {
                    key = string;
                } else if (args[i] instanceof UserTO userTO) {
                    key = userTO.getKey();
                } else if (args[i] instanceof UserUR userUR) {
                    key = userUR.getKey();
                } else if (args[i] instanceof StatusR statusR) {
                    key = statusR.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getUserTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
