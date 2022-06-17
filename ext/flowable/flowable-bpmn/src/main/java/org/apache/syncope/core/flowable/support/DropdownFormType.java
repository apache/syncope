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
package org.apache.syncope.core.flowable.support;

import java.util.Optional;
import org.flowable.engine.form.AbstractFormType;

/**
 * Extension to predefined Flowable form types relying on the provided
 * {@link org.apache.syncope.core.flowable.api.DropdownValueProvider} bean to populate values.
 */
public class DropdownFormType extends AbstractFormType {

    private static final long serialVersionUID = -3549337216346168946L;

    protected final String dropdownValueProvider;

    public DropdownFormType(final String dropdownValueProvider) {
        this.dropdownValueProvider = dropdownValueProvider;
    }

    @Override
    public String getName() {
        return "dropdown";
    }

    @Override
    public Object getInformation(final String key) {
        if (key.equals("dropdownValueProvider")) {
            return dropdownValueProvider;
        }
        return null;
    }

    @Override
    public Object convertFormValueToModelValue(final String propertyValue) {
        return propertyValue;
    }

    @Override
    public String convertModelValueToFormValue(final Object modelValue) {
        return Optional.ofNullable(modelValue).map(Object::toString).orElse(null);
    }
}
