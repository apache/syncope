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
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.provisioning.api.data.DomainDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DomainLogic extends AbstractTransactionalLogic<DomainTO> {

    @Autowired
    private DomainsHolder domainsHolder;

    @Autowired
    private DomainDataBinder binder;

    @Autowired
    private DomainDAO domainDAO;

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DOMAIN_READ + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public DomainTO read(final String key) {
        Domain domain = domainDAO.find(key);
        if (domain == null) {
            LOG.error("Could not find domain '" + key + "'");

            throw new NotFoundException(key);
        }

        return binder.getDomainTO(domain);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<DomainTO> list() {
        return domainDAO.findAll().stream().map(binder::getDomainTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DOMAIN_CREATE + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public DomainTO create(final DomainTO domainTO) {
        if (!domainsHolder.getDomains().keySet().contains(domainTO.getKey())) {
            throw new NotFoundException("No configuration is available for domain " + domainTO.getKey());
        }

        return binder.getDomainTO(domainDAO.save(binder.create(domainTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DOMAIN_UPDATE + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public DomainTO update(final DomainTO domainTO) {
        Domain domain = domainDAO.find(domainTO.getKey());
        if (domain == null) {
            LOG.error("Could not find domain '" + domainTO.getKey() + "'");
            throw new NotFoundException(domainTO.getKey());
        }

        binder.update(domain, domainTO);
        domain = domainDAO.save(domain);

        return binder.getDomainTO(domain);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DOMAIN_DELETE + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public DomainTO delete(final String key) {
        Domain domain = domainDAO.find(key);
        if (domain == null) {
            LOG.error("Could not find domain '" + key + "'");

            throw new NotFoundException(key);
        }

        DomainTO deleted = binder.getDomainTO(domain);
        domainDAO.delete(key);
        return deleted;
    }

    @Override
    protected DomainTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof DomainTO) {
                    key = ((DomainTO) args[i]).getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getDomainTO(domainDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
