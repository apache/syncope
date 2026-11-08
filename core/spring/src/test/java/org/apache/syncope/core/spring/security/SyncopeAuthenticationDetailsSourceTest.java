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
package org.apache.syncope.core.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.junit.jupiter.api.Test;

class SyncopeAuthenticationDetailsSourceTest {

    @Test
    void buildDetailsFromRequestHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(RESTHeaders.DOMAIN)).thenReturn("other");
        when(request.getHeader(RESTHeaders.DELEGATED_BY)).thenReturn("delegator");

        SyncopeAuthenticationDetails details = new SyncopeAuthenticationDetailsSource().buildDetails(request);

        assertEquals("other", details.getDomain());
        assertEquals("delegator", details.getDelegatedBy());
    }

    @Test
    void buildDetailsWithoutHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        SyncopeAuthenticationDetails details = new SyncopeAuthenticationDetailsSource().buildDetails(request);

        assertEquals(SyncopeConstants.MASTER_DOMAIN, details.getDomain());
        assertNull(details.getDelegatedBy());
    }

    @Test
    void buildDetailsWithBlankDomain() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(RESTHeaders.DOMAIN)).thenReturn("   ");

        SyncopeAuthenticationDetails details = new SyncopeAuthenticationDetailsSource().buildDetails(request);

        assertEquals(SyncopeConstants.MASTER_DOMAIN, details.getDomain());
    }
}
