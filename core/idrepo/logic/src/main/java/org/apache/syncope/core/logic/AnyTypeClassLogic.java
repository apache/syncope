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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class AnyTypeClassLogic extends AbstractTransactionalLogic<AnyTypeClassTO> {

    protected final AnyTypeClassDataBinder binder;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    public AnyTypeClassLogic(final AnyTypeClassDataBinder binder, final AnyTypeClassDAO anyTypeClassDAO) {
        this.binder = binder;
        this.anyTypeClassDAO = anyTypeClassDAO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANYTYPECLASS_READ + "')")
    @Transactional(readOnly = true)
    public AnyTypeClassTO read(final String key) {
        AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AnyTypeClass " + key));

        return binder.getAnyTypeClassTO(anyTypeClass);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANYTYPECLASS_LIST + "')")
    @Transactional(readOnly = true)
    public List<AnyTypeClassTO> list() {
        return anyTypeClassDAO.findAll().stream().map(binder::getAnyTypeClassTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANYTYPECLASS_CREATE + "')")
    public AnyTypeClassTO create(final AnyTypeClassTO anyTypeClassTO) {
        if (StringUtils.isBlank(anyTypeClassTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add(AnyTypeClass.class.getSimpleName() + " name");
            throw sce;
        }
        if (anyTypeClassDAO.existsById(anyTypeClassTO.getKey())) {
            throw new DuplicateException(anyTypeClassTO.getKey());
        }
        return binder.getAnyTypeClassTO(anyTypeClassDAO.save(binder.create(anyTypeClassTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANYTYPECLASS_UPDATE + "')")
    public AnyTypeClassTO update(final AnyTypeClassTO anyTypeClassTO) {
        AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(anyTypeClassTO.getKey()).
                orElseThrow(() -> new NotFoundException("AnyTypeClass " + anyTypeClassTO.getKey()));

        binder.update(anyTypeClass, anyTypeClassTO);
        anyTypeClass = anyTypeClassDAO.save(anyTypeClass);

        return binder.getAnyTypeClassTO(anyTypeClass);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANYTYPECLASS_DELETE + "')")
    public AnyTypeClassTO delete(final String key) {
        AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AnyTypeClass " + key));

        AnyTypeClassTO deleted = binder.getAnyTypeClassTO(anyTypeClass);
        anyTypeClassDAO.deleteById(key);
        return deleted;
    }

    @Override
    protected AnyTypeClassTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof AnyTypeClassTO anyTypeClassTO) {
                    key = anyTypeClassTO.getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getAnyTypeClassTO(anyTypeClassDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
