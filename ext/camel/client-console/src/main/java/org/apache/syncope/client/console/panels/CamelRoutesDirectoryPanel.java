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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.CamelRoutesDirectoryPanel.CamelRoutesProvider;
import org.apache.syncope.client.console.rest.CamelRoutesRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class CamelRoutesDirectoryPanel extends DirectoryPanel<
        CamelRouteTO, CamelRouteTO, CamelRoutesProvider, CamelRoutesRestClient> {

    private static final long serialVersionUID = 3727444742501082182L;

    private static final String PREF_CAMEL_ROUTES_PAGINATOR_ROWS = "camel.routes.paginator.rows";

    private final BaseModal<String> utilityModal = new BaseModal<>("outer");

    private final AnyTypeKind anyTypeKind;

    public CamelRoutesDirectoryPanel(final String id, final PageReference pageRef, final AnyTypeKind anyTypeKind) {
        super(id, new Builder<CamelRouteTO, CamelRouteTO, CamelRoutesRestClient>(new CamelRoutesRestClient(), pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<CamelRouteTO> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        }.disableCheckBoxes());
        setOutputMarkupId(true);

        this.anyTypeKind = anyTypeKind;
        setFooterVisibility(true);

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);
        utilityModal.size(Modal.Size.Large);
        utilityModal.addSubmitButton();

        initResultTable();
    }

    @Override
    protected CamelRoutesProvider dataProvider() {
        return new CamelRoutesProvider(anyTypeKind, rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_CAMEL_ROUTES_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<CamelRouteTO, String>> getColumns() {
        final List<IColumn<CamelRouteTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new ResourceModel("key"), "key", "key"));
        return columns;
    }

    @Override
    public ActionsPanel<CamelRouteTO> getActions(final IModel<CamelRouteTO> model) {
        final ActionsPanel<CamelRouteTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final CamelRouteTO ignore) {
                final CamelRouteTO route = CamelRoutesRestClient.read(anyTypeKind, model.getObject().getKey());

                utilityModal.header(Model.of(route.getKey()));
                utilityModal.setContent(new XMLEditorPanel(
                    utilityModal, new PropertyModel<>(route, "content"), filtered, pageRef) {

                    private static final long serialVersionUID = 5488080606102212554L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target) {
                        try {
                            CamelRoutesRestClient.update(anyTypeKind, route);
                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While creating or updating CamelRouteTO", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                        }
                        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }

                });
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.EDIT, CamelEntitlement.ROUTE_UPDATE);

        return panel;
    }

    protected static final class CamelRoutesProvider extends DirectoryDataProvider<CamelRouteTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final AnyTypeKind anyTypeKind;

        private final SortableDataProviderComparator<CamelRouteTO> comparator;

        private CamelRoutesProvider(final AnyTypeKind anyTypeKind, final int paginatorRows) {
            super(paginatorRows);
            this.anyTypeKind = anyTypeKind;
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<CamelRouteTO> iterator(final long first, final long count) {
            List<CamelRouteTO> list = CamelRoutesRestClient.list(anyTypeKind);
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return CamelRoutesRestClient.list(anyTypeKind).size();
        }

        @Override
        public IModel<CamelRouteTO> model(final CamelRouteTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
