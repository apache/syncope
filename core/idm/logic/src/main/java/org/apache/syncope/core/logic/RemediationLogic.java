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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RemediationLogic extends AbstractTransactionalLogic<RemediationTO> {

    @Autowired
    private UserLogic userLogic;

    @Autowired
    private GroupLogic groupLogic;

    @Autowired
    private AnyObjectLogic anyObjectLogic;

    @Autowired
    private RemediationDataBinder binder;

    @Autowired
    private RemediationDAO remediationDAO;

    @PreAuthorize("hasRole('" + IdMEntitlement.REMEDIATION_LIST + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<RemediationTO>> list(
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        int count = remediationDAO.count();

        List<RemediationTO> result = remediationDAO.findAll(page, size, orderByClauses).stream().
                map(binder::getRemediationTO).collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.REMEDIATION_READ + "')")
    @Transactional(readOnly = true)
    public RemediationTO read(final String key) {
        Remediation remediation = remediationDAO.find(key);
        if (remediation == null) {
            LOG.error("Could not find remediation '" + key + '\'');

            throw new NotFoundException(key);
        }

        return binder.getRemediationTO(remediation);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.REMEDIATION_DELETE + "')")
    public void delete(final String key) {
        Remediation remediation = remediationDAO.find(key);
        if (remediation == null) {
            LOG.error("Could not find remediation '" + key + '\'');

            throw new NotFoundException(key);
        }

        remediationDAO.delete(remediation);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.REMEDIATION_REMEDY + "')")
    public ProvisioningResult<?> remedy(final String key, final AnyCR anyCR, final boolean nullPriorityAsync) {
        Remediation remediation = remediationDAO.find(key);
        if (remediation == null) {
            LOG.error("Could not find remediation '" + key + '\'');

            throw new NotFoundException(key);
        }

        ProvisioningResult<?> result;
        switch (remediation.getAnyType().getKind()) {
            case USER:
            default:
                result = userLogic.create((UserCR) anyCR, nullPriorityAsync);
                break;

            case GROUP:
                result = groupLogic.create((GroupCR) anyCR, nullPriorityAsync);
                break;

            case ANY_OBJECT:
                result = anyObjectLogic.create((AnyObjectCR) anyCR, nullPriorityAsync);
        }

        remediationDAO.delete(remediation);

        return result;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.REMEDIATION_REMEDY + "')")
    public ProvisioningResult<?> remedy(final String key, final AnyUR anyUR, final boolean nullPriorityAsync) {
        Remediation remediation = remediationDAO.find(key);
        if (remediation == null) {
            LOG.error("Could not find remediation '" + key + '\'');

            throw new NotFoundException(key);
        }

        ProvisioningResult<?> result;
        switch (remediation.getAnyType().getKind()) {
            case USER:
            default:
                result = userLogic.update((UserUR) anyUR, nullPriorityAsync);
                break;

            case GROUP:
                result = groupLogic.update((GroupUR) anyUR, nullPriorityAsync);
                break;

            case ANY_OBJECT:
                result = anyObjectLogic.update((AnyObjectUR) anyUR, nullPriorityAsync);
        }

        remediationDAO.delete(remediation);

        return result;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.REMEDIATION_REMEDY + "')")
    public ProvisioningResult<?> remedy(final String key, final String anyKey, final boolean nullPriorityAsync) {
        Remediation remediation = remediationDAO.find(key);
        if (remediation == null) {
            LOG.error("Could not find remediation '" + key + '\'');

            throw new NotFoundException(key);
        }

        ProvisioningResult<?> result;
        switch (remediation.getAnyType().getKind()) {
            case USER:
            default:
                result = userLogic.delete(anyKey, nullPriorityAsync);
                break;

            case GROUP:
                result = groupLogic.delete(anyKey, nullPriorityAsync);
                break;

            case ANY_OBJECT:
                result = anyObjectLogic.delete(anyKey, nullPriorityAsync);
        }

        remediationDAO.delete(remediation);

        return result;
    }

    @Override
    protected RemediationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof RemediationTO) {
                    key = ((RemediationTO) args[i]).getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getRemediationTO(remediationDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
