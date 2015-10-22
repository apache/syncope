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

import java.util.Collections;
import java.util.Set;
import org.apache.syncope.core.persistence.api.dao.UnallowedSchemaException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Attr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;

public abstract class AbstractAttr<S extends Schema, O extends Any<?, ?>>
        extends AbstractEntity<Long> implements Attr<S, O> {

    private static final long serialVersionUID = -7722134717360731874L;

    @SuppressWarnings("unchecked")
    private Set<S> getAllowedSchemas(final O any) {
        Set<S> result = Collections.emptySet();

        if (getSchema() instanceof PlainSchema) {
            result = (Set<S>) any.getAllowedPlainSchemas();
        } else if (getSchema() instanceof DerSchema) {
            result = (Set<S>) any.getAllowedDerSchemas();
        } else if (getSchema() instanceof VirSchema) {
            result = (Set<S>) any.getAllowedVirSchemas();
        }

        return result;
    }

    protected void checkSchema(final S schema) {
        if (schema == null || getOwner() == null) {
            throw new IllegalStateException("First set owner then schema and finally add values");
        }

        if (!getAllowedSchemas(getOwner()).contains(schema)) {
            throw new UnallowedSchemaException(schema.getKey());
        }
    }
}
