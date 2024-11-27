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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.util.Objects;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "key")
public abstract class AbstractEntity implements Entity {

    private static final long serialVersionUID = -9017214159540857901L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractEntity.class);

    protected void checkType(final Object object, final Class<?> clazz) {
        if (object != null && !clazz.isInstance(object)) {
            throw new ClassCastException("Expected " + clazz.getName() + ", got " + object.getClass().getName());
        }
    }

    protected void checkImplementationType(final Implementation object, final String expected) {
        if (object != null && !object.getType().equals(expected)) {
            throw new ClassCastException("Expected " + expected + ", got " + object.getType());
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof final AbstractEntity entity)) {
            return false;
        }
        return Objects.equals(getKey(), entity.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey());
    }

    @Override
    public String toString() {
        return new StringBuilder().append(getClass().getSimpleName()).
                append('[').append(getKey()).append(']').toString();
    }
}
