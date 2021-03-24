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
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.WAConfigDirectoryPanel.WAConfigProvider;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class WAConfigDirectoryPanel
        extends DirectoryPanel<Attr, Attr, WAConfigProvider, WAConfigRestClient> {

    private static final long serialVersionUID = 1538796157345L;

    public WAConfigDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, new Builder<Attr, Attr, WAConfigRestClient>(new WAConfigRestClient(), pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<Attr> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        });

        itemKeyFieldName = "schema";
        disableCheckBoxes();

        this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<Attr>(new Attr(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public WizardModalPanel<Attr> build(final String id, final int index, final AjaxWizard.Mode mode) {
                return new WAConfigModalPanel(modal, newModelObject(), mode, pageRef);
            }
        }, true);

        modal.size(Modal.Size.Default);
        initResultTable();
    }

    @Override
    protected WAConfigProvider dataProvider() {
        return new WAConfigProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_WACONFIG_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<Attr, String>> getColumns() {
        final List<IColumn<Attr, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new ResourceModel("schema"), "schema"));
        columns.add(new PropertyColumn<Attr, String>(new ResourceModel("values"), "values") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<Attr>> item,
                    final String componentId,
                    final IModel<Attr> rowModel) {

                if (rowModel.getObject().getValues().toString().length() > 96) {
                    item.add(new Label(componentId, getString("tooLong")).
                            add(new AttributeModifier("style", "font-style:italic")));
                } else {
                    super.populateItem(item, componentId, rowModel);
                }
            }
        });
        return columns;
    }

    @Override
    public ActionsPanel<Attr> getActions(final IModel<Attr> model) {
        ActionsPanel<Attr> panel = super.getActions(model);

        panel.add(new ActionLink<Attr>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                target.add(modal);
                modal.header(new StringResourceModel("any.edit"));
                modal.setContent(new WAConfigModalPanel(modal, model.getObject(), AjaxWizard.Mode.EDIT, pageRef));
                modal.show(true);
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.WA_CONFIG_SET);

        panel.add(new ActionLink<Attr>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                try {
                    WAConfigRestClient.delete(model.getObject().getSchema());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.WA_CONFIG_DELETE, true);

        return panel;
    }

    protected static final class WAConfigProvider extends DirectoryDataProvider<Attr> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<Attr> comparator;

        private WAConfigProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("schema", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<Attr> iterator(final long first, final long count) {
            List<Attr> result = WAConfigRestClient.list();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return WAConfigRestClient.list().size();
        }

        @Override
        public IModel<Attr> model(final Attr object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
