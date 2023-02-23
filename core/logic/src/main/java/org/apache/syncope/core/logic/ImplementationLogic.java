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
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ImplementationLogic extends AbstractTransactionalLogic<ImplementationTO> {

    @Autowired
    private ImplementationDataBinder binder;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @PreAuthorize("hasRole('" + StandardEntitlement.IMPLEMENTATION_LIST + "')")
    @Transactional(readOnly = true)
    public List<ImplementationTO> list(final ImplementationType type) {
        return implementationDAO.find(type).stream().map(binder::getImplementationTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.IMPLEMENTATION_READ + "')")
    @Transactional(readOnly = true)
    public ImplementationTO read(final ImplementationType type, final String key) {
        Implementation implementation = implementationDAO.find(key);
        if (implementation == null) {
            LOG.error("Could not find implementation '" + key + "'");

            throw new NotFoundException(key);
        }

        if (implementation.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + implementation.getType());
            throw sce;
        }

        return binder.getImplementationTO(implementation);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.IMPLEMENTATION_CREATE + "')")
    public ImplementationTO create(final ImplementationTO implementationTO) {
        if (StringUtils.isBlank(implementationTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Implementation key");
            throw sce;
        }

        Implementation implementation = implementationDAO.find(implementationTO.getKey());
        if (implementation != null) {
            throw new DuplicateException(implementationTO.getKey());
        }

        return binder.getImplementationTO(implementationDAO.save(binder.create(implementationTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.IMPLEMENTATION_UPDATE + "')")
    public ImplementationTO update(final ImplementationTO implementationTO) {
        Implementation implementation = implementationDAO.find(implementationTO.getKey());
        if (implementation == null) {
            LOG.error("Could not find implementation '" + implementationTO.getKey() + "'");

            throw new NotFoundException(implementationTO.getKey());
        }

        binder.update(implementation, implementationTO);
        implementation = implementationDAO.save(implementation);

        return binder.getImplementationTO(implementation);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.IMPLEMENTATION_DELETE + "')")
    public void delete(final ImplementationType type, final String key) {
        Implementation implementation = implementationDAO.find(key);
        if (implementation == null) {
            LOG.error("Could not find implementation '" + key + "'");

            throw new NotFoundException(key);
        }

        if (implementation.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + implementation.getType());
            throw sce;
        }

        boolean inUse = false;
        switch (implementation.getType()) {
            case REPORTLET:
                inUse = !reportDAO.findByReportlet(implementation).isEmpty();
                break;

            case ACCOUNT_RULE:
                inUse = !policyDAO.findByAccountRule(implementation).isEmpty();
                break;

            case PASSWORD_RULE:
                inUse = !policyDAO.findByPasswordRule(implementation).isEmpty();
                break;

            case ITEM_TRANSFORMER:
                inUse = !resourceDAO.findByTransformer(implementation).isEmpty();
                break;

            case TASKJOB_DELEGATE:
                inUse = !taskDAO.findByDelegate(implementation).isEmpty();
                break;

            case RECON_FILTER_BUILDER:
                inUse = !taskDAO.findByReconFilterBuilder(implementation).isEmpty();
                break;

            case LOGIC_ACTIONS:
                inUse = !realmDAO.findByLogicActions(implementation).isEmpty();
                break;

            case PROVISION_SORTER:
                inUse = !resourceDAO.findByProvisionSorter(implementation).isEmpty();
                break;

            case PROPAGATION_ACTIONS:
                inUse = !resourceDAO.findByPropagationActions(implementation).isEmpty();
                break;

            case PULL_ACTIONS:
                inUse = !taskDAO.findByPullActions(implementation).isEmpty();
                break;

            case PUSH_ACTIONS:
                inUse = !taskDAO.findByPushActions(implementation).isEmpty();
                break;

            case PULL_CORRELATION_RULE:
                inUse = !policyDAO.findByPullCorrelationRule(implementation).isEmpty();
                break;

            case PUSH_CORRELATION_RULE:
                inUse = !policyDAO.findByPushCorrelationRule(implementation).isEmpty();
                break;

            case VALIDATOR:
                inUse = !plainSchemaDAO.findByValidator(implementation).isEmpty();
                break;

            case RECIPIENTS_PROVIDER:
                inUse = !notificationDAO.findByRecipientsProvider(implementation).isEmpty();
                break;

            default:
        }

        if (inUse) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InUse);
            sce.getElements().add("This implementation is in use");
            throw sce;
        }

        implementationDAO.delete(key);
    }

    @Override
    protected ImplementationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ImplementationTO) {
                    key = ((ImplementationTO) args[i]).getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getImplementationTO(implementationDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
