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
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AnyTypeLogic extends AbstractTransactionalLogic<AnyTypeTO> {

    @Autowired
    private AnyTypeDataBinder binder;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @PreAuthorize("hasRole('" + StandardEntitlement.ANYTYPE_READ + "')")
    @Transactional(readOnly = true)
    public AnyTypeTO read(final String key) {
        AnyType anyType = anyTypeDAO.find(key);
        if (anyType == null) {
            LOG.error("Could not find anyType '" + key + "'");

            throw new NotFoundException(key);
        }

        return binder.getAnyTypeTO(anyType);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ANYTYPE_LIST + "')")
    @Transactional(readOnly = true)
    public List<AnyTypeTO> list() {
        return anyTypeDAO.findAll().stream().map(anyType -> binder.getAnyTypeTO(anyType)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ANYTYPE_CREATE + "')")
    public AnyTypeTO create(final AnyTypeTO anyTypeTO) {
        if (StringUtils.isBlank(anyTypeTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add(AnyType.class.getSimpleName() + " key");
            throw sce;
        }
        if (anyTypeDAO.find(anyTypeTO.getKey()) != null) {
            throw new DuplicateException(anyTypeTO.getKey());
        }

        return binder.getAnyTypeTO(anyTypeDAO.save(binder.create(anyTypeTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ANYTYPE_UPDATE + "')")
    public AnyTypeTO update(final AnyTypeTO anyTypeTO) {
        AnyType anyType = anyTypeDAO.find(anyTypeTO.getKey());
        if (anyType == null) {
            LOG.error("Could not find anyType '" + anyTypeTO.getKey() + "'");
            throw new NotFoundException(anyTypeTO.getKey());
        }

        binder.update(anyType, anyTypeTO);
        anyType = anyTypeDAO.save(anyType);

        return binder.getAnyTypeTO(anyTypeDAO.save(anyType));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ANYTYPE_DELETE + "')")
    public AnyTypeTO delete(final String key) {
        AnyType anyType = anyTypeDAO.find(key);
        if (anyType == null) {
            LOG.error("Could not find anyType '" + key + "'");

            throw new NotFoundException(key);
        }

        try {
            return binder.delete(anyType);
        } catch (IllegalArgumentException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @Override
    protected AnyTypeTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AnyTypeTO) {
                    key = ((AnyTypeTO) args[i]).getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getAnyTypeTO(anyTypeDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
