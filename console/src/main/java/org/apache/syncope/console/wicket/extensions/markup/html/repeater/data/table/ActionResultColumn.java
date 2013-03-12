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

import java.beans.PropertyDescriptor;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public class ActionResultColumn<T, S> extends AbstractColumn<T, S> {

    private static final long serialVersionUID = 7955560320949560716L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ActionResultColumn.class);

    private final BulkActionRes results;

    private final String idFieldName;

    public ActionResultColumn(final BulkActionRes results, final String idFieldName) {
        super(new Model());
        this.results = results;
        this.idFieldName = idFieldName;
    }

    @Override
    public String getCssClass() {
        return "bulkResultColumn";
    }

    @Override
    public Component getHeader(final String componentId) {
        return new Label(componentId, new ResourceModel("bulkActionResultLabel", "Result"));
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel) {
        try {
            final PropertyDescriptor propDesc =
                    BeanUtils.getPropertyDescriptor(rowModel.getObject().getClass(), idFieldName);
            final Object id = propDesc.getReadMethod().invoke(rowModel.getObject(), new Object[0]);
            item.add(new Label(componentId, results.getResultMap().get(id.toString()).toString()));
        } catch (Exception e) {
            LOG.error("Errore retrieving target id value", e);
        }
    }
}
