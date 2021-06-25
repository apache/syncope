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
package org.apache.syncope.client.enduser.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomAttribute implements Serializable {

    private static final long serialVersionUID = 4910266842123376686L;

    private boolean readonly;

    private List<String> defaultValues = new ArrayList<>();

    public CustomAttribute() {
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    public List<String> getDefaultValues() {
        return defaultValues;
    }

    public void setDefaultValues(final List<String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    public CustomAttribute readonly(final Boolean value) {
        this.readonly = value;
        return this;
    }

    public CustomAttribute defaultValues(final List<String> value) {
        this.defaultValues = value;
        return this;
    }

}
