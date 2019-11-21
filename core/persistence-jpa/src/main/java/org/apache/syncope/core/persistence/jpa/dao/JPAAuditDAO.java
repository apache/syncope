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

package org.apache.syncope.core.persistence.jpa.dao;

import org.apache.syncope.core.persistence.api.dao.AuditDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Transactional(rollbackFor = Throwable.class)
@Repository
public class JPAAuditDAO implements AuditDAO<AuditEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(JPAAuditDAO.class);

    @Override
    public AuditEntry findByUsername(final String who) {
        return null;
    }

    @Override
    public SearchCond getAllMatchingCond() {
        AnyCond idCond = new AnyCond(AttributeCond.Type.ISNOTNULL);
        idCond.setSchema("logger");
        return SearchCond.getLeafCond(idCond);
    }

    @Override
    public int count(final Set<String> adminRealms, final SearchCond cond) {
        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return 0;
        }
        return 0;
    }

    @Override
    public List<AuditEntry> search(final Set<String> effective, final SearchCond effectiveSearchCond,
                                   final int page, final int size, final List<OrderByClause> orderBy) {
        return null;
    }
}
