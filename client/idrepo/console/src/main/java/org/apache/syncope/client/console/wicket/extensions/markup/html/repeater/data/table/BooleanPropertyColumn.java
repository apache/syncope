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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Format column's value as boolean.
 *
 * @param <T> The Model object type
 */
public class BooleanPropertyColumn<T> extends PropertyColumn<T, String> {

    private static final long serialVersionUID = 3527840552172947705L;

    public BooleanPropertyColumn(
            final IModel<String> displayModel,
            final String sortProperty,
            final String propertyExpression) {

        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel) {
        BeanWrapper bwi = new BeanWrapperImpl(rowModel.getObject());
        Object obj = bwi.getPropertyValue(getPropertyExpression());

        item.add(new Label(componentId, StringUtils.EMPTY));
        if (obj != null && Boolean.valueOf(obj.toString())) {
            item.add(new AttributeModifier("class", "fa fa-check"));
            item.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
        }
    }
}
