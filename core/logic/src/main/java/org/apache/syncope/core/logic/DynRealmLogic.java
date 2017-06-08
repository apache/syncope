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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class DynRealmLogic extends AbstractTransactionalLogic<DynRealmTO> {

    @Autowired
    private DynRealmDataBinder binder;

    @Autowired
    private DynRealmDAO dynRealmDAO;

    @PreAuthorize("hasRole('" + StandardEntitlement.DYNREALM_READ + "')")
    public DynRealmTO read(final String key) {
        DynRealm dynRealm = dynRealmDAO.find(key);
        if (dynRealm == null) {
            LOG.error("Could not find dynamic realm '" + key + "'");

            throw new NotFoundException(key);
        }

        return binder.getDynRealmTO(dynRealm);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.DYNREALM_LIST + "')")
    public List<DynRealmTO> list() {
        return CollectionUtils.collect(dynRealmDAO.findAll(), new Transformer<DynRealm, DynRealmTO>() {

            @Override
            public DynRealmTO transform(final DynRealm input) {
                return binder.getDynRealmTO(input);
            }
        }, new ArrayList<DynRealmTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.DYNREALM_CREATE + "')")
    public DynRealmTO create(final DynRealmTO dynRealmTO) {
        return binder.getDynRealmTO(dynRealmDAO.save(binder.create(dynRealmTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.DYNREALM_UPDATE + "')")
    public DynRealmTO update(final DynRealmTO dynRealmTO) {
        DynRealm dynRealm = dynRealmDAO.find(dynRealmTO.getKey());
        if (dynRealm == null) {
            LOG.error("Could not find dynamic realm '" + dynRealmTO.getKey() + "'");
            throw new NotFoundException(dynRealmTO.getKey());
        }

        return binder.getDynRealmTO(dynRealmDAO.save(binder.update(dynRealm, dynRealmTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.DYNREALM_DELETE + "')")
    public DynRealmTO delete(final String key) {
        DynRealm dynRealm = dynRealmDAO.find(key);
        if (dynRealm == null) {
            LOG.error("Could not find dynamic realm '" + key + "'");

            throw new NotFoundException(key);
        }

        DynRealmTO deleted = binder.getDynRealmTO(dynRealm);
        dynRealmDAO.delete(key);
        return deleted;
    }

    @Override
    protected DynRealmTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof DynRealmTO) {
                    key = ((DynRealmTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getDynRealmTO(dynRealmDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
