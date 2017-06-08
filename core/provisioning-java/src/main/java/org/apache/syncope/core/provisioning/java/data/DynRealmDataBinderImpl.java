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
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DynRealmDataBinderImpl implements DynRealmDataBinder {

    @Autowired
    private DynRealmDAO dynRealmDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public DynRealm create(final DynRealmTO dynRealmTO) {
        return update(entityFactory.newEntity(DynRealm.class), dynRealmTO);
    }

    @Override
    public DynRealm update(final DynRealm dynRealm, final DynRealmTO dynRealmTO) {
        dynRealm.setKey(dynRealmTO.getKey());

        SearchCond cond = SearchCondConverter.convert(dynRealmTO.getCond());
        if (!cond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(dynRealmTO.getCond());
            throw sce;
        }
        dynRealm.setFIQLCond(dynRealmTO.getCond());

        return dynRealmDAO.save(dynRealm);
    }

    @Override
    public DynRealmTO getDynRealmTO(final DynRealm dynRealm) {
        DynRealmTO dynRealmTO = new DynRealmTO();

        dynRealmTO.setKey(dynRealm.getKey());
        dynRealmTO.setCond(dynRealm.getFIQLCond());

        return dynRealmTO;
    }

}
