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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.annotations.ExtensionPanel;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.rest.CamelRouteRestClient;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

@ExtensionPanel("Camel routes")
public class CamelRoutePanel extends AbstractExtensionPanel {

    private static final long serialVersionUID = 1965360932245590233L;

    private static final int CAMELROUTE_WIN_HEIGHT = 480;

    private static final int CAMELROUTE_WIN_WIDTH = 800;

    private CamelRouteRestClient restClient = new CamelRouteRestClient();

    private ModalWindow editCamelRouteWin;

    public CamelRoutePanel(final String id, final PageReference pageref) {
        super(id, pageref);

        editCamelRouteWin = new ModalWindow("editCamelRouteWin");
        editCamelRouteWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editCamelRouteWin.setInitialHeight(CAMELROUTE_WIN_HEIGHT);
        editCamelRouteWin.setInitialWidth(CAMELROUTE_WIN_WIDTH);
        editCamelRouteWin.setCookieName("editCamelRouteWin-modal");
        add(editCamelRouteWin);

        List<IColumn<CamelRouteTO, String>> routeCols = new ArrayList<>();
        routeCols.add(new PropertyColumn<CamelRouteTO, String>(new ResourceModel("key"), "key", "key"));
        routeCols.add(new AbstractColumn<CamelRouteTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<CamelRouteTO>> cellItem, final String componentId,
                    final IModel<CamelRouteTO> model) {

                // Uncomment with something similar once SYNCOPE-156 is completed
                /* final ActionLinksPanel panel = new
                 * ActionLinksPanel(componentId, model, pageref);
                 *
                 * panel.add(new ActionLink() {
                 *
                 * private static final long serialVersionUID = -3722207913631435501L;
                 *
                 * @Override
                 * public void onClick(final AjaxRequestTarget target) {
                 *
                 * editCamelRouteWin.setPageCreator(new ModalWindow.PageCreator() {
                 *
                 * private static final long serialVersionUID = -7834632442532690940L;
                 *
                 * @Override
                 * public Page createPage() {
                 * return new CamelRouteModalPage(pageref, editCamelRouteWin,
                 * restClient.read(model.getObject().getKey()), false);
                 * }
                 *
                 * });
                 *
                 * editCamelRouteWin.show(target);
                 * }
                 * }, ActionLink.ActionType.EDIT, "CamelRoutes");
                 *
                 * cellItem.add(panel); */
            }
        });

        final AjaxFallbackDefaultDataTable<CamelRouteTO, String> routeTable =
                new AjaxFallbackDefaultDataTable<>("camelRouteTable", routeCols, new CamelRouteProvider(), 50);

        WebMarkupContainer routeContainer = new WebMarkupContainer("camelRoutesContainer");
        routeContainer.add(routeTable);
        routeContainer.setOutputMarkupId(true);
        MetaDataRoleAuthorizationStrategy.authorize(routeContainer, ENABLE, Entitlement.ROUTE_LIST);
        add(routeContainer);
    }

    private class CamelRouteProvider extends SortableDataProvider<CamelRouteTO, String> {

        private static final long serialVersionUID = -2917236020432105949L;

        private final SortableDataProviderComparator<CamelRouteTO> comparator;

        public CamelRouteProvider() {
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<? extends CamelRouteTO> iterator(final long first, final long count) {
            List<CamelRouteTO> list = new ArrayList<>();
            if (restClient.isCamelEnabledFor(AnyTypeKind.USER)) {
                list.addAll(restClient.list(AnyTypeKind.USER));
            }
            if (restClient.isCamelEnabledFor(AnyTypeKind.GROUP)) {
                list.addAll(restClient.list(AnyTypeKind.GROUP));
            }

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return (restClient.isCamelEnabledFor(AnyTypeKind.USER)
                    ? restClient.list(AnyTypeKind.USER).size()
                    : 0)
                    + (restClient.isCamelEnabledFor(AnyTypeKind.GROUP)
                            ? restClient.list(AnyTypeKind.GROUP).size()
                            : 0);
        }

        @Override
        public IModel<CamelRouteTO> model(final CamelRouteTO route) {
            return new AbstractReadOnlyModel<CamelRouteTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public CamelRouteTO getObject() {
                    return route;
                }
            };
        }
    }

}
