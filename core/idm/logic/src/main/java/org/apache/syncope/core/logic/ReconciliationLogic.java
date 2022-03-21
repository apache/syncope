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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.beans.AbstractCSVSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.pushpull.ConstantReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.KeyValueReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.stream.CSVStreamConnector;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.java.pushpull.SinglePullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.SinglePushJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.stream.StreamPullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.stream.StreamPushJobDelegate;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.quartz.JobExecutionException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.transaction.annotation.Transactional;

public class ReconciliationLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final AnyTypeDAO anyTypeDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final RealmDAO realmDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final VirAttrHandler virAttrHandler;

    protected final MappingManager mappingManager;

    protected final InboundMatcher inboundMatcher;

    protected final OutboundMatcher outboundMatcher;

    protected final ConnectorManager connectorManager;

    public ReconciliationLogic(
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final ExternalResourceDAO resourceDAO,
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnySearchDAO anySearchDAO,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final InboundMatcher inboundMatcher,
            final OutboundMatcher outboundMatcher,
            final ConnectorManager connectorManager) {

        this.anyUtilsFactory = anyUtilsFactory;
        this.anyTypeDAO = anyTypeDAO;
        this.resourceDAO = resourceDAO;
        this.realmDAO = realmDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.anySearchDAO = anySearchDAO;
        this.virAttrHandler = virAttrHandler;
        this.mappingManager = mappingManager;
        this.inboundMatcher = inboundMatcher;
        this.outboundMatcher = outboundMatcher;
        this.connectorManager = connectorManager;
    }

    protected Provision getProvision(final String anyTypeKey, final String resourceKey) {
        AnyType anyType = anyTypeDAO.find(anyTypeKey);
        if (anyType == null) {
            throw new NotFoundException("AnyType '" + anyTypeKey + "'");
        }

        ExternalResource resource = resourceDAO.find(resourceKey);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceKey + "'");
        }
        Provision provision = resource.getProvision(anyType).
                orElseThrow(() -> new NotFoundException(
                "Provision for " + anyType + " on Resource '" + resourceKey + "'"));
        if (provision.getMapping() == null) {
            throw new NotFoundException("Mapping for " + anyType + " on Resource '" + resourceKey + "'");
        }

        return provision;
    }

    protected ConnObjectTO getOnSyncope(
            final MappingItem connObjectKeyItem,
            final String connObjectKeyValue,
            final Set<Attribute> attrs) {

        ConnObjectTO connObjectTO = ConnObjectUtils.getConnObjectTO(null, attrs);
        connObjectTO.getAttrs().add(new Attr.Builder(connObjectKeyItem.getExtAttrName()).
                value(connObjectKeyValue).build());
        connObjectTO.getAttrs().add(new Attr.Builder(Uid.NAME).
                value(connObjectKeyValue).build());

        return connObjectTO;
    }

    protected ConnObjectTO getOnSyncope(
            final Any<?> any,
            final MappingItem connObjectKeyItem,
            final Provision provision) {

        Pair<String, Set<Attribute>> prepared = mappingManager.prepareAttrsFromAny(
                any, null, false, true, provision);
        return getOnSyncope(connObjectKeyItem, prepared.getLeft(), prepared.getRight());
    }

    protected ConnObjectTO getOnSyncope(
            final LinkedAccount account,
            final MappingItem connObjectKeyItem,
            final Provision provision) {

        Set<Attribute> attrs = mappingManager.prepareAttrsFromLinkedAccount(
                account.getOwner(), account, null, false, provision);
        return getOnSyncope(connObjectKeyItem, account.getConnObjectKeyValue(), attrs);
    }

    protected Any<?> getAny(final Provision provision, final String anyKey) {
        AnyDAO<Any<?>> dao = anyUtilsFactory.getInstance(provision.getAnyType().getKind()).dao();
        Any<?> any = SyncopeConstants.UUID_PATTERN.matcher(anyKey).matches()
                ? dao.authFind(anyKey)
                : dao.authFind(dao.findKey(anyKey));
        if (any == null) {
            throw new NotFoundException(provision.getAnyType().getKey() + " '" + anyKey + "'");
        }
        return any;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    public ReconStatus status(
            final String anyTypeKey,
            final String resourceKey,
            final String anyKey,
            final Set<String> moreAttrsToGet) {

        Provision provision = getProvision(anyTypeKey, resourceKey);

        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey for " + provision.getAnyType().getKey()
                + " on resource '" + provision.getResource().getKey() + "'"));

        Any<?> any = getAny(provision, anyKey);

        ReconStatus status = new ReconStatus();
        status.setMatchType(MatchType.ANY);
        status.setAnyTypeKind(any.getType().getKind());
        status.setAnyKey(any.getKey());
        status.setRealm(any.getRealm().getFullPath());
        status.setOnSyncope(getOnSyncope(any, connObjectKeyItem, provision));

        List<ConnectorObject> connObjs = outboundMatcher.match(connectorManager.getConnector(
                provision.getResource()), any, provision, Optional.of(moreAttrsToGet.toArray(new String[] {})));
        if (!connObjs.isEmpty()) {
            status.setOnResource(ConnObjectUtils.getConnObjectTO(
                    outboundMatcher.getFIQL(connObjs.get(0), provision), connObjs.get(0).getAttributes()));

            if (connObjs.size() > 1) {
                LOG.warn("Expected single match, found {}", connObjs);
            } else {
                virAttrHandler.setValues(any, connObjs.get(0));
            }
        }

        return status;
    }

    protected SyncDeltaBuilder syncDeltaBuilder(
            final Provision provision,
            final Filter filter,
            final Set<String> moreAttrsToGet) {

        Stream<MappingItem> mapItems = Stream.concat(
                provision.getMapping().getItems().stream(),
                virSchemaDAO.findByProvision(provision).stream().map(VirSchema::asLinkingMappingItem));
        OperationOptions options = MappingUtils.buildOperationOptions(mapItems, moreAttrsToGet.toArray(new String[0]));

        SyncDeltaBuilder syncDeltaBuilder = new SyncDeltaBuilder().
                setToken(new SyncToken("")).
                setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                setObjectClass(provision.getObjectClass());
        connectorManager.getConnector(provision.getResource()).
                search(provision.getObjectClass(), filter, new SearchResultsHandler() {

                    @Override
                    public boolean handle(final ConnectorObject connObj) {
                        syncDeltaBuilder.setObject(connObj);
                        return false;
                    }

                    @Override
                    public void handleResult(final SearchResult sr) {
                        // do nothing
                    }
                }, 1, null, List.of(), options);

        return syncDeltaBuilder;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    public ReconStatus status(
            final String anyTypeKey,
            final String resourceKey,
            final Filter filter,
            final Set<String> moreAttrsToGet) {

        Provision provision = getProvision(anyTypeKey, resourceKey);

        SyncDeltaBuilder syncDeltaBuilder = syncDeltaBuilder(provision, filter, moreAttrsToGet);

        ReconStatus status = new ReconStatus();
        if (syncDeltaBuilder.getObject() != null) {
            MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision).
                    orElseThrow(() -> new NotFoundException(
                    "ConnObjectKey for " + provision.getAnyType().getKey()
                    + " on resource '" + provision.getResource().getKey() + "'"));

            inboundMatcher.match(syncDeltaBuilder.build(), provision).stream().findFirst().ifPresent(match -> {
                if (match.getAny() != null) {
                    status.setMatchType(MatchType.ANY);
                    status.setAnyTypeKind(match.getAny().getType().getKind());
                    status.setAnyKey(match.getAny().getKey());
                    status.setRealm(match.getAny().getRealm().getFullPath());
                    status.setOnSyncope(getOnSyncope(match.getAny(), connObjectKeyItem, provision));
                } else if (match.getLinkedAccount() != null) {
                    status.setMatchType(MatchType.LINKED_ACCOUNT);
                    status.setAnyTypeKind(AnyTypeKind.USER);
                    status.setAnyKey(match.getLinkedAccount().getOwner().getKey());
                    status.setRealm(match.getLinkedAccount().getOwner().getRealm().getFullPath());
                    status.setOnSyncope(getOnSyncope(match.getLinkedAccount(), connObjectKeyItem, provision));
                }
            });

            status.setOnResource(ConnObjectUtils.getConnObjectTO(
                    outboundMatcher.getFIQL(syncDeltaBuilder.getObject(), provision),
                    syncDeltaBuilder.getObject().getAttributes()));

            if (status.getMatchType() == MatchType.ANY && StringUtils.isNotBlank(status.getAnyKey())) {
                virAttrHandler.setValues(getAny(provision, status.getAnyKey()), syncDeltaBuilder.getObject());
            }
        }

        return status;
    }

    protected SyncopeSinglePushExecutor singlePushExecutor() {
        return (SyncopeSinglePushExecutor) ApplicationContextProvider.getBeanFactory().
                createBean(SinglePushJobDelegate.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    public List<ProvisioningReport> push(
            final String anyTypeKey,
            final String resourceKey,
            final String anyKey,
            final PushTaskTO pushTask) {

        Provision provision = getProvision(anyTypeKey, resourceKey);

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();
        try {
            results.addAll(singlePushExecutor().push(
                    provision,
                    connectorManager.getConnector(provision.getResource()),
                    getAny(provision, anyKey),
                    pushTask,
                    AuthContextUtils.getUsername()));
            if (!results.isEmpty() && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {
                sce.getElements().add(results.get(0).getMessage());
            }
        } catch (JobExecutionException e) {
            sce.getElements().add(e.getMessage());
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        return results;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    public List<ProvisioningReport> push(
            final String anyTypeKey,
            final String resourceKey,
            final Filter filter,
            final Set<String> moreAttrsToGet,
            final PushTaskTO pushTask) {

        Provision provision = getProvision(anyTypeKey, resourceKey);

        SyncDeltaBuilder syncDeltaBuilder = syncDeltaBuilder(provision, filter, moreAttrsToGet);

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();

        if (syncDeltaBuilder.getObject() != null) {
            inboundMatcher.match(syncDeltaBuilder.build(), provision).stream().findFirst().ifPresent(match -> {
                try {
                    if (match.getMatchTarget() == MatchType.ANY) {
                        results.addAll(singlePushExecutor().push(
                                provision,
                                connectorManager.getConnector(provision.getResource()),
                                match.getAny(),
                                pushTask,
                                AuthContextUtils.getUsername()));
                        if (!results.isEmpty() && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {
                            sce.getElements().add(results.get(0).getMessage());
                        }
                    } else {
                        ProvisioningReport result = singlePushExecutor().push(
                                provision,
                                connectorManager.getConnector(provision.getResource()),
                                match.getLinkedAccount(),
                                pushTask,
                                AuthContextUtils.getUsername());
                        if (result.getStatus() == ProvisioningReport.Status.FAILURE) {
                            sce.getElements().add(result.getMessage());
                        } else {
                            results.add(result);
                        }
                    }
                } catch (JobExecutionException e) {
                    sce.getElements().add(e.getMessage());
                }
            });
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        return results;
    }

    protected List<ProvisioningReport> pull(
            final Provision provision,
            final ReconFilterBuilder reconFilterBuilder,
            final Set<String> moreAttrsToGet,
            final PullTaskTO pullTask) {

        if (pullTask.getDestinationRealm() == null || realmDAO.findByFullPath(pullTask.getDestinationRealm()) == null) {
            throw new NotFoundException("Realm " + pullTask.getDestinationRealm());
        }

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();
        try {
            SyncopeSinglePullExecutor executor =
                    (SyncopeSinglePullExecutor) ApplicationContextProvider.getBeanFactory().
                            createBean(SinglePullJobDelegate.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);

            results.addAll(executor.pull(
                    provision,
                    connectorManager.getConnector(provision.getResource()),
                    reconFilterBuilder,
                    moreAttrsToGet,
                    pullTask));
            if (!results.isEmpty() && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {
                sce.getElements().add(results.get(0).getMessage());
            }
        } catch (JobExecutionException e) {
            sce.getElements().add(e.getMessage());
        }

        if (!sce.isEmpty()) {
            throw sce;
        }

        return results;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    @Transactional(noRollbackFor = SyncopeClientException.class)
    public List<ProvisioningReport> pull(
            final String anyTypeKey,
            final String resourceKey,
            final String anyKey,
            final Set<String> moreAttrsToGet,
            final PullTaskTO pullTask) {

        Provision provision = getProvision(anyTypeKey, resourceKey);

        Any<?> any = getAny(provision, anyKey);

        if (provision.getMapping().getConnObjectKeyItem().isEmpty()) {
            throw new NotFoundException(
                    "ConnObjectKey cannot be determined for mapping " + provision.getMapping().getKey());
        }

        String connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, provision).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey for " + provision.getAnyType().getKey()
                + " on resource '" + provision.getResource().getKey() + "'"));

        return pull(
                provision,
                new KeyValueReconFilterBuilder(
                        provision.getMapping().getConnObjectKeyItem().get().getExtAttrName(), connObjectKeyValue),
                moreAttrsToGet,
                pullTask);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    @Transactional(noRollbackFor = SyncopeClientException.class)
    public List<ProvisioningReport> pull(
            final String anyTypeKey,
            final String resourceKey,
            final Filter filter,
            final Set<String> moreAttrsToGet,
            final PullTaskTO pullTask) {

        Provision provision = getProvision(anyTypeKey, resourceKey);

        return pull(
                provision,
                new ConstantReconFilterBuilder(filter),
                moreAttrsToGet,
                pullTask);
    }

    protected CsvSchema.Builder csvSchema(final AbstractCSVSpec spec) {
        CsvSchema.Builder schemaBuilder = new CsvSchema.Builder().setUseHeader(true).
                setColumnSeparator(spec.getColumnSeparator()).
                setArrayElementSeparator(spec.getArrayElementSeparator()).
                setQuoteChar(spec.getQuoteChar()).
                setLineSeparator(spec.getLineSeparator()).
                setNullValue(spec.getNullValue()).
                setAllowComments(spec.getAllowComments());
        if (spec.getEscapeChar() != null) {
            schemaBuilder.setEscapeChar(spec.getEscapeChar());
        }
        return schemaBuilder;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    public List<ProvisioningReport> push(
            final SearchCond searchCond,
            final int page,
            final int size,
            final List<OrderByClause> orderBy,
            final String realm,
            final CSVPushSpec spec,
            final OutputStream os) {

        AnyType anyType = anyTypeDAO.find(spec.getAnyTypeKey());
        if (anyType == null) {
            throw new NotFoundException("AnyType '" + spec.getAnyTypeKey() + "'");
        }

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());

        String entitlement;
        switch (anyType.getKind()) {
            case GROUP:
                entitlement = IdRepoEntitlement.GROUP_SEARCH;
                break;

            case ANY_OBJECT:
                entitlement = AnyEntitlement.SEARCH.getFor(anyType.getKey());
                break;

            case USER:
            default:
                entitlement = IdRepoEntitlement.USER_SEARCH;
        }

        Set<String> adminRealms = RealmUtils.getEffective(AuthContextUtils.getAuthorizations().get(entitlement), realm);
        SearchCond effectiveCond = searchCond == null ? anyUtils.dao().getAllMatchingCond() : searchCond;

        List<Any<?>> matching;
        if (spec.getIgnorePaging()) {
            matching = new ArrayList<>();

            int count = anySearchDAO.count(adminRealms, effectiveCond, anyType.getKind());
            int pages = (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            for (int p = 1; p <= pages; p++) {
                matching.addAll(anySearchDAO.search(adminRealms, effectiveCond,
                        p, AnyDAO.DEFAULT_PAGE_SIZE, orderBy, anyType.getKind()));
            }
        } else {
            matching = anySearchDAO.search(adminRealms, effectiveCond, page, size, orderBy, anyType.getKind());
        }

        List<String> columns = new ArrayList<>();
        spec.getFields().forEach(item -> {
            if (anyUtils.getField(item) == null) {
                LOG.warn("Ignoring invalid field {}", item);
            } else {
                columns.add(item);
            }
        });
        spec.getPlainAttrs().forEach(item -> {
            if (plainSchemaDAO.find(item) == null) {
                LOG.warn("Ignoring invalid plain schema {}", item);
            } else {
                columns.add(item);
            }
        });
        spec.getDerAttrs().forEach(item -> {
            if (derSchemaDAO.find(item) == null) {
                LOG.warn("Ignoring invalid derived schema {}", item);
            } else {
                columns.add(item);
            }
        });
        spec.getVirAttrs().forEach(item -> {
            if (virSchemaDAO.find(item) == null) {
                LOG.warn("Ignoring invalid virtual schema {}", item);
            } else {
                columns.add(item);
            }
        });

        PushTaskTO pushTask = new PushTaskTO();
        pushTask.setMatchingRule(spec.getMatchingRule());
        pushTask.setUnmatchingRule(spec.getUnmatchingRule());
        pushTask.getActions().addAll(spec.getProvisioningActions());

        try (CSVStreamConnector connector = new CSVStreamConnector(
                null,
                spec.getArrayElementSeparator(),
                csvSchema(spec),
                null,
                os,
                columns.toArray(new String[columns.size()]))) {

            SyncopeStreamPushExecutor executor =
                    (SyncopeStreamPushExecutor) ApplicationContextProvider.getBeanFactory().
                            createBean(StreamPushJobDelegate.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
            return executor.push(
                    anyType,
                    matching,
                    columns,
                    connector,
                    spec.getPropagationActions(),
                    pushTask,
                    AuthContextUtils.getUsername());
        } catch (Exception e) {
            LOG.error("Could not push to stream", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    @Transactional(noRollbackFor = SyncopeClientException.class)
    public List<ProvisioningReport> pull(final CSVPullSpec spec, final InputStream csv) {
        AnyType anyType = anyTypeDAO.find(spec.getAnyTypeKey());
        if (anyType == null) {
            throw new NotFoundException("AnyType '" + spec.getAnyTypeKey() + "'");
        }

        if (realmDAO.findByFullPath(spec.getDestinationRealm()) == null) {
            throw new NotFoundException("Realm " + spec.getDestinationRealm());
        }

        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(spec.getDestinationRealm());
        pullTask.setRemediation(spec.getRemediation());
        pullTask.setMatchingRule(spec.getMatchingRule());
        pullTask.setUnmatchingRule(spec.getUnmatchingRule());
        pullTask.getActions().addAll(spec.getProvisioningActions());

        try (CSVStreamConnector connector = new CSVStreamConnector(
                spec.getKeyColumn(),
                spec.getArrayElementSeparator(),
                csvSchema(spec),
                csv,
                null)) {

            List<String> columns = connector.getColumns(spec);
            if (!columns.contains(spec.getKeyColumn())) {
                throw new NotFoundException("Key column '" + spec.getKeyColumn() + "'");
            }

            SyncopeStreamPullExecutor executor =
                    (SyncopeStreamPullExecutor) ApplicationContextProvider.getBeanFactory().
                            createBean(StreamPullJobDelegate.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
            return executor.pull(anyType,
                    spec.getKeyColumn(),
                    columns,
                    spec.getConflictResolutionAction(),
                    spec.getPullCorrelationRule(),
                    connector,
                    pullTask);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Could not pull from stream", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... os)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
