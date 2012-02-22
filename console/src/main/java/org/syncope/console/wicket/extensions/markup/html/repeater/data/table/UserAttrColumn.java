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

import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.syncope.client.to.UserTO;

public class UserAttrColumn extends AbstractColumn<UserTO> {

    private static final long serialVersionUID = 2624734332447371372L;

    public enum SchemaType {

        schema,
        virtualSchema,
        derivedSchema;

    };

    private final String name;

    private final SchemaType schemaType;

    public UserAttrColumn(
            final String name,
            final SchemaType schemaType) {

        super(new ResourceModel(name, name), name);
        this.name = name;
        this.schemaType = schemaType;
    }

    @Override
    public void populateItem(
            final Item<ICellPopulator<UserTO>> cellItem,
            final String componentId,
            final IModel<UserTO> rowModel) {

        List<String> values = null;

        switch (schemaType) {
            case schema:
                if (rowModel.getObject().getAttributeMap().containsKey(name)) {
                    values = rowModel.getObject().getAttributeMap().
                            get(name).getValues();
                }
                break;

            case virtualSchema:
                if (rowModel.getObject().getVirtualAttributeMap().
                        containsKey(name)) {

                    values = rowModel.getObject().getVirtualAttributeMap().
                            get(name).getValues();
                }
                break;

            case derivedSchema:
                if (rowModel.getObject().getDerivedAttributeMap().
                        containsKey(name)) {

                    values = rowModel.getObject().getDerivedAttributeMap().
                            get(name).getValues();
                }
                break;

            default:
        }

        if (values == null || values.isEmpty()) {
            cellItem.add(new Label(componentId, ""));
        } else {
            if (values.size() == 1) {
                cellItem.add(
                        new Label(componentId, values.get(0)));
            } else {
                cellItem.add(
                        new Label(componentId, values.toString()));
            }
        }
    }
}
