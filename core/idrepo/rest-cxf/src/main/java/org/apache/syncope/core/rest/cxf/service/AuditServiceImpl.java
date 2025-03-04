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
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.core.logic.AuditLogic;
import org.springframework.data.domain.Page;

public class AuditServiceImpl extends AbstractService implements AuditService {

    protected final AuditLogic logic;

    public AuditServiceImpl(final AuditLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<AuditConfTO> confs() {
        return logic.confs();
    }

    @Override
    public AuditConfTO getConf(final String key) {
        return logic.getConf(key);
    }

    @Override
    public void setConf(final AuditConfTO auditTO) {
        logic.setConf(auditTO);
    }

    @Override
    public void deleteConf(final String key) {
        logic.deleteConf(key);
    }

    @Override
    public void create(final AuditEventTO auditEvent) {
        logic.create(auditEvent);
    }

    @Override
    public List<OpEvent> events() {
        return logic.events();
    }

    @Override
    public PagedResult<AuditEventTO> search(final AuditQuery auditQuery) {
        Page<AuditEventTO> result = logic.search(
                auditQuery.getEntityKey(),
                auditQuery.getType(),
                auditQuery.getCategory(),
                auditQuery.getSubcategory(),
                auditQuery.getOp(),
                auditQuery.getOutcome(),
                auditQuery.getBefore(),
                auditQuery.getAfter(),
                pageable(auditQuery));

        return buildPagedResult(result);
    }
}
