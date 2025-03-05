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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfParam implements Serializable {

    private static final long serialVersionUID = -9162995157523535429L;

    private String schema;

    private final List<Serializable> values = new ArrayList<>();

    private boolean multivalue;

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public List<Serializable> getValues() {
        return values;
    }

    public Object valueAsObject() {
        return multivalue
                ? values
                : values.isEmpty()
                ? null
                : values.getFirst();
    }

    public void setValues(final Object value) {
        this.values.clear();
        if (value instanceof final Collection<?> objects) {
            this.values.addAll(objects.stream().
                    filter(Serializable.class::isInstance).
                    map(Serializable.class::cast).
                toList());
            this.multivalue = true;
        } else {
            this.values.add((Serializable) value);
            this.multivalue = false;
        }
    }

    public void setMultivalue(final boolean multivalue) {
        this.multivalue = multivalue;
    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public boolean isInstance(final Class<?> clazz) {
        return !values.isEmpty() && clazz.isInstance(values.getFirst());
    }

    @Override
    public String toString() {
        return "ConfParam{"
                + "schema=" + schema
                + ", values=" + values
                + ", multivalue=" + multivalue
                + '}';
    }
}
