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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.rest.api.beans.AbstractCSVSpec;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.ConstantReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.KeyValueReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.SinglePullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.SinglePushJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.stream.CSVStreamConnector;
import org.apache.syncope.core.provisioning.java.pushpull.stream.StreamPullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.stream.StreamPushJobDelegate;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ReconciliationLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final AnyTypeDAO anyTypeDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final RealmSearchDAO realmSearchDAO;

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
            final RealmSearchDAO realmSearchDAO,
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
        this.realmSearchDAO = realmSearchDAO;
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

    protected Triple<AnyType, ExternalResource, Provision> getProvision(
            final String anyTypeKey, final String resourceKey) {

        AnyType anyType = anyTypeDAO.findById(anyTypeKey).
                orElseThrow(() -> new NotFoundException("AnyType " + anyTypeKey));

        ExternalResource resource = resourceDAO.findById(resourceKey).
                orElseThrow(() -> new NotFoundException("Resource '" + resourceKey));

        Provision provision = resource.getProvisionByAnyType(anyType.getKey()).
                orElseThrow(() -> new NotFoundException(
                "Provision for " + anyType + " on Resource '" + resourceKey + "'"));
        if (provision.getMapping() == null) {
            throw new NotFoundException("Mapping for " + anyType + " on Resource '" + resourceKey + "'");
        }

        return Triple.of(anyType, resource, provision);
    }

    protected ConnObject getOnSyncope(
            final Item connObjectKeyItem,
            final String connObjectKeyValue,
            final Boolean suspended,
            final Set<Attribute> attrs) {

        ConnObject connObjectTO = ConnObjectUtils.getConnObjectTO(null, attrs);
        connObjectTO.getAttrs().add(new Attr.Builder(connObjectKeyItem.getExtAttrName()).
                value(connObjectKeyValue).build());
        connObjectTO.getAttrs().add(new Attr.Builder(Uid.NAME).
                value(connObjectKeyValue).build());
        Optional.ofNullable(suspended).ifPresent(s -> {
            connObjectTO.getAttrs().removeIf(a -> OperationalAttributes.ENABLE_NAME.equals(a.getSchema()));
            connObjectTO.getAttrs().add(new Attr.Builder(OperationalAttributes.ENABLE_NAME).
                    value(BooleanUtils.negate(s).toString()).build());
        });

        return connObjectTO;
    }

    protected ConnObject getOnSyncope(
            final Any any,
            final Item connObjectKeyItem,
            final ExternalResource resource,
            final Provision provision) {

        Pair<String, Set<Attribute>> prepared = mappingManager.prepareAttrsFromAny(
                any, null, false, true, resource, provision);
        return getOnSyncope(
                connObjectKeyItem,
                prepared.getLeft(),
                any instanceof final User user ? user.isSuspended() : null,
                prepared.getRight());
    }

    protected ConnObject getOnSyncope(
            final LinkedAccount account,
            final Item connObjectKeyItem,
            final Provision provision) {

        Set<Attribute> attrs = mappingManager.prepareAttrsFromLinkedAccount(
                account.getOwner(), account, null, false, provision);
        return getOnSyncope(
                connObjectKeyItem,
                account.getConnObjectKeyValue(),
                account.isSuspended(),
                attrs);
    }

    protected Any getAny(final Provision provision, final AnyTypeKind anyTypeKind, final String anyKey) {
        AnyDAO<?> dao = anyUtilsFactory.getInstance(anyTypeKind).dao();

        String actualKey = anyKey;
        if (!SyncopeConstants.UUID_PATTERN.matcher(anyKey).matches()) {
            actualKey = (dao instanceof final UserDAO userDAO
                    ? userDAO.findKey(anyKey)
                    : dao instanceof final GroupDAO groupDAO
                            ? groupDAO.findKey(anyKey)
                            : ((AnyObjectDAO) dao).findKey(provision.getAnyType(), anyKey)).
                    orElse(null);
        }

        return Optional.ofNullable(dao.authFind(actualKey)).
                orElseThrow(() -> new NotFoundException(provision.getAnyType() + " '" + anyKey + "'"));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    public ReconStatus status(
            final String anyTypeKey,
            final String resourceKey,
            final String anyKey,
            final Set<String> moreAttrsToGet) {

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, resourceKey);

        Item connObjectKeyItem = MappingUtils.getConnObjectKeyItem(triple.getRight()).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey for " + triple.getLeft().getKey()
                + " on resource '" + triple.getMiddle().getKey() + "'"));

        Any any = getAny(triple.getRight(), triple.getLeft().getKind(), anyKey);

        ReconStatus status = new ReconStatus();
        status.setMatchType(MatchType.ANY);
        status.setAnyTypeKind(any.getType().getKind());
        status.setAnyKey(any.getKey());
        status.setRealm(any.getRealm().getFullPath());
        status.setOnSyncope(getOnSyncope(any, connObjectKeyItem, triple.getMiddle(), triple.getRight()));

        List<ConnectorObject> connObjs = outboundMatcher.match(
                connectorManager.getConnector(triple.getMiddle()),
                any,
                triple.getMiddle(),
                triple.getRight(),
                Optional.of(moreAttrsToGet.toArray(String[]::new)));
        if (!connObjs.isEmpty()) {
            status.setOnResource(ConnObjectUtils.getConnObjectTO(
                    outboundMatcher.getFIQL(connObjs.getFirst(), triple.getMiddle(), triple.getRight()),
                    connObjs.getFirst().getAttributes()));

            if (connObjs.size() > 1) {
                LOG.warn("Expected single match, found {}", connObjs);
            } else {
                virAttrHandler.setValues(any, connObjs.getFirst());
            }
        }

        return status;
    }

    protected SyncDeltaBuilder syncDeltaBuilder(
            final AnyType anyType,
            final ExternalResource resource,
            final Provision provision,
            final Filter filter,
            final Set<String> moreAttrsToGet) {

        Stream<Item> mapItems = Stream.concat(
                provision.getMapping().getItems().stream(),
                virSchemaDAO.findByResourceAndAnyType(resource.getKey(), anyType.getKey()).stream().
                        map(VirSchema::asLinkingMappingItem));
        OperationOptions options = MappingUtils.buildOperationOptions(mapItems, moreAttrsToGet.toArray(String[]::new));

        SyncDeltaBuilder syncDeltaBuilder = new SyncDeltaBuilder().
                setToken(new SyncToken("")).
                setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                setObjectClass(new ObjectClass(provision.getObjectClass()));
        connectorManager.getConnector(resource).
                search(syncDeltaBuilder.getObjectClass(), filter, new SearchResultsHandler() {

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

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, resourceKey);

        SyncDeltaBuilder syncDeltaBuilder = syncDeltaBuilder(
                triple.getLeft(), triple.getMiddle(), triple.getRight(), filter, moreAttrsToGet);

        ReconStatus status = new ReconStatus();
        if (syncDeltaBuilder.getObject() != null) {
            Item connObjectKeyItem = MappingUtils.getConnObjectKeyItem(triple.getRight()).
                    orElseThrow(() -> new NotFoundException(
                    "ConnObjectKey for " + triple.getLeft().getKey()
                    + " on resource '" + triple.getMiddle().getKey() + "'"));

            inboundMatcher.match(
                    syncDeltaBuilder.build(), triple.getMiddle(), triple.getRight(), triple.getLeft().getKind()).
                    stream().findFirst().ifPresent(match -> {

                        if (match.getAny() != null) {
                            status.setMatchType(MatchType.ANY);
                            status.setAnyTypeKind(match.getAny().getType().getKind());
                            status.setAnyKey(match.getAny().getKey());
                            status.setRealm(match.getAny().getRealm().getFullPath());
                            status.setOnSyncope(getOnSyncope(
                                    match.getAny(), connObjectKeyItem, triple.getMiddle(), triple.getRight()));
                        } else if (match.getLinkedAccount() != null) {
                            status.setMatchType(MatchType.LINKED_ACCOUNT);
                            status.setAnyTypeKind(AnyTypeKind.USER);
                            status.setAnyKey(match.getLinkedAccount().getOwner().getKey());
                            status.setRealm(match.getLinkedAccount().getOwner().getRealm().getFullPath());
                            status.setOnSyncope(getOnSyncope(
                                    match.getLinkedAccount(), connObjectKeyItem, triple.getRight()));
                        }
                    });

            status.setOnResource(ConnObjectUtils.getConnObjectTO(
                    outboundMatcher.getFIQL(syncDeltaBuilder.getObject(), triple.getMiddle(), triple.getRight()),
                    syncDeltaBuilder.getObject().getAttributes()));

            if (status.getMatchType() == MatchType.ANY && StringUtils.isNotBlank(status.getAnyKey())) {
                virAttrHandler.setValues(
                        getAny(triple.getRight(), triple.getLeft().getKind(), status.getAnyKey()),
                        syncDeltaBuilder.getObject());
            }
        }

        return status;
    }

    protected SyncopeSinglePushExecutor singlePushExecutor() {
        return ApplicationContextProvider.getBeanFactory().createBean(SinglePushJobDelegate.class);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    public List<ProvisioningReport> push(
            final String anyTypeKey,
            final String resourceKey,
            final String anyKey,
            final PushTaskTO pushTask) {

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, resourceKey);

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();
        try {
            results.addAll(singlePushExecutor().push(
                    triple.getMiddle(),
                    triple.getRight(),
                    connectorManager.getConnector(triple.getMiddle()),
                    getAny(triple.getRight(), triple.getLeft().getKind(), anyKey),
                    pushTask,
                    AuthContextUtils.getWho()));
            if (!results.isEmpty() && results.getFirst().getStatus() == ProvisioningReport.Status.FAILURE) {
                sce.getElements().add(results.getFirst().getMessage());
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

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, resourceKey);

        SyncDeltaBuilder syncDeltaBuilder = syncDeltaBuilder(
                triple.getLeft(), triple.getMiddle(), triple.getRight(), filter, moreAttrsToGet);

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();

        if (syncDeltaBuilder.getObject() != null) {
            inboundMatcher.match(
                    syncDeltaBuilder.build(), triple.getMiddle(), triple.getRight(), triple.getLeft().getKind()).
                    stream().findFirst().ifPresent(match -> {

                        try {
                            if (match.getMatchTarget() == MatchType.ANY) {
                                results.addAll(singlePushExecutor().push(
                                        triple.getMiddle(),
                                        triple.getRight(),
                                        connectorManager.getConnector(triple.getMiddle()),
                                        match.getAny(),
                                        pushTask,
                                        AuthContextUtils.getWho()));
                                if (!results.isEmpty()
                                        && results.getFirst().getStatus() == ProvisioningReport.Status.FAILURE) {

                                    sce.getElements().add(results.getFirst().getMessage());
                                }
                            } else {
                                ProvisioningReport result = singlePushExecutor().push(
                                        triple.getMiddle(),
                                        triple.getRight(),
                                        connectorManager.getConnector(triple.getMiddle()),
                                        match.getLinkedAccount(),
                                        pushTask,
                                        AuthContextUtils.getWho());
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
            final ExternalResource resource,
            final Provision provision,
            final ReconFilterBuilder reconFilterBuilder,
            final Set<String> moreAttrsToGet,
            final PullTaskTO pullTask) {

        if (pullTask.getDestinationRealm() == null || realmSearchDAO.findByFullPath(pullTask.getDestinationRealm())
                == null) {
            throw new NotFoundException("Realm " + pullTask.getDestinationRealm());
        }

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();
        try {
            SyncopeSinglePullExecutor executor =
                    ApplicationContextProvider.getBeanFactory().createBean(SinglePullJobDelegate.class);

            results.addAll(executor.pull(
                    resource,
                    provision,
                    connectorManager.getConnector(resource),
                    reconFilterBuilder,
                    moreAttrsToGet,
                    pullTask,
                    AuthContextUtils.getWho()));
            if (!results.isEmpty() && results.getFirst().getStatus() == ProvisioningReport.Status.FAILURE) {
                sce.getElements().add(results.getFirst().getMessage());
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

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, resourceKey);

        if (triple.getRight().getMapping().getConnObjectKeyItem().isEmpty()) {
            throw new NotFoundException(
                    "ConnObjectKey cannot be determined for mapping " + anyTypeKey);
        }

        Any any = getAny(triple.getRight(), triple.getLeft().getKind(), anyKey);

        String connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, triple.getMiddle(), triple.getRight()).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey for " + triple.getLeft().getKey()
                + " on resource '" + triple.getMiddle().getKey() + "'"));

        return pull(
                triple.getMiddle(),
                triple.getRight(),
                new KeyValueReconFilterBuilder(
                        triple.getRight().getMapping().getConnObjectKeyItem().get().getExtAttrName(),
                        connObjectKeyValue),
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

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, resourceKey);

        return pull(
                triple.getMiddle(),
                triple.getRight(),
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
            final Pageable pageable,
            final String realm,
            final CSVPushSpec spec,
            final OutputStream os) {

        AnyType anyType = anyTypeDAO.findById(spec.getAnyTypeKey()).
                orElseThrow(() -> new NotFoundException("AnyType " + spec.getAnyTypeKey()));

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

        Realm base = realmSearchDAO.findByFullPath(realm).
                orElseThrow(() -> new NotFoundException("Realm " + realm));

        Set<String> adminRealms = RealmUtils.getEffective(AuthContextUtils.getAuthorizations().get(entitlement), realm);
        SearchCond effectiveCond = searchCond == null ? anyUtils.dao().getAllMatchingCond() : searchCond;

        List<Any> matching;
        if (spec.getIgnorePaging()) {
            matching = new ArrayList<>();

            long count = anySearchDAO.count(base, true, adminRealms, effectiveCond, anyType.getKind());
            long pages = (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            for (int page = 0; page < pages; page++) {
                matching.addAll(anySearchDAO.search(
                        base, true, adminRealms, effectiveCond,
                        PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, pageable.getSort()),
                        anyType.getKind()));
            }
        } else {
            matching = anySearchDAO.search(base, true, adminRealms, effectiveCond, pageable, anyType.getKind());
        }

        List<String> columns = new ArrayList<>();
        spec.getFields().forEach(item -> anyUtils.getField(item).ifPresentOrElse(
                field -> columns.add(item),
                () -> LOG.warn("Ignoring invalid field {}", item)));
        spec.getPlainAttrs().forEach(item -> {
            if (plainSchemaDAO.existsById(item)) {
                columns.add(item);
            } else {
                LOG.warn("Ignoring invalid plain schema {}", item);
            }
        });
        spec.getDerAttrs().forEach(item -> {
            if (derSchemaDAO.existsById(item)) {
                columns.add(item);
            } else {
                LOG.warn("Ignoring invalid derived schema {}", item);
            }
        });
        spec.getDerAttrs().forEach(item -> {
            if (virSchemaDAO.existsById(item)) {
                columns.add(item);
            } else {
                LOG.warn("Ignoring invalid virtual schema {}", item);
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
                columns.toArray(String[]::new))) {

            SyncopeStreamPushExecutor executor =
                    ApplicationContextProvider.getBeanFactory().createBean(StreamPushJobDelegate.class);
            return executor.push(
                    anyType,
                    matching,
                    columns,
                    connector,
                    spec.getPropagationActions(),
                    pushTask,
                    AuthContextUtils.getWho());
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
        AnyType anyType = anyTypeDAO.findById(spec.getAnyTypeKey()).
                orElseThrow(() -> new NotFoundException("AnyType " + spec.getAnyTypeKey()));

        if (realmSearchDAO.findByFullPath(spec.getDestinationRealm()) == null) {
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
                    ApplicationContextProvider.getBeanFactory().createBean(StreamPullJobDelegate.class);
            return executor.pull(anyType,
                    spec.getKeyColumn(),
                    columns,
                    spec.getConflictResolutionAction(),
                    spec.getInboundCorrelationRule(),
                    connector,
                    pullTask,
                    AuthContextUtils.getWho());
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
