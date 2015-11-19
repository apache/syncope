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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class RelationshipTypeLogic extends AbstractTransactionalLogic<RelationshipTypeTO> {

    @Autowired
    private RelationshipTypeDataBinder binder;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @PreAuthorize("hasRole('" + StandardEntitlement.RELATIONSHIPTYPE_READ + "')")
    public RelationshipTypeTO read(final String key) {
        RelationshipType relationshipType = relationshipTypeDAO.find(key);
        if (relationshipType == null) {
            LOG.error("Could not find relationshipType '" + key + "'");

            throw new NotFoundException(key);
        }

        return binder.getRelationshipTypeTO(relationshipType);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RELATIONSHIPTYPE_LIST + "')")
    public List<RelationshipTypeTO> list() {
        return CollectionUtils.collect(relationshipTypeDAO.findAll(),
                new Transformer<RelationshipType, RelationshipTypeTO>() {

            @Override
            public RelationshipTypeTO transform(final RelationshipType input) {
                return binder.getRelationshipTypeTO(input);
            }
        }, new ArrayList<RelationshipTypeTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RELATIONSHIPTYPE_CREATE + "')")
    public RelationshipTypeTO create(final RelationshipTypeTO relationshipTypeTO) {
        return binder.getRelationshipTypeTO(relationshipTypeDAO.save(binder.create(relationshipTypeTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RELATIONSHIPTYPE_UPDATE + "')")
    public RelationshipTypeTO update(final RelationshipTypeTO relationshipTypeTO) {
        RelationshipType relationshipType = relationshipTypeDAO.find(relationshipTypeTO.getKey());
        if (relationshipType == null) {
            LOG.error("Could not find relationshipType '" + relationshipTypeTO.getKey() + "'");
            throw new NotFoundException(relationshipTypeTO.getKey());
        }

        binder.update(relationshipType, relationshipTypeTO);
        relationshipType = relationshipTypeDAO.save(relationshipType);

        return binder.getRelationshipTypeTO(relationshipType);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RELATIONSHIPTYPE_DELETE + "')")
    public RelationshipTypeTO delete(final String key) {
        RelationshipType relationshipType = relationshipTypeDAO.find(key);
        if (relationshipType == null) {
            LOG.error("Could not find relationshipType '" + key + "'");

            throw new NotFoundException(key);
        }

        RelationshipTypeTO deleted = binder.getRelationshipTypeTO(relationshipType);

        relationshipTypeDAO.delete(key);

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
                } else if (args[i] instanceof RelationshipTypeTO) {
                    key = ((RelationshipTypeTO) args[i]).getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getRelationshipTypeTO(relationshipTypeDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
