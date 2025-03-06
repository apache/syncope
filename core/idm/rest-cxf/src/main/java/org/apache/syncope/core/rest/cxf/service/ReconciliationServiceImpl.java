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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.core.logic.ReconciliationLogic;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.search.FilterVisitor;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.filter.Filter;

public class ReconciliationServiceImpl extends AbstractSearchService implements ReconciliationService {

    protected final ReconciliationLogic logic;

    public ReconciliationServiceImpl(final SearchCondVisitor searchCondVisitor, final ReconciliationLogic logic) {
        super(searchCondVisitor);
        this.logic = logic;
    }

    private void validate(final ReconQuery reconQuery) {
        if ((reconQuery.getAnyKey() == null && reconQuery.getFiql() == null)
                || (reconQuery.getAnyKey() != null && reconQuery.getFiql() != null)) {

            throw new ValidationException("Either provide anyKey or fiql, not both");
        }
    }

    private Pair<Filter, Set<String>> buildFromFIQL(final ReconQuery reconQuery) {
        Filter filter = null;
        Set<String> moreAttrsToGet = new HashSet<>();
        if (reconQuery.getMoreAttrsToGet() != null) {
            moreAttrsToGet.addAll(reconQuery.getMoreAttrsToGet());
        }
        if (StringUtils.isNotBlank(reconQuery.getFiql())) {
            try {
                FilterVisitor visitor = new FilterVisitor();
                SearchCondition<SearchBean> sc = searchContext.getCondition(reconQuery.getFiql(), SearchBean.class);
                sc.accept(visitor);

                filter = visitor.getQuery();
                moreAttrsToGet.addAll(visitor.getAttrs());
            } catch (Exception e) {
                LOG.error("Invalid FIQL expression: {}", reconQuery.getFiql(), e);

                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
                sce.getElements().add(reconQuery.getFiql());
                sce.getElements().add(ExceptionUtils.getRootCauseMessage(e));
                throw sce;
            }
        }

        return Pair.of(filter, moreAttrsToGet);
    }

    @Override
    public ReconStatus status(final ReconQuery query) {
        validate(query);

        if (query.getAnyKey() != null) {
            return logic.status(
                    query.getAnyTypeKey(),
                    query.getResourceKey(),
                    query.getAnyKey(),
                    Optional.ofNullable(query.getMoreAttrsToGet()).orElseGet(Set::of));
        }

        Pair<Filter, Set<String>> fromFIQL = buildFromFIQL(query);
        return logic.status(query.getAnyTypeKey(), query.getResourceKey(), fromFIQL.getLeft(), fromFIQL.getRight());
    }

    @Override
    public List<ProvisioningReport> push(final ReconQuery query, final PushTaskTO pushTask) {
        validate(query);

        if (query.getAnyKey() != null) {
            return logic.push(query.getAnyTypeKey(), query.getResourceKey(), query.getAnyKey(), pushTask);
        }

        Pair<Filter, Set<String>> fromFIQL = buildFromFIQL(query);
        return logic.push(
                query.getAnyTypeKey(), query.getResourceKey(), fromFIQL.getLeft(), fromFIQL.getRight(), pushTask);
    }

    @Override
    public List<ProvisioningReport> pull(final ReconQuery query, final PullTaskTO pullTask) {
        validate(query);

        if (query.getAnyKey() != null) {
            return logic.pull(
                    query.getAnyTypeKey(),
                    query.getResourceKey(),
                    query.getAnyKey(),
                    Optional.ofNullable(query.getMoreAttrsToGet()).orElseGet(Set::of),
                    pullTask);
        }

        Pair<Filter, Set<String>> fromFIQL = buildFromFIQL(query);
        return logic.pull(
                query.getAnyTypeKey(), query.getResourceKey(), fromFIQL.getLeft(), fromFIQL.getRight(), pullTask);
    }

    @Override
    public Response push(final AnyQuery query, final CSVPushSpec spec) {
        String realm = StringUtils.prependIfMissing(query.getRealm(), SyncopeConstants.ROOT_REALM);

        SearchCond searchCond = StringUtils.isBlank(query.getFiql())
                ? null
                : getSearchCond(query.getFiql(), realm);

        StreamingOutput sout = os -> logic.push(
                searchCond,
                pageable(query),
                realm,
                spec,
                os);

        return Response.ok(sout).
                type(RESTHeaders.TEXT_CSV).
                header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + AuthContextUtils.getDomain() + ".csv").
                build();
    }

    @Override
    public List<ProvisioningReport> pull(final CSVPullSpec spec, final InputStream csv) {
        return logic.pull(spec, csv);
    }
}
