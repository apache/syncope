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
package org.apache.syncope.core.rest.cxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.PasswordResetThrottleException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

public class RestServiceExceptionMapperTest {

    @Test
    public void passwordResetThrottleReturnsTooManyRequests() {
        Response response = new RestServiceExceptionMapper(mock(Environment.class)).
                toResponse(new PasswordResetThrottleException(42));

        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
        assertEquals("42", response.getHeaderString(HttpHeaders.RETRY_AFTER));
        assertEquals(ClientExceptionType.TooManyRequests.name(), response.getHeaderString(RESTHeaders.ERROR_CODE));
    }
}
