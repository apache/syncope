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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnyObjectServiceImpl extends AbstractAnyService<AnyObjectTO, AnyObjectPatch> implements AnyObjectService {

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnyObjectLogic logic;

    @Override
    protected AnyDAO<?> getAnyDAO() {
        return anyObjectDAO;
    }

    @Override
    protected AbstractAnyLogic<AnyObjectTO, AnyObjectPatch> getAnyLogic() {
        return logic;
    }

    @Override
    protected AnyObjectPatch newPatch(final String key) {
        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    public PagedResult<AnyObjectTO> search(final AnyQuery anyQuery) {
        if (StringUtils.isBlank(anyQuery.getFiql())
                || -1 == anyQuery.getFiql().indexOf(SpecialAttr.TYPE.toString())) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(SpecialAttr.TYPE.toString() + " is required in the FIQL string");
            throw sce;
        }

        return super.search(anyQuery);
    }

}
