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

import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAML2SPDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2SPDetector.class);

    private static Boolean ENABLED;

    public static boolean isSAML2SPAvailable() {
        synchronized (LOG) {
            if (ENABLED == null) {
                try {
                    new SyncopeClientFactoryBean().
                            setAddress(AbstractITCase.ADDRESS).
                            setContentType(SyncopeClientFactoryBean.ContentType.XML).
                            create(new AnonymousAuthenticationHandler(
                                    AbstractITCase.ANONYMOUS_UNAME, AbstractITCase.ANONYMOUS_KEY)).
                            getService(SAML2SPService.class).getMetadata("http://localhost:9080/syncope", "saml2sp");
                    ENABLED = true;
                } catch (Exception e) {
                    // ignore
                    ENABLED = false;
                }
            }
        }
        return ENABLED;
    }
}
