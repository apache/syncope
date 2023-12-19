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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.UrlUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.handler.RedirectRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SAML2SPBeforeLogout extends WebPage {

    private static final long serialVersionUID = 4666948447239743855L;

    public SAML2SPBeforeLogout(final PageParameters parameters) {
        super(parameters);

        String queryString = Optional.ofNullable(
                Session.get().getAttribute(SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID)).
                map(idp -> "?" + SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID
                + "=" + URLEncoder.encode(idp.toString(), StandardCharsets.UTF_8)).
                orElse("");

        RequestCycle.get().scheduleRequestHandlerAfterCurrent(new RedirectRequestHandler(
                UrlUtils.rewriteToContextRelative(
                        SAML2SP4UIConstants.URL_CONTEXT + "/logout" + queryString,
                        RequestCycle.get())));
    }
}
