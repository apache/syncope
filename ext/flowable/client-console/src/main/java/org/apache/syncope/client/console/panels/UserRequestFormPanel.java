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

import org.apache.syncope.client.ui.commons.panels.SyncopeFormPanel;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;

public abstract class UserRequestFormPanel extends SyncopeFormPanel<UserRequestForm> {

    private static final long serialVersionUID = 6064351260702815499L;

    public UserRequestFormPanel(final String id, final UserRequestForm form) {
        super(id, form);

        AjaxLink<String> userDetails = new AjaxLink<>("userDetails") {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                viewDetails(target);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(userDetails, ENABLE, IdRepoEntitlement.USER_READ);

        boolean enabled = form.getUserTO() != null;
        add(userDetails.setVisible(enabled).setEnabled(enabled));
    }

    protected abstract void viewDetails(AjaxRequestTarget target);
}
