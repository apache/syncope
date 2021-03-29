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
import org.apache.syncope.common.lib.to.OIDCRPTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class OIDCRPDirectoryPanel extends ClientAppDirectoryPanel<OIDCRPTO> {

    private static final long serialVersionUID = 1L;

    public OIDCRPDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, ClientAppType.OIDCRP, pageRef);

        OIDCRPTO defaultItem = new OIDCRPTO();

        this.addNewItemPanelBuilder(
                new ClientAppModalPanelBuilder<>(ClientAppType.OIDCRP, defaultItem, modal, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.CLIENTAPP_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomColumnFields(final List<IColumn<OIDCRPTO, String>> columns) {
        columns.add(new PropertyColumn<>(new StringResourceModel("clientId", this), "clientId", "clientId"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("redirectUris", this), "redirectUris", "redirectUris"));
        columns.add(new AbstractColumn<OIDCRPTO, String>(new StringResourceModel("logout")) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<OIDCRPTO>> item,
                    final String componentId,
                    final IModel<OIDCRPTO> rowModel) {

                item.add(new Label(componentId, StringUtils.EMPTY));
                if (StringUtils.isNotBlank(rowModel.getObject().getLogoutUri())) {
                    item.add(new AttributeModifier("class", "fa fa-check"));
                    item.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
                }
            }
        });
    }
}
