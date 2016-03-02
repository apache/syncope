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

import java.lang.reflect.InvocationTargetException;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.BulkActionResult.Status;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;

public class BulkActionResultColumn<T, S> extends AbstractColumn<T, S> {

    private static final long serialVersionUID = 7955560320949560716L;

    private static final Logger LOG = LoggerFactory.getLogger(BulkActionResultColumn.class);

    private final BulkActionResult results;

    private final String keyFieldName;

    public BulkActionResultColumn(final BulkActionResult results, final String keyFieldName) {
        super(new Model<String>());
        this.results = results;
        this.keyFieldName = keyFieldName;
    }

    @Override
    public String getCssClass() {
        return "bulkResultColumn";
    }

    @Override
    public Component getHeader(final String componentId) {
        final Label label = new Label(componentId, new Model<>());
        label.setDefaultModel(new StringResourceModel("bulk.action.result.header", label, new Model<>("Result")));
        return label;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel) {
        try {
            final Object id = BeanUtils.getPropertyDescriptor(rowModel.getObject().getClass(), keyFieldName).
                    getReadMethod().invoke(rowModel.getObject(), new Object[0]);
            final Status status = results.getResults().containsKey(id.toString())
                    ? results.getResults().get(id.toString())
                    : Status.NOT_ATTEMPTED;

            item.add(new Label(componentId, new StringResourceModel(status.name(), item, new Model<>(status.name()))));

        } catch (BeansException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LOG.error("Errore retrieving target id value", e);
        }
    }
}
