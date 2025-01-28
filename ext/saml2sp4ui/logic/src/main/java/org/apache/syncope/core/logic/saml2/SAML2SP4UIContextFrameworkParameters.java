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

import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.pac4j.core.context.FrameworkParameters;

public class SAML2SP4UIContextFrameworkParameters implements FrameworkParameters {

    private final String bindingType;

    private final SAML2Response saml2Response;

    public SAML2SP4UIContextFrameworkParameters(final String bindingType, final SAML2Response saml2Response) {
        this.bindingType = bindingType;
        this.saml2Response = saml2Response;
    }

    public String getBindingType() {
        return bindingType;
    }

    public SAML2Response getSaml2Response() {
        return saml2Response;
    }
}
