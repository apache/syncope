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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.WAConfigTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.auth.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WAConfigLogic extends AbstractTransactionalLogic<WAConfigTO> {
    @Autowired
    private WAConfigDataBinder binder;

    @Autowired
    private WAConfigDAO configDAO;

    @Override
    protected WAConfigTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof WAConfigTO) {
                    key = ((WAConfigTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getConfigTO(configDAO.find(key));
            } catch (final Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }

        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<WAConfigTO> list() {
        return configDAO.findAll().stream().map(binder::getConfigTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_UPDATE + "')")
    public void update(final WAConfigTO configTO) {
        WAConfigEntry entry = configDAO.find(configTO.getKey());
        if (entry == null) {
            throw new NotFoundException("Configuration entry " + configTO.getKey() + " not found");
        }
        binder.update(entry, configTO);
        configDAO.save(entry);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_DELETE + "')")
    public void delete(final String key) {
        configDAO.delete(key);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_READ + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAConfigTO get(final String name) {
        WAConfigEntry entry = configDAO.findByName(name);
        if (entry == null) {
            throw new NotFoundException("Configuration entry " + name + " not found");
        }
        return binder.getConfigTO(entry);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_READ + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAConfigTO read(final String key) {
        WAConfigEntry entry = configDAO.find(key);
        if (entry == null) {
            throw new NotFoundException("Configuration entry " + key + " not found");
        }
        return binder.getConfigTO(entry);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_CREATE + "')")
    public WAConfigTO create(final WAConfigTO configTO) {
        return binder.getConfigTO(binder.create(configTO));
    }
}
