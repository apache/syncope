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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.data.FIQLQueryDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FIQLQueryDataBinderImpl implements FIQLQueryDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(FIQLQueryDataBinder.class);

    protected final SearchCondVisitor searchCondVisitor;

    protected final UserDAO userDAO;

    protected final EntityFactory entityFactory;

    public FIQLQueryDataBinderImpl(
            final SearchCondVisitor searchCondVisitor,
            final UserDAO userDAO,
            final EntityFactory entityFactory) {

        this.searchCondVisitor = searchCondVisitor;
        this.userDAO = userDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public FIQLQuery create(final FIQLQueryTO fiqlQueryTO) {
        FIQLQuery fiqlQuery = entityFactory.newEntity(FIQLQuery.class);

        fiqlQuery.setOwner(userDAO.findByUsername(AuthContextUtils.getUsername()).
                orElseThrow(() -> new NotFoundException("User " + AuthContextUtils.getUsername())));

        return update(fiqlQuery, fiqlQueryTO);
    }

    @Override
    public FIQLQuery update(final FIQLQuery fiqlQuery, final FIQLQueryTO fiqlQueryTO) {
        fiqlQuery.setName(fiqlQueryTO.getName());
        fiqlQuery.setTarget(fiqlQueryTO.getTarget());

        SearchCond cond = SearchCondConverter.convert(searchCondVisitor, fiqlQueryTO.getFiql());
        if (!cond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(fiqlQueryTO.getFiql());
            throw sce;
        }
        fiqlQuery.setFIQL(fiqlQueryTO.getFiql());

        return fiqlQuery;
    }

    @Override
    public FIQLQueryTO getFIQLQueryTO(final FIQLQuery fiqlQuery) {
        FIQLQueryTO fiqlQueryTO = new FIQLQueryTO();

        fiqlQueryTO.setKey(fiqlQuery.getKey());
        fiqlQueryTO.setName(fiqlQuery.getName());
        fiqlQueryTO.setTarget(fiqlQuery.getTarget());
        fiqlQueryTO.setFiql(fiqlQuery.getFIQL());

        return fiqlQueryTO;
    }
}
