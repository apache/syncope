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
package org.apache.syncope.client.enduser.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.wicket.NonResettingRestartException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class HomePage extends WebPage {

    private static final long serialVersionUID = -3422492668689122688L;

    public HomePage(final PageParameters parameters) {
        super(parameters);

        StringBuilder redirectUrl = new StringBuilder("/app/");
        if (!parameters.get("errorMessage").isNull()) {
            redirectUrl.append("#!self?errorMessage=");
            appendMessage(redirectUrl, parameters.get("errorMessage").toString());
        } else if (!parameters.get("successMessage").isNull()) {
            redirectUrl.append("#!self?successMessage=");
            appendMessage(redirectUrl, parameters.get("successMessage").toString());
        } else if (!parameters.get("saml2SPUserAttrs").isNull()) {
            redirectUrl.append("#!self-saml2sp");
        }
        throw new NonResettingRestartException(redirectUrl.toString());
    }

    private void appendMessage(final StringBuilder redirectUrl, final String message) {
        try {
            redirectUrl.append(URLEncoder.encode(message, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            redirectUrl.append("Generic error");
        }
    }
}
