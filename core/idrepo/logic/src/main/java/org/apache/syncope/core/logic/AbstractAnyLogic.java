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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.logic.api.LogicActions;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public abstract class AbstractAnyLogic<TO extends AnyTO, C extends AnyCR, U extends AnyUR>
        extends AbstractResourceAssociator<TO> {

    protected static final String REST_CONTEXT = "REST";

    protected final RealmSearchDAO realmSearchDAO;

    protected final AnyTypeDAO anyTypeDAO;

    protected final TemplateUtils templateUtils;

    protected final Map<String, LogicActions> perContextActions = new ConcurrentHashMap<>();

    public AbstractAnyLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils) {

        this.realmSearchDAO = realmSearchDAO;
        this.anyTypeDAO = anyTypeDAO;
        this.templateUtils = templateUtils;
    }

    protected List<LogicActions> getActions(final Realm realm) {
        List<LogicActions> result = new ArrayList<>();

        realm.getActions().forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return result;
    }

    @SuppressWarnings("unchecked")
    protected Pair<C, List<LogicActions>> beforeCreate(final C input) {
        Realm realm = realmSearchDAO.findByFullPath(input.getRealm()).orElseThrow(() -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(input.getRealm());
            return sce;
        });

        AnyType anyType = null;
        if (input instanceof UserCR) {
            anyType = anyTypeDAO.getUser();
        } else if (input instanceof GroupCR) {
            anyType = anyTypeDAO.getGroup();
        } else if (input instanceof AnyObjectCR anyObjectCR) {
            anyType = anyTypeDAO.findById(anyObjectCR.getType()).orElse(null);
        }
        if (anyType == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            throw sce;
        }

        C templatedCR = input;

        realm.getTemplate(anyType).ifPresent(template -> templateUtils.apply(templatedCR, template.get()));

        C actionedCR = input;

        List<LogicActions> actions = getActions(realm);
        for (LogicActions action : actions) {
            actionedCR = action.beforeCreate(actionedCR);
        }

        LOG.debug("Input: {}\nOutput: {}\n", input, actionedCR);

        return Pair.of(actionedCR, actions);
    }

    @SuppressWarnings("unchecked")
    protected Pair<U, List<LogicActions>> beforeUpdate(final U input, final String realmPath) {
        Realm realm = realmSearchDAO.findByFullPath(realmPath).orElseThrow(() -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(realmPath);
            return sce;
        });

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
        Realm realm = realmSearchDAO.findByFullPath(input.getRealm()).orElseThrow(() -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            sce.getElements().add(input.getRealm());
            return sce;
        });

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

    public abstract TO read(String key);

    public abstract Page<TO> search(
            SearchCond searchCond,
            Pageable pageable,
            String realm,
            boolean recursive,
            boolean details);

    public abstract ProvisioningResult<TO> update(U updateReq, boolean nullPriorityAsync);

    public abstract ProvisioningResult<TO> delete(String key, boolean nullPriorityAsync);
}
