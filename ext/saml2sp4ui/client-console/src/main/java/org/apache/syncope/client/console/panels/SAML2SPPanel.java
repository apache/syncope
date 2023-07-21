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
package org.apache.syncope.client.console.panels;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.SAML2SPRestClient;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.UrlUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAML2SPPanel extends Panel {

    private static final long serialVersionUID = 2806917712636062674L;

    protected static final Logger LOG = LoggerFactory.getLogger(SAML2SPPanel.class);

    public SAML2SPPanel(final String id, final SAML2SPRestClient restClient) {
        super(id);

        add(new Link<Void>("downloadMetadata") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    String spEntityID = StringUtils.substringBefore(
                            RequestCycle.get().getUrlRenderer().renderFullUrl(
                                    Url.parse(UrlUtils.rewriteToContextRelative(
                                            SAML2SP4UIConstants.URL_CONTEXT, RequestCycle.get()))),
                            SAML2SP4UIConstants.URL_CONTEXT);
                    IResourceStream stream = restClient.getMetadata(spEntityID);

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(SyncopeConsoleSession.get().getDomain() + "-SAML-SP-Metadata.xml");
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    LOG.error("While exporting SAML 2.0 SP metadata", e);
                    SyncopeConsoleSession.get().onException(e);
                }
            }
        });
    }
}
