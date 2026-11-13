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
package org.apache.syncope.core.logic.saml2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.HttpMethod;
import java.util.Optional;
import org.apache.syncope.common.lib.saml2.SAML2Constants;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.junit.jupiter.api.Test;

class SAML2SP4UIContextTest {

    private static SAML2Response response() {
        SAML2Response response = new SAML2Response();
        response.setSamlResponse("samlResponse");
        response.setRelayState("relayState");
        return response;
    }

    @Test
    void postBinding() {
        SAML2SP4UIContext context = new SAML2SP4UIContext(SAML2BindingType.POST.getUri(), response());

        assertEquals(HttpMethod.POST, context.getRequestMethod());
    }

    @Test
    void redirectBinding() {
        SAML2SP4UIContext context = new SAML2SP4UIContext(SAML2BindingType.REDIRECT.getUri(), response());

        assertEquals(HttpMethod.GET, context.getRequestMethod());
    }

    @Test
    void requestParameters() {
        SAML2SP4UIContext context = new SAML2SP4UIContext(SAML2BindingType.POST.getUri(), response());

        assertEquals(Optional.of("samlResponse"),
                context.getRequestParameter(SAML2Constants.SAML_RESPONSE));
        assertEquals(Optional.of("relayState"),
                context.getRequestParameter(SAML2Constants.RELAY_STATE));
        assertEquals(Optional.empty(), context.getRequestParameter("other"));

        assertTrue(context.getRequestParameters().isEmpty());
        assertTrue(context.getQueryString().isEmpty());
        assertTrue(context.getRequestAttribute("other").isEmpty());
        assertTrue(context.getRequestHeader("other").isEmpty());
        assertFalse(context.isSecure());
        assertTrue(context.getRequestCookies().isEmpty());
    }

    @Test
    void missingResponseData() {
        SAML2SP4UIContext context = new SAML2SP4UIContext(SAML2BindingType.POST.getUri(), new SAML2Response());

        assertEquals(Optional.empty(), context.getRequestParameter(SAML2Constants.SAML_RESPONSE));
        assertEquals(Optional.empty(), context.getRequestParameter(SAML2Constants.RELAY_STATE));
    }
}
