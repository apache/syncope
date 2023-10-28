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

import com.nimbusds.jwt.SignedJWT;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AccessTokenDirectoryPanel.AccessTokenDataProvider;
import org.apache.syncope.client.console.rest.AccessTokenRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class AccessTokenDirectoryPanel
        extends DirectoryPanel<AccessTokenTO, AccessTokenTO, AccessTokenDataProvider, AccessTokenRestClient> {

    private static final long serialVersionUID = -6903586269155682961L;

    protected AccessTokenDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);

        setShowResultPanel(true);

        modal.size(Modal.Size.Large);
        initResultTable();
    }

    @Override
    protected AccessTokenDataProvider dataProvider() {
        return new AccessTokenDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_ACCESS_TOKEN_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<AccessTokenTO, String>> getColumns() {
        List<IColumn<AccessTokenTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME,
                Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(new ResourceModel("owner"), "owner", "owner"));

        columns.add(new AbstractColumn<>(new ResourceModel("issuedAt", "")) {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<AccessTokenTO>> cellItem,
                    final String componentId,
                    final IModel<AccessTokenTO> model) {

                try {
                    SignedJWT jwt = SignedJWT.parse(model.getObject().getBody());
                    cellItem.add(new Label(
                            componentId,
                            SyncopeConsoleSession.get().getDateFormat().format(jwt.getJWTClaimsSet().getIssueTime())));
                } catch (ParseException e) {
                    LOG.error("Could not parse JWT {}", model.getObject().getBody(), e);
                    cellItem.add(new Label(componentId, StringUtils.EMPTY));
                }
            }
        });

        columns.add(new DatePropertyColumn<>(new ResourceModel("expirationTime"), "expirationTime", "expirationTime"));

        return columns;
    }

    @Override
    public ActionsPanel<AccessTokenTO> getActions(final IModel<AccessTokenTO> model) {
        ActionsPanel<AccessTokenTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AccessTokenTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.ACCESS_TOKEN_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class AccessTokenDataProvider extends DirectoryDataProvider<AccessTokenTO> {

        private static final long serialVersionUID = 6267494272884913376L;

        public AccessTokenDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("owner", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<AccessTokenTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.list((page < 0 ? 0 : page) + 1, paginatorRows, getSort()).iterator();
        }

        @Override
        public long size() {
            return restClient.count();
        }

        @Override
        public IModel<AccessTokenTO> model(final AccessTokenTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    public static class Builder
            extends DirectoryPanel.Builder<AccessTokenTO, AccessTokenTO, AccessTokenRestClient> {

        private static final long serialVersionUID = 5088962796986706805L;

        public Builder(final AccessTokenRestClient restClient, final PageReference pageRef) {
            super(restClient, pageRef);
        }

        @Override
        protected WizardMgtPanel<AccessTokenTO> newInstance(final String id, final boolean wizardInModal) {
            return new AccessTokenDirectoryPanel(id, this);
        }
    }
}
