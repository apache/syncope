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
import java.util.List;
import org.apache.syncope.common.lib.Attributable;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class AttrColumn<T extends Attributable> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 2624734332447371372L;

    private final String name;

    private final SchemaType schemaType;

    public AttrColumn(final String name, final String label, final SchemaType schemaType) {
        // set sortProperty to schematype#name (e.g. derivedSchema#cn, 
        // for use with SortableUserProviderComparator.AttrModel#getObject)
        super(new ResourceModel(name, label), schemaType.name() + '#' + name);
        this.name = name;
        this.schemaType = schemaType;
    }

    @Override
    public void populateItem(
            final Item<ICellPopulator<T>> cellItem, final String componentId, final IModel<T> rowModel) {

        List<String> values = new ArrayList<>();

        switch (schemaType) {
            case PLAIN:
                rowModel.getObject().getPlainAttr(name).ifPresent(attr -> values.addAll(attr.getValues()));
                break;

            case DERIVED:
                rowModel.getObject().getDerAttr(name).ifPresent(attr -> values.addAll(attr.getValues()));
                break;

            case VIRTUAL:
                rowModel.getObject().getVirAttr(name).ifPresent(attr -> values.addAll(attr.getValues()));
                break;

            default:
        }

        if (values.isEmpty()) {
            cellItem.add(new Label(componentId, ""));
        } else if (values.size() == 1) {
            cellItem.add(new Label(componentId, values.getFirst()));
        } else {
            cellItem.add(new Label(componentId, values.toString()));
        }
    }
}
