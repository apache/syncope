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
package org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckGroupColumn<T, S> extends AbstractColumn<T, S> {

    private static final long serialVersionUID = 7955560320949560715L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(CheckGroupColumn.class);

    private CheckGroup<T> group;

    public CheckGroupColumn(CheckGroup<T> checkGroup) {
        super(new Model());
        this.group = checkGroup;
    }

    @Override
    public String getCssClass() {
        return "checkGroupColumn";
    }

    @Override
    public Component getHeader(String componentId) {
        return new CheckBoxGroupSelectorPanel(componentId, group);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel) {
        item.add(new CheckBoxPanel<T>(componentId, rowModel, group));
    }
    
}
