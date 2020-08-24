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
package org.apache.syncope.client.enduser.resources;

import org.apache.syncope.client.enduser.pages.OIDCClientLogout;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.syncope.client.ui.commons.resources.oidcc4ui.LogoutResource;
import org.apache.wicket.markup.html.WebPage;

@Resource(
        key = OIDCC4UIConstants.URL_CONTEXT + ".logout",
        path = "/" + OIDCC4UIConstants.URL_CONTEXT + "/logout")
public class EnduserLogoutResource extends LogoutResource {

    private static final long serialVersionUID = -4250716706885039749L;

    @Override
    protected Class<? extends WebPage> getLogoutPageClass() {
        return OIDCClientLogout.class;
    }
}
