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

import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIDCClientSelfReg extends WebPage {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCClientSelfReg.class);

    private static final long serialVersionUID = -2533879075075645461L;

    public OIDCClientSelfReg(final PageParameters parameters) {
        super(parameters);

        PageParameters params = new PageParameters();
        try {
            params.add(Self.NEW_USER_PARAM,
                    ((ServletWebRequest) getRequest()).getContainerRequest().
                            getSession().getAttribute(OIDCC4UIConstants.OIDCC4UI_NEW_USER));
        } catch (Exception e) {
            LOG.error("While getting user data from social registration", e);

            params.add("errorMessage", "OpenID Connect error - while getting user data from social registration");
        }
        setResponsePage(Self.class, params);
    }
}
