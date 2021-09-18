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

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.core.logic.AuditLogic;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl extends AbstractService implements AuditService {

    protected final AuditLogic logic;

    public AuditServiceImpl(final AuditLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<AuditConfTO> list() {
        return logic.list();
    }

    @Override
    public AuditConfTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public void create(final AuditConfTO auditTO) {
        logic.create(auditTO);
    }

    @Override
    public void update(final AuditConfTO auditTO) {
        logic.update(auditTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public void create(final AuditEntry auditEntry) {
        logic.create(auditEntry);
    }

    @Override
    public List<EventCategory> events() {
        return logic.events();
    }

    @Override
    public PagedResult<AuditEntry> search(final AuditQuery auditQuery) {
        Pair<Integer, List<AuditEntry>> result = logic.search(
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
