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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ReconciliationRequest;
import org.apache.syncope.common.lib.to.ReconciliationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationLogic extends AbstractTransactionalLogic<AbstractBaseBean> {

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private ConnectorFactory connFactory;

    @Autowired
    private SyncopeSinglePullExecutor singlePullExecutor;

    @Autowired
    private SyncopeSinglePushExecutor singlePushExecutor;

    @SuppressWarnings("unchecked")
    private Pair<Any<?>, Provision> init(final AnyTypeKind anyTypeKind, final String anyKey, final String resourceKey) {
        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyTypeKind);

        Any<?> any = anyUtils.dao().authFind(anyKey);
        if (any == null) {
            throw new NotFoundException(anyTypeKind + " '" + anyKey + "'");
        }

        ExternalResource resource = resourceDAO.find(resourceKey);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceKey + "'");
        }
        Provision provision = resource.getProvision(any.getType());
        if (provision == null) {
            throw new NotFoundException("Provision for " + any.getType() + " on Resource '" + resourceKey + "'");
        }
        if (provision.getMapping() == null) {
            throw new NotFoundException("Mapping for " + any.getType() + " on Resource '" + resourceKey + "'");
        }

        return (Pair<Any<?>, Provision>) Pair.of(any, provision);
    }

    private ConnObjectTO getOnSyncope(final Any<?> any, final Provision provision, final String resourceKey) {
        Pair<String, Set<Attribute>> attrs = mappingManager.prepareAttrs(any, null, false, true, provision);

        MappingItem connObjectKey = provision.getMapping().getConnObjectKeyItem();
        if (connObjectKey == null) {
            throw new NotFoundException("No RemoteKey set for " + resourceKey);
        }

        ConnObjectTO connObjectTO = ConnObjectUtils.getConnObjectTO(attrs.getRight());
        if (attrs.getLeft() != null) {
            connObjectTO.getAttrs().add(new AttrTO.Builder().
                    schema(connObjectKey.getExtAttrName()).value(attrs.getLeft()).build());
            connObjectTO.getAttrs().add(new AttrTO.Builder().
                    schema(Uid.NAME).value(attrs.getLeft()).build());
        }

        return connObjectTO;
    }

    private ConnObjectTO getOnResource(final Any<?> any, final Provision provision) {
        // 1. build connObjectKeyItem
        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
        if (connObjectKeyItem == null) {
            throw new NotFoundException("ConnObjectKey for " + any.getType()
                    + " on resource '" + provision.getResource().getKey() + "'");
        }
        String connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, provision);

        // 2. determine attributes to query
        Set<MappingItem> linkinMappingItems = new HashSet<>();
        for (VirSchema virSchema : virSchemaDAO.findByProvision(provision)) {
            linkinMappingItems.add(virSchema.asLinkingMappingItem());
        }
        Iterator<MappingItem> mapItems = IteratorUtils.chainedIterator(
                provision.getMapping().getItems().iterator(),
                linkinMappingItems.iterator());

        // 3. read from the underlying connector
        ConnObjectTO connObjectTO = null;

        Connector connector = connFactory.getConnector(provision.getResource());
        ConnectorObject connectorObject = connector.getObject(
                provision.getObjectClass(),
                AttributeBuilder.build(connObjectKeyItem.getExtAttrName(), connObjectKeyValue),
                MappingUtils.buildOperationOptions(mapItems));
        if (connectorObject != null) {
            Set<Attribute> attributes = connectorObject.getAttributes();
            if (AttributeUtil.find(Uid.NAME, attributes) == null) {
                attributes.add(connectorObject.getUid());
            }
            if (AttributeUtil.find(Name.NAME, attributes) == null) {
                attributes.add(connectorObject.getName());
            }

            connObjectTO = ConnObjectUtils.getConnObjectTO(attributes);
        }

        return connObjectTO;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    public ReconciliationStatus status(final AnyTypeKind anyTypeKind, final String anyKey, final String resourceKey) {
        Pair<Any<?>, Provision> init = init(anyTypeKind, anyKey, resourceKey);

        ReconciliationStatus status = new ReconciliationStatus();
        status.setOnSyncope(getOnSyncope(init.getLeft(), init.getRight(), resourceKey));
        status.setOnResource(getOnResource(init.getLeft(), init.getRight()));

        return status;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    public void reconcile(final ReconciliationRequest request) {
        Pair<Any<?>, Provision> init = init(request.getAnyTypeKind(), request.getAnyKey(), request.getResourceKey());

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        try {
            List<ProvisioningReport> results = null;
            switch (request.getAction()) {
                case PUSH:
                    results = singlePushExecutor.push(
                            init.getRight(),
                            connFactory.getConnector(init.getRight().getResource()),
                            init.getLeft(),
                            request.getActionsClassNames());
                    break;

                case PULL:
                    results = singlePullExecutor.pull(
                            init.getRight(),
                            connFactory.getConnector(init.getRight().getResource()),
                            init.getRight().getMapping().getConnObjectKeyItem().getExtAttrName(),
                            mappingManager.getConnObjectKeyValue(init.getLeft(), init.getRight()),
                            realmDAO.findByFullPath(init.getLeft().getRealm().getFullPath()),
                            request.getActionsClassNames());
                    break;

                default:
            }

            if (results != null && !results.isEmpty()
                    && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {

                sce.getElements().add(results.get(0).getMessage());
            }
        } catch (JobExecutionException e) {
            sce.getElements().add(e.getMessage());
        }

        if (!sce.isEmpty()) {
            throw sce;
        }
    }

    @Override
    protected AbstractBaseBean resolveReference(final Method method, final Object... os)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
