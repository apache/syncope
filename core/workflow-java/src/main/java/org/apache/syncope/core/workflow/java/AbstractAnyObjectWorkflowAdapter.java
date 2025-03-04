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
package org.apache.syncope.core.workflow.java;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public abstract class AbstractAnyObjectWorkflowAdapter
        extends AbstractWorkflowAdapter implements AnyObjectWorkflowAdapter {

    protected final AnyObjectDataBinder dataBinder;

    protected final AnyObjectDAO anyObjectDAO;

    public AbstractAnyObjectWorkflowAdapter(
            final AnyObjectDataBinder dataBinder,
            final AnyObjectDAO anyObjectDAO,
            final GroupDAO groupDAO,
            final EntityFactory entityFactory,
            final ApplicationEventPublisher publisher) {

        super(groupDAO, entityFactory, publisher);
        this.dataBinder = dataBinder;
        this.anyObjectDAO = anyObjectDAO;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    protected abstract WorkflowResult<String> doCreate(AnyObjectCR anyObjectCR, String creator, String context);

    @Override
    public WorkflowResult<String> create(final AnyObjectCR anyObjectCR, final String creator, final String context) {
        WorkflowResult<String> result = doCreate(anyObjectCR, creator, context);

        AnyObject anyObject = anyObjectDAO.findById(result.getResult()).
                orElseThrow(() -> new IllegalStateException("Could not find the AnyObject just created"));

        // finally publish events for all groups affected by this operation, via membership
        anyObject.getMemberships().forEach(m -> publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, m.getRightEnd(), AuthContextUtils.getDomain())));

        return result;
    }

    protected abstract WorkflowResult<AnyObjectUR> doUpdate(
            AnyObject anyObject, AnyObjectUR anyObjectUR, String updater, String context);

    @Override
    public WorkflowResult<AnyObjectUR> update(
            final AnyObjectUR anyObjectUR, final String updater, final String context) {

        WorkflowResult<AnyObjectUR> result = doUpdate(
                anyObjectDAO.authFind(anyObjectUR.getKey()), anyObjectUR, updater, context);

        AnyObject anyObject = anyObjectDAO.findById(anyObjectUR.getKey()).
                orElseThrow(() -> new IllegalStateException("Could not find the AnyObject just updated"));

        // ensure that requester's administration rights are still valid
        Set<String> authRealms = new HashSet<>();
        authRealms.addAll(AuthContextUtils.getAuthorizations().
                getOrDefault(AnyEntitlement.READ.getFor(anyObject.getType().getKey()), Collections.emptySet()));
        authRealms.addAll(AuthContextUtils.getAuthorizations().
                getOrDefault(AnyEntitlement.UPDATE.getFor(anyObject.getType().getKey()), Collections.emptySet()));
        anyObjectDAO.securityChecks(
                authRealms,
                anyObject.getKey(),
                anyObject.getRealm().getFullPath(),
                anyObjectDAO.findAllGroupKeys(anyObject));

        // finally publish events for all groups affected by this operation, via membership
        result.getResult().getMemberships().stream().map(MembershipUR::getGroup).distinct().
                map(groupDAO::findById).flatMap(Optional::stream).
                forEach(group -> publisher.publishEvent(new EntityLifecycleEvent<>(
                this, SyncDeltaType.UPDATE, group, AuthContextUtils.getDomain())));

        return result;
    }

    protected abstract void doDelete(AnyObject anyObject, String eraser, String context);

    @Override
    public void delete(final String anyObjectKey, final String eraser, final String context) {
        AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);

        Set<Group> groups = anyObject.getMemberships().stream().
                map(AMembership::getRightEnd).collect(Collectors.toSet());

        doDelete(anyObject, eraser, context);

        // finally publish events for all groups affected by this operation, via membership
        groups.forEach(group -> publisher.publishEvent(new EntityLifecycleEvent<>(
                this, SyncDeltaType.UPDATE, group, AuthContextUtils.getDomain())));
    }
}
