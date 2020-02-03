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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.core.logic.AuditLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditServiceImpl extends AbstractServiceImpl implements AuditService {

    @Autowired
    private AuditLogic logic;

    @Override
    public PagedResult<AuditEntryTO> search(final AuditQuery auditQuery) {
        Pair<Integer, List<AuditEntryTO>> result = logic.search(
                auditQuery.getEntityKey(),
                auditQuery.getPage(),
                auditQuery.getSize(),
                auditQuery.getType(),
                auditQuery.getCategory(),
                auditQuery.getSubcategory(),
                auditQuery.getEvents(),
                auditQuery.getResult(),
                getOrderByClauses(auditQuery.getOrderBy()));
        return buildPagedResult(result.getRight(), auditQuery.getPage(), auditQuery.getSize(), result.getLeft());
    }
}
