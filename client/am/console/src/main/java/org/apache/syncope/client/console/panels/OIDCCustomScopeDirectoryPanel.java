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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.OIDCCustomScopeDirectoryPanel.CustomScope;
import org.apache.syncope.client.console.rest.OIDCOpEntityRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public abstract class OIDCCustomScopeDirectoryPanel
        extends DirectoryPanel<CustomScope, CustomScope, DirectoryDataProvider<CustomScope>, OIDCOpEntityRestClient> {

    private static final long serialVersionUID = -7283064059391373326L;

    public static class CustomScope implements EntityTO {

        private static final long serialVersionUID = -6041970196389196072L;

        private String scope;

        private final List<String> claims = new ArrayList<>();

        @Override
        public void setKey(final String key) {
            this.scope = key;
        }

        @Override
        public String getKey() {
            return scope;
        }

        public List<String> getClaims() {
            return claims;
        }
    }

    protected final OIDC oidc;

    protected OIDCCustomScopeDirectoryPanel(
            final OIDC oidc, final String id, final OIDCOpEntityRestClient restClient, final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        this.oidc = oidc;

        addNewItemPanelBuilder(new OIDCCustomScopeWizardBuilder(
                new CustomScope(), restClient, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.OIDC_OP_ENTITY_SET);

        disableCheckBoxes();

        initResultTable();

        modal.setWindowClosedCallback(target -> {
            oidc.refreshOIDCOpEntity(target);
            modal.show(false);
        });
    }

    protected Optional<OIDCOpEntityTO> getOIDCOpEntity() {
        Mutable<OIDCOpEntityTO> oidcOpEntity = restClient.get();
        return Optional.ofNullable(oidcOpEntity.get());
    }

    @Override
    protected List<IColumn<CustomScope, String>> getColumns() {
        List<IColumn<CustomScope, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(new StringResourceModel("scope", this), "scope", "scope"));
        columns.add(new PropertyColumn<>(new StringResourceModel("claims", this), "claims", "claims"));

        return columns;
    }

    @Override
    protected ActionsPanel<CustomScope> getActions(final IModel<CustomScope> model) {
        ActionsPanel<CustomScope> panel = super.getActions(model);

        getOIDCOpEntity().ifPresent(oidcOpEntity -> {
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final CustomScope ignore) {
                    send(OIDCCustomScopeDirectoryPanel.this, Broadcast.EXACT,
                            new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                }
            }, ActionLink.ActionType.EDIT, AMEntitlement.OIDC_OP_ENTITY_SET);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final CustomScope ignore) {
                    CustomScope clone = new CustomScope();
                    clone.setKey(model.getObject().getKey() + "_clone");
                    clone.getClaims().addAll(model.getObject().getClaims());

                    send(OIDCCustomScopeDirectoryPanel.this, Broadcast.EXACT,
                            new AjaxWizard.EditItemActionEvent<>(clone, target));
                }
            }, ActionLink.ActionType.CLONE, AMEntitlement.OIDC_OP_ENTITY_SET);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final CustomScope ignore) {
                    CustomScope customScope = model.getObject();

                    oidcOpEntity.getCustomScopes().remove(customScope.getKey());
                    try {
                        restClient.set(oidcOpEntity);

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        target.add(container);
                    } catch (SyncopeClientException e) {
                        LOG.error("While updating OIDC OP custom scopes", e);
                        SyncopeConsoleSession.get().onException(e);
                    }

                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }
            }, ActionLink.ActionType.DELETE, AMEntitlement.OIDC_OP_ENTITY_SET, true);
        });

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected DirectoryDataProvider<CustomScope> dataProvider() {
        return new CustomScopeDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_OIDC_CUSTOMSCOPES_PAGINATOR_ROWS;
    }

    protected class CustomScopeDataProvider extends DirectoryDataProvider<CustomScope> {

        private static final long serialVersionUID = 4725679400450513556L;

        public CustomScopeDataProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<CustomScope> iterator(final long first, final long count) {
            return getOIDCOpEntity().map(oidcOpEntity -> {
                List<CustomScope> list = oidcOpEntity.getCustomScopes().entrySet().stream().
                        map(entry -> {
                            CustomScope customScope = new CustomScope();
                            customScope.setKey(entry.getKey());
                            customScope.getClaims().addAll(entry.getValue());
                            return customScope;
                        }).
                        sorted(Comparator.comparing(CustomScope::getKey)).
                        collect(Collectors.toList());
                return list.subList((int) first, (int) first + (int) count).iterator();
            }).orElseGet(() -> Collections.emptyIterator());
        }

        @Override
        public long size() {
            return getOIDCOpEntity().map(oidcOpEntity -> oidcOpEntity.getCustomScopes().size()).orElse(0);
        }

        @Override
        public IModel<CustomScope> model(final CustomScope report) {
            return new CompoundPropertyModel<>(report);
        }
    }
}
