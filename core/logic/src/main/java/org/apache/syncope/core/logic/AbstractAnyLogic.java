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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.ApplicationContextProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public abstract class AbstractAnyLogic<TO extends AnyTO, P extends AnyPatch> extends AbstractResourceAssociator<TO> {

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

    private List<LogicActions> getActions(final Realm realm) {
        List<LogicActions> actions = new ArrayList<>();

        realm.getActionsClassNames().forEach(className -> {
            try {
                Class<?> actionsClass = Class.forName(className);
                LogicActions logicActions = (LogicActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

                actions.add(logicActions);
            } catch (Exception e) {
                LOG.warn("Class '{}' not found", className, e);
            }
        });

        return actions;
    }

    protected Pair<TO, List<LogicActions>> beforeCreate(final TO input) {
        Realm realm = realmDAO.findByFullPath(input.getRealm());
        if (realm == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(input.getRealm());
            throw sce;
        }

        AnyType anyType = input instanceof UserTO
                ? anyTypeDAO.findUser()
                : input instanceof GroupTO
                        ? anyTypeDAO.findGroup()
                        : anyTypeDAO.find(input.getType());
        if (anyType == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(input.getType());
            throw sce;
        }

        TO any = input;

        templateUtils.apply(any, realm.getTemplate(anyType));

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            any = action.beforeCreate(any);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, any);

        return ImmutablePair.of(any, actions);
    }

    protected Pair<P, List<LogicActions>> beforeUpdate(final P input, final String realmPath) {
        Realm realm = realmDAO.findByFullPath(realmPath);
        if (realm == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(realmPath);
            throw sce;
        }

        P mod = input;

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            mod = action.beforeUpdate(mod);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, mod);

        return ImmutablePair.of(mod, actions);
    }

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

        return ImmutablePair.of(any, actions);
    }

    protected ProvisioningResult<TO> afterCreate(
            final TO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

        TO any = input;

        for (LogicActions action : actions) {
            any = action.afterCreate(any);
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
            any = action.afterUpdate(any);
        }

        ProvisioningResult<TO> result = new ProvisioningResult<>();
        result.setEntity(any);
        result.getPropagationStatuses().addAll(statuses);

        return result;
    }

    protected ProvisioningResult<TO> afterDelete(
            final TO input, final List<PropagationStatus> statuses, final List<LogicActions> actions) {

        TO any = input;

        for (LogicActions action : actions) {
            any = action.afterDelete(any);
        }

        ProvisioningResult<TO> result = new ProvisioningResult<>();
        result.setEntity(any);
        result.getPropagationStatuses().addAll(statuses);

        return result;
    }

    protected boolean securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        boolean authorized = effectiveRealms.stream().anyMatch(ownedRealm -> realm.startsWith(ownedRealm));
        if (!authorized) {
            AnyDAO<?> anyDAO = this instanceof UserLogic
                    ? userDAO
                    : this instanceof GroupLogic
                            ? groupDAO
                            : anyObjectDAO;
            authorized = anyDAO.findDynRealms(key).stream().
                    filter(dynRealm -> effectiveRealms.contains(dynRealm)).findFirst().isPresent();
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

    public abstract ProvisioningResult<TO> create(TO anyTO, boolean nullPriorityAsync);

    public abstract ProvisioningResult<TO> update(P anyPatch, boolean nullPriorityAsync);

    public abstract ProvisioningResult<TO> delete(String key, boolean nullPriorityAsync);

    public abstract Pair<Integer, List<TO>> search(
            SearchCond searchCond,
            int page, int size, List<OrderByClause> orderBy,
            String realm,
            boolean details);
}
