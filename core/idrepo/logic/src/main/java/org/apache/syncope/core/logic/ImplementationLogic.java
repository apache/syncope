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
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ImplementationLogic extends AbstractTransactionalLogic<ImplementationTO> {

    protected final ImplementationDataBinder binder;

    protected final ImplementationDAO implementationDAO;

    protected final ReportDAO reportDAO;

    protected final PolicyDAO policyDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final TaskDAO taskDAO;

    protected final RealmDAO realmDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final NotificationDAO notificationDAO;

    public ImplementationLogic(
            final ImplementationDataBinder binder,
            final ImplementationDAO implementationDAO,
            final ReportDAO reportDAO,
            final PolicyDAO policyDAO,
            final ExternalResourceDAO resourceDAO,
            final TaskDAO taskDAO,
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final NotificationDAO notificationDAO) {

        this.binder = binder;
        this.implementationDAO = implementationDAO;
        this.reportDAO = reportDAO;
        this.policyDAO = policyDAO;
        this.resourceDAO = resourceDAO;
        this.taskDAO = taskDAO;
        this.realmDAO = realmDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.notificationDAO = notificationDAO;
    }

    protected void checkType(final String type) {
        if (!ImplementationTypesHolder.getInstance().getValues().containsKey(type)) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementationType);
            sce.getElements().add("Implementation type not found: ");
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_LIST + "')")
    @Transactional(readOnly = true)
    public List<ImplementationTO> list(final String type) {
        checkType(type);

        return implementationDAO.findByType(type).stream().
                map(binder::getImplementationTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_READ + "')")
    @Transactional(readOnly = true)
    public ImplementationTO read(final String type, final String key) {
        checkType(type);

        Implementation implementation = implementationDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Implementation " + key));

        if (!implementation.getType().equals(type)) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + implementation.getType());
            throw sce;
        }

        return binder.getImplementationTO(implementation);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_CREATE + "')")
    public ImplementationTO create(final ImplementationTO implementationTO) {
        if (StringUtils.isBlank(implementationTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Implementation key");
            throw sce;
        }

        checkType(implementationTO.getType());

        if (implementationDAO.existsById(implementationTO.getKey())) {
            throw new DuplicateException(implementationTO.getKey());
        }

        return binder.getImplementationTO(implementationDAO.save(binder.create(implementationTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_UPDATE + "')")
    public ImplementationTO update(final ImplementationTO implementationTO) {
        Implementation implementation = implementationDAO.findById(implementationTO.getKey()).
                orElseThrow(() -> new NotFoundException("Implementation " + implementationTO.getKey()));

        checkType(implementationTO.getType());

        binder.update(implementation, implementationTO);
        implementation = implementationDAO.save(implementation);

        return binder.getImplementationTO(implementation);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_DELETE + "')")
    public void delete(final String type, final String key) {
        Implementation implementation = implementationDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Implementation " + key));

        if (!implementation.getType().equals(type)) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + implementation.getType());
            throw sce;
        }

        boolean inUse = false;
        switch (implementation.getType()) {
            case IdRepoImplementationType.ACCOUNT_RULE:
                inUse = !policyDAO.findByAccountRule(implementation).isEmpty();
                break;

            case IdRepoImplementationType.PASSWORD_RULE:
                inUse = !policyDAO.findByPasswordRule(implementation).isEmpty();
                break;

            case IdRepoImplementationType.ITEM_TRANSFORMER:
                inUse = resourceDAO.anyItemHaving(implementation);
                break;

            case IdRepoImplementationType.TASKJOB_DELEGATE:
                inUse = !taskDAO.findByDelegate(implementation).isEmpty();
                break;

            case IdRepoImplementationType.REPORT_DELEGATE:
                inUse = !reportDAO.findByJobDelegate(implementation).isEmpty();
                break;

            case IdRepoImplementationType.COMMAND:
                inUse = !taskDAO.findByCommand(implementation).isEmpty();
                break;

            case IdMImplementationType.RECON_FILTER_BUILDER:
                inUse = !taskDAO.findByReconFilterBuilder(implementation).isEmpty();
                break;

            case IdRepoImplementationType.LOGIC_ACTIONS:
                inUse = !realmDAO.findByActionsContaining(implementation).isEmpty();
                break;

            case IdMImplementationType.PROVISION_SORTER:
                inUse = !resourceDAO.findByProvisionSorter(implementation).isEmpty();
                break;

            case IdMImplementationType.PROPAGATION_ACTIONS:
                inUse = !resourceDAO.findByPropagationActionsContaining(implementation).isEmpty();
                break;

            case IdMImplementationType.INBOUND_ACTIONS:
                inUse = !taskDAO.findByInboundActions(implementation).isEmpty();
                break;

            case IdMImplementationType.PUSH_ACTIONS:
                inUse = !taskDAO.findByPushActions(implementation).isEmpty();
                break;

            case IdMImplementationType.INBOUND_CORRELATION_RULE:
                inUse = !policyDAO.findByInboundCorrelationRule(implementation).isEmpty();
                break;

            case IdMImplementationType.PUSH_CORRELATION_RULE:
                inUse = !policyDAO.findByPushCorrelationRule(implementation).isEmpty();
                break;

            case IdRepoImplementationType.ATTR_VALUE_VALIDATOR:
                inUse = !plainSchemaDAO.findByValidator(implementation).isEmpty();
                break;

            case IdRepoImplementationType.RECIPIENTS_PROVIDER:
                inUse = !notificationDAO.findByRecipientsProvider(implementation).isEmpty();
                break;

            default:
        }

        if (inUse) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InUse);
            sce.getElements().add("This implementation is in use");
            throw sce;
        }

        implementationDAO.deleteById(key);
    }

    @Override
    protected ImplementationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof ImplementationTO implementationTO) {
                    key = implementationTO.getKey();
                }
            }
        }

        if (StringUtils.isNotBlank(key)) {
            try {
                return binder.getImplementationTO(implementationDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
