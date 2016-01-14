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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.SearchableDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.CamelRoutesPage;
import org.apache.syncope.client.console.pages.CamelRoutesPopupPage;
import org.apache.syncope.client.console.panels.CamelRoutesPanel.CamelRoutesProvider;
import org.apache.syncope.client.console.rest.CamelRoutesRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CamelEntitlement;
import org.apache.syncope.common.rest.api.service.CamelRouteService;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class CamelRoutesPanel extends AbstractSearchResultPanel<
        CamelRouteTO, CamelRouteTO, CamelRoutesProvider, CamelRoutesRestClient> {

    private static final long serialVersionUID = 3727444742501082182L;

    private static final int CAMELROUTE_WIN_HEIGHT = 480;

    private static final int CAMELROUTE_WIN_WIDTH = 800;

    private final AnyTypeKind anyTypeKind;

    private ModalWindow editCamelRouteWin;

    public CamelRoutesPanel(final String id, final PageReference pageRef, final AnyTypeKind anyTypeKind) {
        super(id, new Builder<CamelRouteTO, CamelRouteTO, CamelRoutesRestClient>(
                new CamelRoutesRestClient(), pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<CamelRouteTO> newInstance(final String id) {
                return new CamelRoutesPanel(id, this, anyTypeKind);
            }
        }.disableCheckBoxes());

        this.anyTypeKind = anyTypeKind;
        setFooterVisibility(true);
        modal.addSumbitButton();
        modal.size(Modal.Size.Large);
        initResultTable();

        editCamelRouteWin = new ModalWindow("editCamelRouteWin");
        editCamelRouteWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editCamelRouteWin.setInitialHeight(CAMELROUTE_WIN_HEIGHT);
        editCamelRouteWin.setInitialWidth(CAMELROUTE_WIN_WIDTH);
        editCamelRouteWin.setCookieName("editCamelRouteWin-modal");
        add(editCamelRouteWin);
    }

    private CamelRoutesPanel(
            final String id,
            final Builder<CamelRouteTO, CamelRouteTO, CamelRoutesRestClient> builder,
            final AnyTypeKind anyTypeKind) {

        super(id, builder);
        this.anyTypeKind = anyTypeKind;
    }

    @Override
    protected CamelRoutesProvider dataProvider() {
        return new CamelRoutesProvider(anyTypeKind, rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return CamelRoutesPage.PREF_CAMEL_ROUTES_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    @Override
    protected List<IColumn<CamelRouteTO, String>> getColumns() {
        final List<IColumn<CamelRouteTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<CamelRouteTO, String>(new ResourceModel("name"), "name", "name"));
        columns.add(new AbstractColumn<CamelRouteTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<CamelRouteTO>> item, final String componentId,
                    final IModel<CamelRouteTO> model) {

                ActionLinksPanel.Builder<Serializable> actionLinks = ActionLinksPanel.builder(page.getPageReference());
                actionLinks.setDisableIndicator(true);
                actionLinks.addWithRoles(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        editCamelRouteWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new CamelRoutesPopupPage(restClient.read(model.getObject().getKey()));
                            }

                        });

                        editCamelRouteWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, CamelEntitlement.ROUTE_READ);
                item.add(actionLinks.build(componentId));
            }
        });

        return columns;

    }

    protected final class CamelRoutesProvider extends SearchableDataProvider<CamelRouteTO> {

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
            List<CamelRouteTO> list = SyncopeConsoleSession.get().getService(CamelRouteService.class).list(anyTypeKind);
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return SyncopeConsoleSession.get().getService(CamelRouteService.class).list(anyTypeKind).size();
        }

        @Override
        public IModel<CamelRouteTO> model(final CamelRouteTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
