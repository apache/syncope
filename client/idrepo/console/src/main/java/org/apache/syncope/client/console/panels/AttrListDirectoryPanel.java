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
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AttrListDirectoryPanel.AttrListProvider;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.Attr;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public abstract class AttrListDirectoryPanel
        extends DirectoryPanel<Attr, Attr, AttrListProvider, BaseRestClient> {

    private static final long serialVersionUID = -9098924321080135095L;

    protected AttrListDirectoryPanel(
            final String id,
            final BaseRestClient restClient,
            final PageReference pageRef,
            final boolean wizardInModal) {

        super(id, restClient, pageRef, wizardInModal);

        itemKeyFieldName = "schema";
        disableCheckBoxes();

        modal.size(Modal.Size.Default);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<Attr, String>> getColumns() {
        final List<IColumn<Attr, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new ResourceModel("schema"), "schema"));
        columns.add(new PropertyColumn<>(new ResourceModel("values"), "values") {

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

    protected abstract static class AttrListProvider extends DirectoryDataProvider<Attr> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<Attr> comparator;

        protected AttrListProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("schema", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        protected abstract List<Attr> list();

        @Override
        public Iterator<Attr> iterator(final long first, final long count) {
            List<Attr> result = list();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return list().size();
        }

        @Override
        public IModel<Attr> model(final Attr object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
