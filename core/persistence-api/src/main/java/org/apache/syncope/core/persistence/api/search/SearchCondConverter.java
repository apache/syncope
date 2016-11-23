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
package org.apache.syncope.core.persistence.api.search;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.SyncopeFiqlParser;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;

/**
 * Converts FIQL expressions to Syncope's {@link SearchCond}.
 */
public final class SearchCondConverter {

    /**
     * Parses a FIQL expression into Syncope's <tt>SearchCond</tt>, using CXF's <tt>FiqlParser</tt>.
     *
     * @param fiqlExpression FIQL string
     * @param realms optional realm to provide to {@link SearchCondVisitor}
     * @return {@link SearchCond} instance for given FIQL expression
     * @see SyncopeFiqlParser
     */
    public static SearchCond convert(final String fiqlExpression, final String... realms) {
        SyncopeFiqlParser<SearchBean> fiqlParser = new SyncopeFiqlParser<>(
                SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);

        try {
            SearchCondVisitor searchCondVisitor = new SearchCondVisitor();
            if (realms != null && realms.length > 0) {
                searchCondVisitor.setRealm(realms[0]);
            }
            searchCondVisitor.visit(fiqlParser.parse(fiqlExpression));
            return searchCondVisitor.getQuery();
        } catch (Exception e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(fiqlExpression);
            sce.getElements().add(ExceptionUtils.getRootCauseMessage(e));
            throw sce;
        }
    }

    private SearchCondConverter() {
        // empty constructor for static utility class        
    }
}
