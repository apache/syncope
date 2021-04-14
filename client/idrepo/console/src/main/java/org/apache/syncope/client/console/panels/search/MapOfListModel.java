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
package org.apache.syncope.client.console.panels.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.model.PropertyModel;

public class MapOfListModel<T> extends PropertyModel<List<T>> {

    private static final long serialVersionUID = -7647997536634092231L;

    private final String key;

    public MapOfListModel(final Object modelObject, final String expression, final String key) {
        super(modelObject, expression);
        this.key = key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getObject() {
        final String expression = propertyExpression();
        final Object target = getInnermostModelOrObject();

        if (target == null || StringUtils.isBlank(expression) || expression.startsWith(".")) {
            throw new IllegalArgumentException("Property expressions cannot start with a '.' character");
        }

        Map<String, List<T>> map = (Map<String, List<T>>) PropertyResolver.getValue(expression, target);

        List<T> res;
        if (map.containsKey(key)) {
            res = map.get(key);
        } else {
            res = new ArrayList<>();
            map.put(key, res);
        }
        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setObject(final List<T> object) {
        final String expression = propertyExpression();
        final Object target = getInnermostModelOrObject();
        ((Map<String, List<T>>) PropertyResolver.getValue(expression, target)).put(key, object);
    }
}
