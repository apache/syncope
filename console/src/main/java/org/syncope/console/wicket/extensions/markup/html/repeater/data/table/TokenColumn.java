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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.StringUtils;
import org.syncope.client.to.UserTO;

public class TokenColumn extends AbstractColumn<UserTO> {

    private static final long serialVersionUID = 8077865338230121496L;

    public TokenColumn(final String name) {
        super(new ResourceModel(name, name), name);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<UserTO>> cellItem,
            final String componentId,
            final IModel<UserTO> rowModel) {

        if (StringUtils.hasText(rowModel.getObject().getToken())) {
            cellItem.add(new Label(
                    componentId,
                    new ResourceModel("tokenValued", "tokenValued")));
        } else {
            cellItem.add(new Label(
                    componentId,
                    new ResourceModel("tokenNotValued", "tokenNotValued")));
        }
    }
}