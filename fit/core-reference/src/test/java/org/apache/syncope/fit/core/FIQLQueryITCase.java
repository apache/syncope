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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.FIQLQueryService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class FIQLQueryITCase extends AbstractITCase {

    @Test
    public void crud() {
        FIQLQueryService fiqlQueryService =
                CLIENT_FACTORY.create("bellini", "password").getService(FIQLQueryService.class);

        int before = fiqlQueryService.list(AnyTypeKind.USER.name()).size();

        FIQLQueryTO query = new FIQLQueryTO();
        query.setFiql("INVALID");
        query.setName("fancy name");
        query.setTarget(AnyTypeKind.USER.name());

        try {
            fiqlQueryService.create(query);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }

        query.setFiql("username=~*one*");
        Response response = fiqlQueryService.create(query);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);

        query = fiqlQueryService.read(key);
        assertNotNull(query.getKey());
        assertEquals("username=~*one*", query.getFiql());
        assertEquals("fancy name", query.getName());

        List<FIQLQueryTO> queries = fiqlQueryService.list(AnyTypeKind.USER.name());
        assertEquals(before + 1, queries.size());
        assertTrue(queries.stream().anyMatch(q -> key.equals(q.getKey())));

        query.setFiql("email==ciao@bao.it");
        query.setName("not so fancy");
        fiqlQueryService.update(query);

        query = fiqlQueryService.read(key);
        assertEquals("email==ciao@bao.it", query.getFiql());
        assertEquals("not so fancy", query.getName());

        fiqlQueryService.delete(key);

        queries = fiqlQueryService.list(AnyTypeKind.USER.name());
        assertEquals(before, queries.size());
        assertFalse(queries.contains(query));
    }
}
