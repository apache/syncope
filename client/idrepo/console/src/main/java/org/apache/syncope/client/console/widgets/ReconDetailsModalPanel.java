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
package org.apache.syncope.client.console.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.widgets.reconciliation.Any;
import org.apache.syncope.client.console.widgets.reconciliation.Misaligned;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class ReconDetailsModalPanel extends AbstractModalPanel<Any> {

    private static final long serialVersionUID = 1469396040405535283L;

    private static final int ROWS = 10;

    private final String resource;

    private final List<Misaligned> misaligned;

    public ReconDetailsModalPanel(
            final BaseModal<Any> modal,
            final String resource,
            final List<Misaligned> misaligned,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.resource = resource;
        this.misaligned = misaligned;

        add(new DiffPanel("diff", pageRef));
    }

    private class DiffPanel extends DirectoryPanel<
        Misaligned, Misaligned, DetailsProvider, BaseRestClient> {

        private static final long serialVersionUID = -8214546246301342868L;

        DiffPanel(final String id, final PageReference pageRef) {
            super(id, new Builder<Misaligned, Misaligned, BaseRestClient>(null, pageRef) {

                private static final long serialVersionUID = 8769126634538601689L;

                @Override
                protected WizardMgtPanel<Misaligned> newInstance(final String id, final boolean wizardInModal) {
                    throw new UnsupportedOperationException();
                }
            }.disableCheckBoxes().hidePaginator());

            rows = 10;
            initResultTable();
        }

        @Override
        protected DetailsProvider dataProvider() {
            return new DetailsProvider();
        }

        @Override
        protected String paginatorRowsKey() {
            return StringUtils.EMPTY;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return List.of();
        }

        @Override
        protected List<IColumn<Misaligned, String>> getColumns() {
            List<IColumn<Misaligned, String>> columns = new ArrayList<>();

            columns.add(new PropertyColumn<>(new ResourceModel(Constants.KEY_FIELD_NAME),
                    Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));

            columns.add(new AbstractColumn<>(Model.of(Constants.SYNCOPE)) {

                private static final long serialVersionUID = 2054811145491901166L;

                @Override
                public void populateItem(
                    final Item<ICellPopulator<Misaligned>> cellItem,
                    final String componentId,
                    final IModel<Misaligned> rowModel) {

                    cellItem.add(new Label(componentId, rowModel.getObject().getOnSyncope().toString()));
                    cellItem.add(new AttributeModifier("class", "code-deletion"));
                }
            });

            columns.add(new AbstractColumn<>(Model.of(resource)) {

                private static final long serialVersionUID = 2054811145491901166L;

                @Override
                public void populateItem(
                    final Item<ICellPopulator<Misaligned>> cellItem,
                    final String componentId,
                    final IModel<Misaligned> rowModel) {

                    cellItem.add(new Label(componentId, rowModel.getObject().getOnResource().toString()));
                    cellItem.add(new AttributeModifier("class", "code-addition"));
                }
            });

            return columns;
        }
    }

    protected final class DetailsProvider extends DirectoryDataProvider<Misaligned> {

        private static final long serialVersionUID = -1500081449932597854L;

        private final SortableDataProviderComparator<Misaligned> comparator;

        private DetailsProvider() {
            super(ROWS);
            setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<Misaligned> iterator(final long first, final long count) {
            misaligned.sort(comparator);
            return misaligned.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return misaligned.size();
        }

        @Override
        public IModel<Misaligned> model(final Misaligned object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
