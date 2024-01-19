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

import java.util.Iterator;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynRealmDataBinderImpl implements DynRealmDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(DynRealmDataBinder.class);

    protected final AnyTypeDAO anyTypeDAO;

    protected final DynRealmDAO dynRealmDAO;

    protected final EntityFactory entityFactory;

    protected final SearchCondVisitor searchCondVisitor;

    public DynRealmDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final DynRealmDAO dynRealmDAO,
            final EntityFactory entityFactory,
            final SearchCondVisitor searchCondVisitor) {

        this.anyTypeDAO = anyTypeDAO;
        this.dynRealmDAO = dynRealmDAO;
        this.entityFactory = entityFactory;
        this.searchCondVisitor = searchCondVisitor;
    }

    protected void setDynMembership(final DynRealm dynRealm, final AnyType anyType, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(searchCondVisitor, dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(dynMembershipFIQL);
            throw sce;
        }

        DynRealmMembership dynMembership;
        if (dynRealm.getDynMembership(anyType).isPresent()) {
            dynMembership = dynRealm.getDynMembership(anyType).get();
        } else {
            dynMembership = entityFactory.newEntity(DynRealmMembership.class);
            dynMembership.setDynRealm(dynRealm);
            dynMembership.setAnyType(anyType);
            dynRealm.add(dynMembership);
        }
        dynMembership.setFIQLCond(dynMembershipFIQL);
    }

    @Override
    public DynRealm create(final DynRealmTO dynRealmTO) {
        return update(entityFactory.newEntity(DynRealm.class), dynRealmTO);
    }

    @Override
    public DynRealm update(final DynRealm toBeUpdated, final DynRealmTO dynRealmTO) {
        toBeUpdated.setKey(dynRealmTO.getKey());
        DynRealm dynRealm = dynRealmDAO.save(toBeUpdated);

        for (Iterator<? extends DynRealmMembership> itor = dynRealm.getDynMemberships().iterator(); itor.hasNext();) {
            DynRealmMembership memb = itor.next();
            memb.setDynRealm(null);
            itor.remove();
        }

        dynRealmTO.getDynMembershipConds().forEach((type, fiql) -> anyTypeDAO.findById(type).ifPresentOrElse(
                anyType -> setDynMembership(dynRealm, anyType, fiql),
                () -> LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), type)));

        return dynRealmDAO.saveAndRefreshDynMemberships(dynRealm);
    }

    @Override
    public DynRealmTO getDynRealmTO(final DynRealm dynRealm) {
        DynRealmTO dynRealmTO = new DynRealmTO();

        dynRealmTO.setKey(dynRealm.getKey());

        dynRealm.getDynMemberships().forEach(memb -> dynRealmTO.getDynMembershipConds().
                put(memb.getAnyType().getKey(), memb.getFIQLCond()));

        return dynRealmTO;
    }
}
