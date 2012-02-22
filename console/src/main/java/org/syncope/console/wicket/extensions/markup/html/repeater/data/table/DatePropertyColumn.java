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
package org.syncope.console.wicket.extensions.markup.html.repeater.data.table;

import java.util.Date;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.syncope.console.SyncopeSession;

/**
 * Format column's value as date string.
 */
public class DatePropertyColumn<T> extends PropertyColumn<T> {

    private static final long serialVersionUID = 3527840552172947705L;

    public DatePropertyColumn(final IModel<String> displayModel,
            final String sortProperty, final String propertyExpression) {

        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item,
            final String componentId, final IModel<T> rowModel) {

        IModel date = (IModel<Date>) createLabelModel(rowModel);

        String convertedDate = "";

        if (date.getObject() != null) {
            convertedDate = SyncopeSession.get().
                    getDateFormat().format(date.getObject());
            item.add(new Label(componentId, convertedDate));
        } else {
            item.add(new Label(componentId, convertedDate));
        }
    }
}