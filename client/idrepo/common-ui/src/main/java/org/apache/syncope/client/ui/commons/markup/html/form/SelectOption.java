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
package org.apache.syncope.client.ui.commons.markup.html.form;

import java.io.Serializable;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SelectOption implements Serializable {

    private static final long serialVersionUID = 2961127533930849828L;

    private String displayValue;

    private String keyValue;

    public SelectOption(final String displayValue, final String keyValue) {
        this.displayValue = displayValue;
        this.keyValue = keyValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(final String displayValue) {
        this.displayValue = displayValue;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(final String keyValue) {
        this.keyValue = keyValue;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof final SelectOption selectOption)) {
            return false;
        }

        return (keyValue == null && selectOption.keyValue == null) || keyValue != null
                && keyValue.equals(selectOption.keyValue);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return keyValue;
    }
}
