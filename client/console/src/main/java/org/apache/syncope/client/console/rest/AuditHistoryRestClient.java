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

import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

import java.util.List;

public class AuditHistoryRestClient extends BaseRestClient {
    private static final long serialVersionUID = -381814125643246243L;

    public List<AuditEntryTO> search(final String key,
                                     final int page,
                                     final int size,
                                     final SortParam<String> sort,
                                     final List<String> events,
                                     final AuditElements.Result result) {
        AuditQuery query = new AuditQuery.Builder(key)
            .size(size)
            .page(page)
            .events(events)
            .result(result)
            .orderBy(toOrderBy(sort))
            .build();
        return getService(AuditService.class).search(query).getResult();
    }

    public int count(final String key,
                     final List<String> events,
                     final AuditElements.Result result) {
        AuditQuery query = new AuditQuery.Builder(key)
            .events(events)
            .result(result)
            .build();
        return getService(AuditService.class).search(query).getTotalCount();
    }
}

