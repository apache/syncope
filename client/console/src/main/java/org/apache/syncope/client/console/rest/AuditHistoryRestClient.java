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
package org.apache.syncope.client.console.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.syncope.client.console.audit.AnyTOAuditEntryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

import javax.ws.rs.core.Response;

import java.util.List;

public class AuditHistoryRestClient extends BaseRestClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long serialVersionUID = -381814125643246243L;

    public List<AuditEntryTO> search(final String key,
                                     final int page,
                                     final int size,
                                     final SortParam<String> sort,
                                     final List<String> events,
                                     final List<AuditElements.Result> results) {
        AuditQuery query = new AuditQuery.Builder()
            .size(size)
            .key(key)
            .page(page)
            .events(events)
            .results(results)
            .orderBy(toOrderBy(sort))
            .build();
        return getService(AuditService.class).search(query).getResult();
    }

    public List<AuditEntryTO> search(final String key,
                                     final SortParam<String> sort,
                                     final List<String> events,
                                     final List<AuditElements.Result> results) {
        AuditQuery query = new AuditQuery.Builder()
            .key(key)
            .events(events)
            .results(results)
            .orderBy(toOrderBy(sort))
            .build();
        return getService(AuditService.class).search(query).getResult();
    }

    public int count(final String key,
                     final List<String> events,
                     final List<AuditElements.Result> results) {
        AuditQuery query = new AuditQuery.Builder()
            .key(key)
            .events(events)
            .results(results)
            .build();
        return getService(AuditService.class).search(query).getTotalCount();
    }

    public Response restore(final AnyTOAuditEntryBean entryBean, final AnyTypeKind anyTypeKind, final AnyTO anyTO) {
        if (anyTypeKind == AnyTypeKind.USER) {
            UserTO userTO = (UserTO) MAPPER.convertValue(entryBean.getBefore(), anyTypeKind.getTOClass());
            userTO.setPassword(((UserTO) anyTO).getPassword());
            userTO.setSecurityAnswer(((UserTO) anyTO).getSecurityAnswer());
            return getService(UserService.class).update(userTO);
        }
        if (anyTypeKind == AnyTypeKind.GROUP) {
            GroupTO groupTO = (GroupTO) MAPPER.convertValue(entryBean.getBefore(), anyTypeKind.getTOClass());
            return getService(GroupService.class).update(groupTO);
        }
        if (anyTypeKind == AnyTypeKind.ANY_OBJECT) {
            AnyObjectTO anyObjectTO = (AnyObjectTO)
                MAPPER.convertValue(entryBean.getBefore(), anyTypeKind.getTOClass());
            return getService(AnyObjectService.class).update(anyObjectTO);
        }
        throw SyncopeClientException.build(ClientExceptionType.InvalidAnyObject);
    }
}

