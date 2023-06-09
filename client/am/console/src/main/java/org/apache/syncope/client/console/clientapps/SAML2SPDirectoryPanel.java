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
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.StringResourceModel;

public class SAML2SPDirectoryPanel extends ClientAppDirectoryPanel<SAML2SPClientAppTO> {

    private static final long serialVersionUID = 7724172966123776889L;

    public SAML2SPDirectoryPanel(final String id, final ClientAppRestClient restClient, final PageReference pageRef) {
        super(id, restClient, ClientAppType.SAML2SP, pageRef);

        SAML2SPClientAppTO defaultItem = new SAML2SPClientAppTO();

        addNewItemPanelBuilder(new ClientAppModalPanelBuilder<>(
                ClientAppType.SAML2SP,
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
    protected void addCustomColumnFields(final List<IColumn<SAML2SPClientAppTO, String>> columns) {
        columns.add(new PropertyColumn<>(new StringResourceModel("entityId", this), "entityId", "entityId"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("signAssertions", this), "signAssertions", "signAssertions"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("signResponses", this), "signResponses", "signResponses"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("encryptionOptional", this), "encryptionOptional", "encryptionOptional"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("encryptAssertions", this), "encryptAssertions", "encryptAssertions"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("requiredNameIdFormat", this), "requiredNameIdFormat", "requiredNameIdFormat"));
    }
}
