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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractAnyLogic<TO extends AnyTO, C extends AnyCR, U extends AnyUR>
        extends AbstractResourceAssociator<TO> {

    protected static final String REST_CONTEXT = "REST";

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private TemplateUtils templateUtils;

    private static List<LogicActions> getActions(final Realm realm) {
        List<LogicActions> actions = new ArrayList<>();

        realm.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return actions;
    }

    @SuppressWarnings("unchecked")
    protected Pair<C, List<LogicActions>> beforeCreate(final C input) {
        Realm realm = realmDAO.findByFullPath(input.getRealm());
        if (realm == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(input.getRealm());
            throw sce;
        }

        AnyType anyType = null;
        if (input instanceof UserCR) {
            anyType = anyTypeDAO.findUser();
        } else if (input instanceof GroupCR) {
            anyType = anyTypeDAO.findGroup();
        } else if (input instanceof AnyObjectCR) {
            anyType = anyTypeDAO.find(((AnyObjectCR) input).getType());
        }
        if (anyType == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            throw sce;
        }

        C anyCR = input;

        templateUtils.apply(anyCR, realm.getTemplate(anyType));

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            anyCR = action.beforeCreate(anyCR);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, anyCR);

        return Pair.of(anyCR, actions);
    }

    @SuppressWarnings("unchecked")
    protected Pair<U, List<LogicActions>> beforeUpdate(final U input, final String realmPath) {
        Realm realm = realmDAO.findByFullPath(realmPath);
        if (realm == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(realmPath);
            throw sce;
        }

        U update = input;

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            update = action.beforeUpdate(update);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, update);

        return Pair.of(update, actions);
    }

    @SuppressWarnings("unchecked")
    protected Pair<TO, List<LogicActions>> beforeDelete(final TO input) {
        Realm realm = realmDAO.findByFullPath(input.getRealm());
        if (realm == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(input.getRealm());
            throw sce;
        }

        TO any = input;

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            any = action.beforeDelete(any);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, any);

        return Pair.of(any, actions);
    }

    @SuppressWarnings("unchecked")
    protected ProvisioningResult<TO> afterCreate(
            final TO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

        TO any = input;

        for (LogicActions action : actions) {
            any = action.afterCreate(any, statuses);
        }

        ProvisioningResult<TO> result = new ProvisioningResult<>();
        result.setEntity(any);
        result.getPropagationStatuses().addAll(statuses);

        return result;
    }

    protected ProvisioningResult<TO> afterUpdate(
            final TO input,
            final List<PropagationStatus> statuses,
            final List<LogicActions> actions,
            final boolean authDynRealms,
            final Set<String> dynRealmsBefore) {

        Set<String> dynRealmsAfter = new HashSet<>(input.getDynRealms());
        if (authDynRealms && !dynRealmsBefore.equals(dynRealmsAfter)) {
            throw new DelegatedAdministrationException(
                    this instanceof UserLogic
                            ? AnyTypeKind.USER
                            : this instanceof GroupLogic
                                    ? AnyTypeKind.GROUP
                                    : AnyTypeKind.ANY_OBJECT,
                    input.getKey());
        }

        TO any = input;

        for (LogicActions action : actions) {
            any = action.afterUpdate(any, statuses);
        }

        ProvisioningResult<TO> result = new ProvisioningResult<>();
        result.setEntity(any);
        result.getPropagationStatuses().addAll(statuses);

        return result;
    }

    @SuppressWarnings("unchecked")
    protected ProvisioningResult<TO> afterDelete(
            final TO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

        TO any = input;

        for (LogicActions action : actions) {
            any = action.afterDelete(any, statuses);
        }

        ProvisioningResult<TO> result = new ProvisioningResult<>();
        result.setEntity(any);
        result.getPropagationStatuses().addAll(statuses);

        return result;
    }

    protected boolean securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        boolean authorized = effectiveRealms.stream().anyMatch(realm::startsWith);
        if (!authorized) {
            AnyDAO<?> anyDAO = this instanceof UserLogic
                    ? userDAO
                    : this instanceof GroupLogic
                            ? groupDAO
                            : anyObjectDAO;
            authorized = anyDAO.findDynRealms(key).stream().
                    anyMatch(effectiveRealms::contains);
        }
        if (!authorized) {
            throw new DelegatedAdministrationException(
                    realm,
                    (this instanceof UserLogic
                            ? AnyTypeKind.USER
                            : this instanceof GroupLogic
                                    ? AnyTypeKind.GROUP
                                    : AnyTypeKind.ANY_OBJECT).name(),
                    key);
        }

        return effectiveRealms.stream().anyMatch(new RealmUtils.DynRealmsPredicate());
    }

    public abstract TO read(String key);

    public abstract Pair<Integer, List<TO>> search(
            SearchCond searchCond,
            int page, int size, List<OrderByClause> orderBy,
            String realm,
            boolean details);

    public abstract ProvisioningResult<TO> update(U updateReq, boolean nullPriorityAsync);

    public abstract ProvisioningResult<TO> delete(String key, boolean nullPriorityAsync);
}
