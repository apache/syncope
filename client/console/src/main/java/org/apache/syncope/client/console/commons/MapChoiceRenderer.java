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
package org.apache.syncope.client.console.commons;

import java.util.List;
import java.util.Map;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

public class MapChoiceRenderer<T, K> implements IChoiceRenderer<T> {

    private static final long serialVersionUID = -7452881117778186644L;

    private final Map<T, K> map;

    public MapChoiceRenderer(final Map<T, K> map) {
        this.map = map;
    }

    @Override
    public Object getDisplayValue(final T key) {
        return map.get(key);
    }

    @Override
    public String getIdValue(final T key, final int index) {
        return key.toString();
    }

    @Override
    public T getObject(final String id, final IModel<? extends List<? extends T>> choices) {
        for (Map.Entry<T, K> entry : map.entrySet()) {
            if (entry.getValue() != null && entry.getValue().toString().equals(id)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
