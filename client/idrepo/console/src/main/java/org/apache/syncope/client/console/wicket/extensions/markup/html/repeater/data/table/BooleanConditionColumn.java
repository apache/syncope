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
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

public abstract class BooleanConditionColumn<T> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 1L;

    public BooleanConditionColumn(final IModel<String> displayModel) {
        super(displayModel);
    }

    protected abstract boolean isCondition(IModel<T> rowModel);

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel) {
        item.add(new Label(componentId, StringUtils.EMPTY));
        if (isCondition(rowModel)) {
            item.add(new AttributeModifier("class", "fa fa-check"));
            item.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
        }
    }
}
