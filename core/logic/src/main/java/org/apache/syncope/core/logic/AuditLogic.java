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
package org.apache.syncope.core.logic;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AuditTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AuditDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLogic extends AbstractTransactionalLogic<AuditTO> {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogic.class);

    @Autowired
    private AuditDataBinder binder;

    @Autowired
    private AuditDAO auditDAO;

    @Override
    protected AuditTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        String who = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; who == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    who = (String) args[i];
                } else if (args[i] instanceof AuditTO) {
                    who = ((AuditTO) args[i]).getWho();
                }
            }
        }

        if (who != null) {
            try {
                return binder.getAuditTO(auditDAO.findByUsername(who));
            } catch (final Throwable throwable) {
                LOG.debug("Unresolved reference", throwable);
                throw new UnresolvedReferenceException(throwable);
            }
        }

        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.AUDIT_SEARCH + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<AuditTO>> search(
        final SearchCond searchCond,
        final int page, final int size, final List<OrderByClause> orderBy,
        final String realm,
        final boolean details) {

        SearchCond effectiveSearchCond = searchCond == null ? auditDAO.getAllMatchingCond() : searchCond;
        int count = auditDAO.count(RealmUtils.getEffective(
            AuthContextUtils.getAuthorizations().get(StandardEntitlement.AUDIT_SEARCH), realm), effectiveSearchCond);

        List<AuditEntry> matching = auditDAO.search(RealmUtils.getEffective(
            AuthContextUtils.getAuthorizations().get(StandardEntitlement.AUDIT_SEARCH), realm),
            effectiveSearchCond, page, size, orderBy);
        List<AuditTO> result = matching.stream().
            map(binder::getAuditTO).
            collect(Collectors.toList());

        return Pair.of(count, result);
    }
}
