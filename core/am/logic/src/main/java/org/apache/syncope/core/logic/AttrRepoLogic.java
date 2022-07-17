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
        AttrRepo attrRepo = attrRepoDAO.find(attrRepoTO.getKey());
        if (attrRepo == null) {
            throw new NotFoundException("AttrRepo " + attrRepoTO.getKey() + " not found");
        }

        return binder.getAttrRepoTO(attrRepoDAO.save(binder.update(attrRepo, attrRepoTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<AttrRepoTO> list() {
        return attrRepoDAO.findAll().stream().map(binder::getAttrRepoTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_READ + "')")
    @Transactional(readOnly = true)
    public AttrRepoTO read(final String key) {
        AttrRepo attrRepo = attrRepoDAO.find(key);
        if (attrRepo == null) {
            throw new NotFoundException("AttrRepo " + key + " not found");
        }

        return binder.getAttrRepoTO(attrRepo);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.ATTR_REPO_DELETE + "')")
    public AttrRepoTO delete(final String key) {
        AttrRepo attrRepo = attrRepoDAO.find(key);
        if (attrRepo == null) {
            throw new NotFoundException("AttrRepo " + key + " not found");
        }

        AttrRepoTO deleted = binder.getAttrRepoTO(attrRepo);
        attrRepoDAO.delete(attrRepo);

        return deleted;
    }

    @Override
    protected AttrRepoTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AttrRepoTO) {
                    key = ((AttrRepoTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAttrRepoTO(attrRepoDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
