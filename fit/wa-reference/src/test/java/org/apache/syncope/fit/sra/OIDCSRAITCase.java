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
package org.apache.syncope.fit.sra;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeAll;

class OIDCSRAITCase extends AbstractOIDCITCase {

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("oidc");
    }

    @BeforeAll
    public static void clientAppSetup() {
        assumeTrue(OIDCSRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        Properties props = new Properties();
        try (InputStream propStream = OIDCSRAITCase.class.getResourceAsStream("/sra-oidc.properties")) {
            props.load(propStream);
        } catch (Exception e) {
            fail("Could not load /sra-oidc.properties", e);
        }
        SRA_REGISTRATION_ID = "OIDC";
        CLIENT_APP_ID = 1L;
        CLIENT_ID = props.getProperty("sra.oidc.client-id");
        assertNotNull(CLIENT_ID);
        CLIENT_SECRET = props.getProperty("sra.oidc.client-secret");
        assertNotNull(CLIENT_SECRET);
        TOKEN_URI = WA_ADDRESS + "/oidc/accessToken";

        oidcClientAppSetup(
                OIDCSRAITCase.class.getName(), SRA_REGISTRATION_ID, CLIENT_APP_ID, CLIENT_ID, CLIENT_SECRET);
    }

    @Override
    protected boolean checkIdToken() {
        return true;
    }
}
