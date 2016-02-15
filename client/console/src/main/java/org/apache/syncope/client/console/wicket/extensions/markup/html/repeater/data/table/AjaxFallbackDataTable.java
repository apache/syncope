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
package org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table;

import java.util.List;
import org.apache.syncope.client.console.wicket.ajax.markup.html.navigation.paging.AjaxDataNavigationToolbar;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;

public class AjaxFallbackDataTable<T, S> extends DataTable<T, S> {

    private static final long serialVersionUID = 6861105496141602937L;

    public AjaxFallbackDataTable(final String id, final List<? extends IColumn<T, S>> columns,
            final ISortableDataProvider<T, S> dataProvider, final int rowsPerPage, final WebMarkupContainer container) {
        super(id, columns, dataProvider, rowsPerPage);
        setOutputMarkupId(true);
        setVersioned(false);

        addTopToolbar(new AjaxFallbackHeadersToolbar<S>(this, dataProvider) {

            private static final long serialVersionUID = 7406306172424359609L;

            @Override
            protected WebMarkupContainer newSortableHeader(
                    final String borderId, final S property, final ISortStateLocator<S> locator) {
                return new AjaxFallbackOrderByBorder<S>(borderId, property, locator) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onAjaxClick(final AjaxRequestTarget target) {
                        if (container != null) {
                            target.add(container);
                        }
                    }
                };
            }

        });

        addBottomToolbar(new AjaxFallbackHeadersToolbar<S>(this, dataProvider) {

            private static final long serialVersionUID = 7406306172424359609L;

            @Override
            protected WebMarkupContainer newSortableHeader(
                    final String borderId, final S property, final ISortStateLocator<S> locator) {
                return new AjaxFallbackOrderByBorder<S>(borderId, property, locator) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onAjaxClick(final AjaxRequestTarget target) {
                        if (container != null) {
                            target.add(container);
                        }
                    }
                };
            }

        });
        addBottomToolbar(new AjaxDataNavigationToolbar(this, container));
        addBottomToolbar(new NoRecordsToolbar(this));
    }

    @Override
    protected Item<T> newRowItem(final String id, final int index, final IModel<T> model) {
        return new OddEvenItem<>(id, index, model);
    }
}
