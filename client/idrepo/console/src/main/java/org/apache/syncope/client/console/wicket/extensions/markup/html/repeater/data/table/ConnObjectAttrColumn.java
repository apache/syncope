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

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.syncope.common.lib.to.ConnObjectTO;

public class ConnObjectAttrColumn extends AbstractColumn<ConnObjectTO, String> {

    private static final long serialVersionUID = 2624734332447371372L;

    private final String name;

    public ConnObjectAttrColumn(final String name, final SchemaType schemaType) {
        // set sortProperty to schematype#name (e.g. derivedSchema#cn, 
        // for use with SortableUserProviderComparator.AttrModel#getObject)
        super(new ResourceModel(name, name), schemaType.name() + '#' + name);
        this.name = name;
    }

    @Override
    public void populateItem(
            final Item<ICellPopulator<ConnObjectTO>> cellItem,
            final String componentId,
            final IModel<ConnObjectTO> rowModel) {

        Optional<Attr> attr = rowModel.getObject().getAttr(name);
        List<String> values = attr.map(Attr::getValues).orElse(null);

        if (values == null || values.isEmpty()) {
            cellItem.add(new Label(componentId, ""));
        } else if (values.size() == 1) {
            cellItem.add(new Label(componentId, values.get(0)));
        } else {
            cellItem.add(new Label(componentId, values.toString()));
        }
    }
}
