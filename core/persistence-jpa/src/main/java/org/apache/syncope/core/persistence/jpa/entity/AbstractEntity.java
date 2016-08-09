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
package org.apache.syncope.core.persistence.jpa.entity;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public abstract class AbstractEntity implements Entity {

    private static final long serialVersionUID = -9017214159540857901L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractEntity.class);

    protected void checkType(final Object object, final Class<?> clazz) {
        if (object != null && !clazz.isInstance(object)) {
            throw new ClassCastException("Expected " + clazz.getName() + ", got " + object.getClass().getName());
        }
    }

    /**
     * @param property the integer representing a boolean value
     * @return the boolean value corresponding to the property param
     */
    public final boolean isBooleanAsInteger(final Integer property) {
        return property != null && property == 1;
    }

    /**
     * @param value the boolean value to be represented as integer
     * @return the integer corresponding to the property param
     */
    public final Integer getBooleanAsInteger(final Boolean value) {
        return Boolean.TRUE.equals(value)
                ? 1
                : 0;
    }

    /**
     * @return fields to be excluded when computing equals() or hashcode()
     */
    private String[] getExcludeFields() {
        Set<String> excludeFields = new HashSet<>();

        for (PropertyDescriptor propDesc : BeanUtils.getPropertyDescriptors(getClass())) {
            if (propDesc.getPropertyType().isInstance(Collections.emptySet())
                    || propDesc.getPropertyType().isInstance(Collections.emptyList())) {

                excludeFields.add(propDesc.getName());
            }
        }

        return excludeFields.toArray(new String[] {});
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, getExcludeFields());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, getExcludeFields());
    }

    @Override
    public String toString() {
        return new StringBuilder().append(getClass().getSimpleName()).
                append('[').append(getKey()).append(']').toString();
    }
}
