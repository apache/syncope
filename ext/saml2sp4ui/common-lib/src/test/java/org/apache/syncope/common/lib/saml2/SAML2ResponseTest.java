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
package org.apache.syncope.common.lib.saml2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SAML2ResponseTest {

    @Test
    void spEntityIDAlwaysEndsWithSlash() {
        SAML2Response response = new SAML2Response();

        response.setSpEntityID("https://sp.example.com");
        assertEquals("https://sp.example.com/", response.getSpEntityID());

        response.setSpEntityID("https://sp.example.com/");
        assertEquals("https://sp.example.com/", response.getSpEntityID());

        response.setSpEntityID(null);
        assertNull(response.getSpEntityID());
    }

    @Test
    void messageAndSAMLData() {
        SAML2Response response = new SAML2Response();
        response.setIdpEntityID("https://idp.example.com");
        response.setSpEntityID("https://sp.example.com");
        response.setUrlContext("saml2sp4ui");
        response.setSamlResponse("response");
        response.setRelayState("relayState");

        assertEquals("https://idp.example.com", response.getIdpEntityID());
        assertEquals("https://sp.example.com/", response.getSpEntityID());
        assertEquals("saml2sp4ui", response.getUrlContext());
        assertEquals("response", response.getSamlResponse());
        assertEquals("relayState", response.getRelayState());
    }
}
