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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

public abstract class DataTablePanel<T extends Serializable, S> extends Panel {

    private static final long serialVersionUID = -7264400471578272966L;

    protected CheckGroup<T> group;

    protected AjaxFallbackDataTable<T, S> dataTable;

    protected IModel<Collection<T>> model;

    public DataTablePanel(final String id) {
        super(id);

        model = new IModel<>() {

            private static final long serialVersionUID = 4886729136344643465L;

            private final Collection<T> values = new HashSet<>();

            @Override
            public Collection<T> getObject() {
                // Someone or something call this method to change the model: this is not the right behavior.
                // Return a copy of the model object in order to avoid SYNCOPE-465
                return new HashSet<>(values);
            }

            @Override
            public void setObject(final Collection<T> selected) {
                final Collection<T> all = getGroupModelObjects();
                values.removeAll(all);
                values.addAll(selected);
            }

            @Override
            public void detach() {
            }
        };
    }

    public final void setCurrentPage(final long page) {
        dataTable.setCurrentPage(page);
    }

    public final long getRowCount() {
        return dataTable.getRowCount();
    }

    public final long getCurrentPage() {
        return dataTable.getCurrentPage();
    }

    public final long getPageCount() {
        return dataTable.getPageCount();
    }

    public void setItemsPerPage(final int resourcePaginatorRows) {
        dataTable.setItemsPerPage(resourcePaginatorRows);
    }

    protected Collection<T> getGroupModelObjects() {
        final Set<T> res = new HashSet<>();

        final Component rows = group.get("dataTable:body:rows");
        if (rows instanceof DataGridView) {
            @SuppressWarnings("unchecked")
            final Iterator<Item<T>> iter = ((DataGridView<T>) rows).getItems();

            while (iter.hasNext()) {
                res.add(iter.next().getModelObject());
            }
        }
        return res;
    }
}
