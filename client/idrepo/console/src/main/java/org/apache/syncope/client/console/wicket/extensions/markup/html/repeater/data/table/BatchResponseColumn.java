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
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.ExecStatus;
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

public class BatchResponseColumn<T, S> extends AbstractColumn<T, S> {

    private static final long serialVersionUID = 7955560320949560716L;

    private static final Logger LOG = LoggerFactory.getLogger(BatchResponseColumn.class);

    private final Map<String, String> results;

    private final String keyFieldName;

    public BatchResponseColumn(final Map<String, String> results, final String keyFieldName) {
        super(new Model<>());
        this.results = results;
        this.keyFieldName = keyFieldName;
    }

    @Override
    public Component getHeader(final String componentId) {
        Label label = new Label(componentId, new Model<>());
        label.setDefaultModel(new StringResourceModel("batch.result.header", label, new Model<>("Result")));
        return label;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> item, final String componentId, final IModel<T> rowModel) {
        try {
            Object key = Objects.requireNonNull(
                BeanUtils.getPropertyDescriptor(rowModel.getObject().getClass(), keyFieldName))
                .getReadMethod().invoke(rowModel.getObject(), ArrayUtils.EMPTY_OBJECT_ARRAY);
            String status = results.containsKey(key.toString())
                    ? results.get(key.toString())
                    : ExecStatus.NOT_ATTEMPTED.name();

            item.add(new Label(componentId, new StringResourceModel(status, item, new Model<>(status))));
        } catch (BeansException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LOG.error("Error retrieving target key value", e);
        }
    }
}
