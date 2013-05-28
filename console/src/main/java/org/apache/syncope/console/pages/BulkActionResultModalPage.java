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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.ActionResultColumn;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

/**
 * Show user or role status after performing a successful operation.
 */
public class BulkActionResultModalPage<T, S> extends BaseModalPage {

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 2646115294319713724L;

    public BulkActionResultModalPage(
            final ModalWindow window,
            final List<T> items,
            final List<IColumn<T, S>> columns,
            final BulkActionRes results,
            final String idFieldName) {

        super();

        final List<IColumn<T, S>> newColumnList = new ArrayList<IColumn<T, S>>(columns.subList(1, columns.size() - 1));
        newColumnList.add(newColumnList.size(), new ActionResultColumn<T, S>(results, idFieldName));

        final SortableDataProvider<T, S> dataProvider = new SortableDataProvider<T, S>() {

            private static final long serialVersionUID = 5291903859908641954L;

            @Override
            public Iterator<? extends T> iterator(long first, long count) {
                return items.iterator();
            }

            @Override
            public long size() {
                return items.size();
            }

            @Override
            public IModel<T> model(T object) {
                return new CompoundPropertyModel<T>(object);
            }
        };

        add(new AjaxFallbackDefaultDataTable<T, S>(
                "selectedObjects",
                newColumnList,
                dataProvider,
                Integer.MAX_VALUE).setVisible(items != null && !items.isEmpty()));

        final AjaxLink<Void> close = new IndicatingAjaxLink<Void>("close") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.close(target);
            }
        };

        add(close);

    }
}
