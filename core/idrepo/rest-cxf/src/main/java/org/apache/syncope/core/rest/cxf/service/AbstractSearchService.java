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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;

public abstract class AbstractSearchService extends AbstractService {

    protected final SearchCondVisitor searchCondVisitor;

    public AbstractSearchService(final SearchCondVisitor searchCondVisitor) {
        this.searchCondVisitor = searchCondVisitor;
    }

    protected SearchCond getSearchCond(final String fiql, final String realm) {
        try {
            searchCondVisitor.setRealm(realm);
            SearchCondition<SearchBean> sc = searchContext.getCondition(fiql, SearchBean.class);
            sc.accept(searchCondVisitor);

            return searchCondVisitor.getQuery();
        } catch (Exception e) {
            LOG.error("Invalid FIQL expression: {}", fiql, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(fiql);
            sce.getElements().add(ExceptionUtils.getRootCauseMessage(e));
            throw sce;
        }
    }
}
