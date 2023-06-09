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
package org.apache.syncope.client.console.clientapps;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanConditionColumn;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class OIDCRPDirectoryPanel extends ClientAppDirectoryPanel<OIDCRPClientAppTO> {

    private static final long serialVersionUID = -9182884609300468766L;

    public OIDCRPDirectoryPanel(final String id, final ClientAppRestClient restClient, final PageReference pageRef) {
        super(id, restClient, ClientAppType.OIDCRP, pageRef);

        OIDCRPClientAppTO defaultItem = new OIDCRPClientAppTO();

        addNewItemPanelBuilder(new ClientAppModalPanelBuilder<>(
                ClientAppType.OIDCRP,
                defaultItem,
                modal,
                policyRestClient,
                clientAppRestClient,
                realmRestClient,
                pageRef),
                true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.CLIENTAPP_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomColumnFields(final List<IColumn<OIDCRPClientAppTO, String>> columns) {
        columns.add(new PropertyColumn<>(new StringResourceModel("clientId", this), "clientId", "clientId"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("redirectUris", this), "redirectUris", "redirectUris"));
        columns.add(new BooleanConditionColumn<>(new StringResourceModel("logout")) {

            private static final long serialVersionUID = -8236820422411536323L;

            @Override
            protected boolean isCondition(final IModel<OIDCRPClientAppTO> rowModel) {
                return StringUtils.isNotBlank(rowModel.getObject().getLogoutUri());
            }
        });
    }
}
