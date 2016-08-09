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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

public class CollectionPropertyColumn<T> extends PropertyColumn<T, String> {

    private static final long serialVersionUID = 8077865338230121496L;

    public CollectionPropertyColumn(
            final IModel<String> displayModel,
            final String sortProperty,
            final String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void populateItem(
            final Item<ICellPopulator<T>> cellItem, final String componentId, final IModel<T> rowModel) {

        final Object value = getDataModel(rowModel).getObject();

        if (value instanceof Collection) {
            final List values = new ArrayList((Collection) value);
            Collections.sort(values);
            cellItem.add(new CollectionPanel(componentId, values));
        }
    }
}
