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
package org.apache.syncope.client.console.commons;

import java.util.UUID;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * ITab available to perform authorization on it
 */
public abstract class ITabComponent extends Component implements ITab {

    private static final long serialVersionUID = -6908617525434875508L;

    private final IModel<String> title;

    /**
     * Constructor.
     *
     * @param title IModel used to represent the title of the tab. Must contain a string
     * @param roles authorized roles
     */
    public ITabComponent(final IModel<String> title, final String... roles) {
        super(UUID.randomUUID().toString());
        this.title = title;

        final ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        if (roles == null || roles.length == 0) {
            permissions.authorizeAll(RENDER);
        } else {
            permissions.authorize(RENDER, new Roles(roles));
        }
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    protected void onRender() {
        internalRenderComponent();
    }

    @Override
    public abstract WebMarkupContainer getPanel(String panelId);
}
