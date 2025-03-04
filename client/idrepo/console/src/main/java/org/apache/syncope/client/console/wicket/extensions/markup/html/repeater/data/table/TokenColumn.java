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
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class TokenColumn<T extends AnyTO> extends AbstractColumn<T, String> {

    private static final long serialVersionUID = 8077865338230121496L;

    public TokenColumn(final IModel<String> displayModel, final String sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public void populateItem(
            final Item<ICellPopulator<T>> cellItem,
            final String componentId,
            final IModel<T> rowModel) {

        if (rowModel.getObject() instanceof final UserTO userTO) {
            if (StringUtils.isNotBlank(userTO.getToken())) {
                cellItem.add(new Label(componentId, new ResourceModel("tokenValued", "tokenValued")));
            } else {
                cellItem.add(new Label(componentId, new ResourceModel("tokenNotValued", "tokenNotValued")));
            }
        }
    }
}
