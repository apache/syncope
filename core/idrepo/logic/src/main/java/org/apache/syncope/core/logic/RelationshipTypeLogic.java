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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF RELATIONSHIP
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class RelationshipTypeLogic extends AbstractTransactionalLogic<RelationshipTypeTO> {

    protected final RelationshipTypeDataBinder binder;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final ApplicationEventPublisher publisher;

    public RelationshipTypeLogic(
            final RelationshipTypeDataBinder binder,
            final RelationshipTypeDAO relationshipTypeDAO,
            final ApplicationEventPublisher publisher) {

        this.binder = binder;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.publisher = publisher;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.RELATIONSHIPTYPE_READ + "')")
    @Transactional(readOnly = true)
    public RelationshipTypeTO read(final String key) {
        RelationshipType relationshipType = relationshipTypeDAO.findById(key).
                orElseThrow(() -> new NotFoundException("RelationshipType " + key));

        return binder.getRelationshipTypeTO(relationshipType);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.RELATIONSHIPTYPE_LIST + "')")
    @Transactional(readOnly = true)
    public List<RelationshipTypeTO> list() {
        return relationshipTypeDAO.findAll().stream().map(binder::getRelationshipTypeTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.RELATIONSHIPTYPE_CREATE + "')")
    public RelationshipTypeTO create(final RelationshipTypeTO relationshipTypeTO) {
        RelationshipType relationshipType = relationshipTypeDAO.save(binder.create(relationshipTypeTO));

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.CREATE, relationshipType, AuthContextUtils.getDomain()));

        return binder.getRelationshipTypeTO(relationshipType);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.RELATIONSHIPTYPE_UPDATE + "')")
    public RelationshipTypeTO update(final RelationshipTypeTO relationshipTypeTO) {
        RelationshipType relationshipType = relationshipTypeDAO.findById(relationshipTypeTO.getKey()).
                orElseThrow(() -> new NotFoundException("RelationshipType " + relationshipTypeTO.getKey()));

        binder.update(relationshipType, relationshipTypeTO);
        relationshipType = relationshipTypeDAO.save(relationshipType);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, relationshipType, AuthContextUtils.getDomain()));

        return binder.getRelationshipTypeTO(relationshipType);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.RELATIONSHIPTYPE_DELETE + "')")
    public RelationshipTypeTO delete(final String key) {
        RelationshipType relationshipType = relationshipTypeDAO.findById(key).
                orElseThrow(() -> new NotFoundException("RelationshipType " + key));

        RelationshipTypeTO deleted = binder.getRelationshipTypeTO(relationshipType);

        relationshipTypeDAO.deleteById(key);

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, relationshipType, AuthContextUtils.getDomain()));

        return deleted;
    }

    @Override
    protected RelationshipTypeTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (String) args[i];
                } else if (args[i] instanceof RelationshipTypeTO relationshipTypeTO) {
                    key = relationshipTypeTO.getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getRelationshipTypeTO(relationshipTypeDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
