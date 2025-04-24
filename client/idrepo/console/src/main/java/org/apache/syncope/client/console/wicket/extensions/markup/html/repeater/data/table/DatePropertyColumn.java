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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * Format column's value as date string.
 */
public class DatePropertyColumn<T> extends PropertyColumn<T, String> {

    private static final long serialVersionUID = 3527840552172947705L;

    public DatePropertyColumn(
            final IModel<String> displayModel,
            final String sortProperty,
            final String propertyExpression) {

        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel) {
        IModel<?> date = getDataModel(rowModel);

        String convertedDate = "";
        if (date.getObject() instanceof final OffsetDateTime offsetDateTime) {
            convertedDate = SyncopeConsoleSession.get().getDateFormat().format(offsetDateTime);
        } else if (date.getObject() instanceof final ZonedDateTime zonedDateTime) {
            convertedDate = SyncopeConsoleSession.get().getDateFormat().format(zonedDateTime);
        } else if (date.getObject() instanceof final LocalDateTime localDateTime) {
            convertedDate = SyncopeConsoleSession.get().getDateFormat().format(localDateTime);
        } else if (date.getObject() instanceof final Date date1) {
            convertedDate = SyncopeConsoleSession.get().getDateFormat().format(date1);
        }
        item.add(new Label(componentId, convertedDate));
    }
}
