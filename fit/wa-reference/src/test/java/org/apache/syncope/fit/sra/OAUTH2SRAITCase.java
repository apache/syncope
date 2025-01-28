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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.BeforeAll;

class OAUTH2SRAITCase extends AbstractOIDCITCase {

    @BeforeAll
    public static void startSRA() throws IOException, InterruptedException, TimeoutException {
        assumeTrue(OAUTH2SRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        doStartSRA("oauth2");
    }

    @BeforeAll
    public static void clientAppSetup() {
        assumeTrue(OAUTH2SRAITCase.class.equals(MethodHandles.lookup().lookupClass()));

        Properties props = new Properties();
        try (InputStream propStream = OAUTH2SRAITCase.class.getResourceAsStream("/sra-oauth2.properties")) {
            props.load(propStream);
        } catch (Exception e) {
            fail("Could not load /sra-oauth2.properties", e);
        }
        SRA_REGISTRATION_ID = "OAUTH2";
        CLIENT_APP_ID = 2L;
        CLIENT_ID = props.getProperty("sra.oauth2.client-id");
        assertNotNull(CLIENT_ID);
        CLIENT_SECRET = props.getProperty("sra.oauth2.client-secret");
        assertNotNull(CLIENT_SECRET);
        TOKEN_URI = props.getProperty("sra.oauth2.tokenUri");
        assertNotNull(TOKEN_URI);

        oidcClientAppSetup(
                OAUTH2SRAITCase.class.getName(), SRA_REGISTRATION_ID, CLIENT_APP_ID, CLIENT_ID, CLIENT_SECRET);
    }

    @Override
    protected void checkLogout(final CloseableHttpResponse response) {
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Override
    protected boolean checkIdToken() {
        return false;
    }
}
