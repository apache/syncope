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
package org.apache.syncope.fit;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;
import org.apache.syncope.fit.core.SCIMITCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SCIMDetector.class);

    private static Boolean ENABLED;

    public static boolean isSCIMAvailable() {
        synchronized (LOG) {
            if (ENABLED == null) {
                try {
                    Response response = WebClient.create(SCIMITCase.SCIM_ADDRESS).path("ServiceProviderConfigs").
                            accept(SCIMConstants.APPLICATION_SCIM_JSON_TYPE).
                            header(HttpHeaders.AUTHORIZATION,
                                    "Basic " + DatatypeConverter.printBase64Binary(
                                            (AbstractITCase.ADMIN_UNAME + ":" + AbstractITCase.ADMIN_PWD).getBytes())).
                            get();
                    ENABLED = response.getStatus() == 200;
                } catch (Exception e) {
                    // ignore
                    ENABLED = false;
                }
            }
        }
        return ENABLED;
    }
}
