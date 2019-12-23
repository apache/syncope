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

import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.ajax.markup.html.navigation.paging.AjaxDataNavigationToolbar;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.sort.AjaxFallbackOrderByBorder;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxFallbackDataTable<T extends Serializable, S> extends DataTable<T, S> {

    private static final long serialVersionUID = 6861105496141602937L;

    private ActionLinksTogglePanel<T> togglePanel;

    public AjaxFallbackDataTable(
            final String id,
            final List<? extends IColumn<T, S>> columns,
            final ISortableDataProvider<T, S> dataProvider,
            final int rowsPerPage,
            final WebMarkupContainer container) {

        super(id, columns, dataProvider, rowsPerPage);

        setOutputMarkupId(true);
        setVersioned(false);

        togglePanel = getTogglePanel();

        addTopToolbar(new AjaxFallbackHeadersToolbar<S>(this, dataProvider) {

            private static final long serialVersionUID = 7406306172424359609L;

            @Override
            protected WebMarkupContainer newSortableHeader(
                    final String borderId, final S property, final ISortStateLocator<S> locator) {
                return new AjaxFallbackOrderByBorder<S>(borderId, property, locator) {

                    private static final long serialVersionUID = -5729452929468028615L;

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

    protected ActionsPanel<T> getActions(final IModel<T> model) {
        return null;
    }

    protected ActionLinksTogglePanel<T> getTogglePanel() {
        return null;
    }

    @Override
    protected Item<T> newRowItem(final String id, final int index, final IModel<T> model) {
        final OddEvenItem<T> item = new OddEvenItem<>(id, index, model);

        if (togglePanel != null) {
            final ActionsPanel<T> actions = getActions(model);

            if (actions != null && !actions.isEmpty()) {
                item.add(new AttributeModifier("style", "cursor: pointer;"));
                item.add(new AjaxEventBehavior(Constants.ON_CLICK) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String findIndicatorId() {
                        return StringUtils.EMPTY;
                    }

                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        final String lastFocussedElementId = target.getLastFocusedElementId();
                        if (lastFocussedElementId == null) {
                            togglePanel.toggleWithContent(target, getActions(model), model.getObject());
                        } else {
                            final AjaxDataTablePanel<?, ?> parent = findParent(AjaxDataTablePanel.class);
                            final Model<Boolean> isCheck = Model.<Boolean>of(Boolean.FALSE);

                            parent.visitChildren(CheckGroupSelector.class, (selector, ivisit) -> {
                                if (selector.getMarkupId().equalsIgnoreCase(lastFocussedElementId)) {
                                    isCheck.setObject(Boolean.TRUE);
                                    ivisit.stop();
                                }
                            });

                            if (!isCheck.getObject()) {
                                parent.visitChildren(Check.class, (check, ivisit) -> {
                                    if (check.getMarkupId().equalsIgnoreCase(lastFocussedElementId)) {
                                        isCheck.setObject(Boolean.TRUE);
                                        ivisit.stop();
                                    }
                                });
                            }

                            if (!isCheck.getObject()) {
                                togglePanel.toggleWithContent(target, getActions(model), model.getObject());
                            }
                        }
                    }
                });
            }
        }

        return item;
    }
}
