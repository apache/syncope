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
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class AttrRepoLogic extends AbstractTransactionalLogic<AttrRepoTO> {

    protected final AttrRepoDataBinder binder;

    protected final AttrRepoDAO attrRepoDAO;

    public AttrRepoLogic(final AttrRepoDataBinder binder, final AttrRepoDAO attrRepoDAO) {
        this.binder = binder;
        this.attrRepoDAO = attrRepoDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_CREATE + "')")
    public AttrRepoTO create(final AttrRepoTO attrRepoTO) {
        return binder.getAttrRepoTO(attrRepoDAO.save(binder.create(attrRepoTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_UPDATE + "')")
    public AttrRepoTO update(final AttrRepoTO attrRepoTO) {
        AttrRepo attrRepo = attrRepoDAO.findById(attrRepoTO.getKey()).
                orElseThrow(() -> new NotFoundException("AttrRepo " + attrRepoTO.getKey()));

        return binder.getAttrRepoTO(attrRepoDAO.save(binder.update(attrRepo, attrRepoTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<AttrRepoTO> list() {
        return attrRepoDAO.findAll().stream().map(binder::getAttrRepoTO).toList();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_READ + "')")
    @Transactional(readOnly = true)
    public AttrRepoTO read(final String key) {
        AttrRepo attrRepo = attrRepoDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AttrRepo " + key));

        return binder.getAttrRepoTO(attrRepo);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_DELETE + "')")
    public AttrRepoTO delete(final String key) {
        AttrRepo attrRepo = attrRepoDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AttrRepo " + key));

        AttrRepoTO deleted = binder.getAttrRepoTO(attrRepo);
        attrRepoDAO.delete(attrRepo);

        return deleted;
    }

    @Override
    protected AttrRepoTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        if (ArrayUtils.isEmpty(args)) {
            throw new UnresolvedReferenceException();
        }

        final String key;

        if (args[0] instanceof String string) {
            key = string;
        } else if (args[0] instanceof AttrRepoTO attrRepoTO) {
            key = attrRepoTO.getKey();
        } else {
            throw new UnresolvedReferenceException();
        }

        try {
            return binder.getAttrRepoTO(attrRepoDAO.findById(key).orElseThrow());
        } catch (Throwable ignore) {
            LOG.debug("Unresolved reference", ignore);
            throw new UnresolvedReferenceException(ignore);
        }
    }
}
