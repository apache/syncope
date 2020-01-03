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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
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
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.rest.api.beans.AbstractCSVSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.pushpull.stream.StreamConnector;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Uid;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;

@Component
public class ReconciliationLogic extends AbstractTransactionalLogic<EntityTO> {

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private VirAttrHandler virAttrHandler;

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private InboundMatcher inboundMatcher;

    @Autowired
    private OutboundMatcher outboundMatcher;

    @Autowired
    private ConnectorFactory connFactory;

    @Autowired
    private SyncopeSinglePullExecutor singlePullExecutor;

    @Autowired
    private SyncopeSinglePushExecutor singlePushExecutor;

    @Autowired
    private SyncopeStreamPushExecutor streamPushExecutor;

    @Autowired
    private SyncopeStreamPullExecutor streamPullExecutor;

    private Provision getProvision(final String anyTypeKey, final String resourceKey) {
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

    private ConnObjectTO getOnSyncope(
            final MappingItem connObjectKeyItem,
            final String connObjectKeyValue,
            final Set<Attribute> attrs) {

        ConnObjectTO connObjectTO = ConnObjectUtils.getConnObjectTO(attrs);
        connObjectTO.getAttrs().add(new AttrTO.Builder().
                schema(connObjectKeyItem.getExtAttrName()).value(connObjectKeyValue).build());
        connObjectTO.getAttrs().add(new AttrTO.Builder().
                schema(Uid.NAME).value(connObjectKeyValue).build());

        return connObjectTO;
    }

    private ConnObjectTO getOnSyncope(
            final Any<?> any,
            final MappingItem connObjectKeyItem,
            final Provision provision) {

        Pair<String, Set<Attribute>> prepared = mappingManager.prepareAttrs(any, null, false, true, provision);
        return getOnSyncope(connObjectKeyItem, prepared.getLeft(), prepared.getRight());
    }

    private ConnObjectTO getOnSyncope(
            final LinkedAccount account,
            final MappingItem connObjectKeyItem,
            final Provision provision) {

        Set<Attribute> attrs = mappingManager.prepareAttrs(account.getOwner(), account, null, false, provision);
        return getOnSyncope(connObjectKeyItem, account.getConnObjectKeyValue(), attrs);
    }

    private Any<?> getAny(final Provision provision, final String anyKey) {
        AnyDAO<Any<?>> dao = anyUtilsFactory.getInstance(provision.getAnyType().getKind()).dao();
        Any<?> any = SyncopeConstants.UUID_PATTERN.matcher(anyKey).matches()
                ? dao.authFind(anyKey)
                : dao.authFind(dao.findKey(anyKey));
        if (any == null) {
            throw new NotFoundException(provision.getAnyType().getKey() + " '" + anyKey + "'");
        }
        return any;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    public ReconStatus status(final ReconQuery query) {
        Provision provision = getProvision(query.getAnyTypeKey(), query.getResourceKey());

        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey for " + provision.getAnyType().getKey()
                + " on resource '" + provision.getResource().getKey() + "'"));

        ReconStatus status = new ReconStatus();

        if (query.getConnObjectKeyValue() != null) {
            inboundMatcher.matchByConnObjectKeyValue(connObjectKeyItem, query.getConnObjectKeyValue(), provision).
                    stream().findFirst().ifPresent(match -> {
                        if (match.getAny() != null) {
                            status.setMatchType(MatchType.ANY);
                            status.setAnyTypeKind(match.getAny().getType().getKind());
                            status.setAnyKey(match.getAny().getKey());
                            status.setOnSyncope(getOnSyncope(match.getAny(), connObjectKeyItem, provision));
                        } else if (match.getLinkedAccount() != null) {
                            status.setMatchType(MatchType.LINKED_ACCOUNT);
                            status.setAnyTypeKind(AnyTypeKind.USER);
                            status.setAnyKey(match.getLinkedAccount().getOwner().getKey());
                            status.setOnSyncope(getOnSyncope(match.getLinkedAccount(), connObjectKeyItem, provision));
                        }
                    });

            outboundMatcher.matchByConnObjectKeyValue(
                    connFactory.getConnector(provision.getResource()),
                    connObjectKeyItem,
                    query.getConnObjectKeyValue(),
                    provision,
                    Optional.empty(),
                    Optional.empty()).
                    ifPresent(connObj -> {
                        status.setOnResource(ConnObjectUtils.getConnObjectTO(connObj.getAttributes()));

                        if (status.getMatchType() == MatchType.ANY && StringUtils.isNotBlank(status.getAnyKey())) {
                            virAttrHandler.setValues(getAny(provision, status.getAnyKey()), connObj);
                        }
                    });
        }
        if (query.getAnyKey() != null) {
            Any<?> any = getAny(provision, query.getAnyKey());
            status.setMatchType(MatchType.ANY);
            status.setAnyTypeKind(any.getType().getKind());
            status.setAnyKey(any.getKey());
            status.setOnSyncope(getOnSyncope(any, connObjectKeyItem, provision));

            List<ConnectorObject> connObjs = outboundMatcher.match(
                    connFactory.getConnector(provision.getResource()), any, provision);
            if (!connObjs.isEmpty()) {
                status.setOnResource(ConnObjectUtils.getConnObjectTO(connObjs.get(0).getAttributes()));

                if (connObjs.size() > 1) {
                    LOG.warn("Expected single match, found {}", connObjs);
                } else {
                    virAttrHandler.setValues(any, connObjs.get(0));
                }
            }
        }

        return status;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    public void push(final ReconQuery query, final PushTaskTO pushTask) {
        Provision provision = getProvision(query.getAnyTypeKey(), query.getResourceKey());

        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey for " + provision.getAnyType().getKey()
                + " on resource '" + provision.getResource().getKey() + "'"));

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        List<ProvisioningReport> results = new ArrayList<>();

        if (query.getConnObjectKeyValue() != null) {
            inboundMatcher.matchByConnObjectKeyValue(connObjectKeyItem, query.getConnObjectKeyValue(), provision).
                    stream().findFirst().ifPresent(match -> {
                        try {
                            if (match.getMatchTarget() == MatchType.ANY) {
                                results.addAll(singlePushExecutor.push(
                                        provision,
                                        connFactory.getConnector(provision.getResource()),
                                        match.getAny(),
                                        pushTask));
                                if (!results.isEmpty()
                                        && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {

                                    sce.getElements().add(results.get(0).getMessage());
                                }
                            } else {
                                ProvisioningReport result = singlePushExecutor.push(
                                        provision,
                                        connFactory.getConnector(provision.getResource()),
                                        match.getLinkedAccount(),
                                        pushTask);
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

        if (sce.isEmpty() && results.isEmpty() && query.getAnyKey() != null) {
            try {
                results.addAll(singlePushExecutor.push(
                        provision,
                        connFactory.getConnector(provision.getResource()),
                        getAny(provision, query.getAnyKey()),
                        pushTask));
                if (!results.isEmpty() && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {
                    sce.getElements().add(results.get(0).getMessage());
                }
            } catch (JobExecutionException e) {
                sce.getElements().add(e.getMessage());
            }
        }

        if (!sce.isEmpty()) {
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    public void pull(final ReconQuery query, final PullTaskTO pullTask) {
        Provision provision = getProvision(query.getAnyTypeKey(), query.getResourceKey());

        Optional<String> connObjectKeyValue = Optional.ofNullable(query.getConnObjectKeyValue());
        if (query.getAnyKey() != null) {
            Any<?> any = getAny(provision, query.getAnyKey());
            connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, provision);
        }
        if (!connObjectKeyValue.isPresent()) {
            throw new NotFoundException(
                    "ConnObjectKey for " + provision.getAnyType().getKey()
                    + " on resource '" + provision.getResource().getKey() + "'");
        }

        if (pullTask.getDestinationRealm() == null || realmDAO.findByFullPath(pullTask.getDestinationRealm()) == null) {
            throw new NotFoundException("Realm " + pullTask.getDestinationRealm());
        }

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
        try {
            List<ProvisioningReport> results = singlePullExecutor.pull(
                    provision,
                    connFactory.getConnector(provision.getResource()),
                    provision.getMapping().getConnObjectKeyItem().get().getExtAttrName(),
                    connObjectKeyValue.get(),
                    pullTask);
            if (!results.isEmpty() && results.get(0).getStatus() == ProvisioningReport.Status.FAILURE) {
                sce.getElements().add(results.get(0).getMessage());
            }
        } catch (JobExecutionException e) {
            sce.getElements().add(e.getMessage());
        }

        if (!sce.isEmpty()) {
            throw sce;
        }
    }

    private CsvSchema csvSchema(final AbstractCSVSpec spec, final CsvSchema base) {
        CsvSchema schema = base.
                withColumnSeparator(spec.getColumnSeparator()).
                withArrayElementSeparator(spec.getArrayElementSeparator()).
                withQuoteChar(spec.getQuoteChar()).
                withLineSeparator(spec.getLineSeparator()).
                withNullValue(spec.getNullValue()).
                withAllowComments(spec.isAllowComments());
        if (spec.getEscapeChar() != null) {
            schema = schema.withEscapeChar(spec.getEscapeChar());
        }

        return schema;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
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
                entitlement = StandardEntitlement.GROUP_SEARCH;
                break;

            case ANY_OBJECT:
                entitlement = AnyEntitlement.SEARCH.getFor(anyType.getKey());
                break;

            case USER:
            default:
                entitlement = StandardEntitlement.USER_SEARCH;
        }

        Set<String> adminRealms = RealmUtils.getEffective(AuthContextUtils.getAuthorizations().get(entitlement), realm);
        SearchCond effectiveCond = searchCond == null ? anyUtils.dao().getAllMatchingCond() : searchCond;

        List<Any<?>> matching;
        if (spec.isIgnorePaging()) {
            matching = new ArrayList<>();

            int count = searchDAO.count(adminRealms, searchCond, anyType.getKind());
            int pages = (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            for (int p = 1; p <= pages; p++) {
                matching.addAll(searchDAO.search(adminRealms, effectiveCond,
                        p, AnyDAO.DEFAULT_PAGE_SIZE, orderBy, anyType.getKind()));
            }
        } else {
            matching = searchDAO.search(adminRealms, effectiveCond, page, size, orderBy, anyType.getKind());
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

        CsvSchema.Builder schemaBuilder = CsvSchema.builder().setUseHeader(true);
        columns.forEach(schemaBuilder::addColumn);
        CsvSchema schema = csvSchema(spec, schemaBuilder.build());

        PushTaskTO pushTask = new PushTaskTO();
        pushTask.setMatchingRule(spec.getMatchingRule());
        pushTask.setUnmatchingRule(spec.getUnmatchingRule());
        pushTask.getActions().addAll(spec.getActions());

        try (SequenceWriter writer = new CsvMapper().writer(schema).forType(Map.class).writeValues(os)) {
            return streamPushExecutor.push(
                    anyType,
                    matching,
                    columns,
                    new StreamConnector(null, spec.getArrayElementSeparator(), null, writer),
                    pushTask);
        } catch (Exception e) {
            LOG.error("Could not push to stream", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Reconciliation);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
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
        pullTask.setRemediation(spec.isRemediation());
        pullTask.setMatchingRule(spec.getMatchingRule());
        pullTask.setUnmatchingRule(spec.getUnmatchingRule());
        pullTask.getActions().addAll(spec.getActions());

        CsvSchema schema = csvSchema(spec, CsvSchema.emptySchema().withHeader());
        try {
            MappingIterator<Map<String, String>> reader =
                    new CsvMapper().readerFor(Map.class).with(schema).readValues(csv);

            List<String> columns = new ArrayList<>();
            ((CsvSchema) reader.getParserSchema()).forEach(column -> {
                if (!spec.getIgnoreColumns().contains(column.getName())) {
                    columns.add(column.getName());
                }
            });

            if (!columns.contains(spec.getKeyColumn())) {
                throw new NotFoundException("Key column '" + spec.getKeyColumn() + "'");
            }

            return streamPullExecutor.pull(
                    anyType,
                    spec.getKeyColumn(),
                    columns,
                    spec.getConflictResolutionAction(),
                    spec.getPullCorrelationRule(),
                    new StreamConnector(spec.getKeyColumn(), spec.getArrayElementSeparator(), reader, null),
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
