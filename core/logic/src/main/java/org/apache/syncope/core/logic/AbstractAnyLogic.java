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
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.spring.ImplementationManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractAnyLogic<TO extends AnyTO, P extends AnyPatch> extends AbstractResourceAssociator<TO> {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private TemplateUtils templateUtils;

    private List<LogicActions> getActions(final Realm realm) {
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

        return Pair.of(any, actions);
    }

    protected Pair<P, List<LogicActions>> beforeUpdate(final P input, final String realmPath) {
        Realm realm = realmDAO.findByFullPath(realmPath);
        if (realm == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(realmPath);
            throw sce;
        }

        P patch = input;

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            patch = action.beforeUpdate(patch);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, patch);

        return Pair.of(patch, actions);
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

        return Pair.of(any, actions);
    }

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
            final List<LogicActions> actions) {

        TO any = input;

        for (LogicActions action : actions) {
            any = action.afterUpdate(any, statuses);
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
            any = action.afterDelete(any, statuses);
        }

        ProvisioningResult<TO> result = new ProvisioningResult<>();
        result.setEntity(any);
        result.getPropagationStatuses().addAll(statuses);

        return result;
    }

    public abstract TO read(String key);

    public abstract Pair<Integer, List<TO>> search(
            SearchCond searchCond,
            int page, int size, List<OrderByClause> orderBy,
            String realm,
            boolean details);

    public abstract ProvisioningResult<TO> update(P anyPatch, boolean nullPriorityAsync);

    public abstract ProvisioningResult<TO> delete(String key, boolean nullPriorityAsync);
}
