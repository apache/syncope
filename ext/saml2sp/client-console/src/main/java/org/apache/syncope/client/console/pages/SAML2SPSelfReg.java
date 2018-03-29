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
package org.apache.syncope.client.console.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SAML2SPSelfReg extends WebPage {

    private static final long serialVersionUID = -4330637558823990359L;

    private static final String SAML_ACCESS_ERROR = 
            "SAML 2.0 error - Admin Console does not support Self Registration";

    public SAML2SPSelfReg(final PageParameters parameters) {
        super(parameters);

        PageParameters params = new PageParameters();
        params.add("errorMessage", SAML_ACCESS_ERROR);
        setResponsePage(Login.class, params);
    }
}
