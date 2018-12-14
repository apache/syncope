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
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.Attr;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;

public class ParametersEditModalPanel extends AbstractModalPanel<Attr> {

    private static final long serialVersionUID = 4024126489500665435L;

    private final Attr attr;

    private final ConfRestClient restClient = new ConfRestClient();

    public ParametersEditModalPanel(
            final BaseModal<Attr> modal,
            final Attr attr,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.attr = attr;
        add(new ParametersDetailsPanel("parametersDetailsPanel", getItem()));
    }

    @Override
    public final Attr getItem() {
        return this.attr;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            restClient.set(attr);
            modal.close(target);
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("While creating or updating conf prarameter", e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName()
                    : e.getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
