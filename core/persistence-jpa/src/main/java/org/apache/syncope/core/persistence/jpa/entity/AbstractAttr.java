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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Transient;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Attr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;

public abstract class AbstractAttr<S extends Schema, O extends Any<?, ?, ?>>
        extends AbstractEntity<Long> implements Attr<S, O> {

    private static final long serialVersionUID = -7722134717360731874L;

    @Transient
    private Set<PlainSchema> allowedPlainSchemas;

    @Transient
    private Set<DerSchema> allowedDerSchemas;

    @Transient
    private Set<VirSchema> allowedVirSchemas;

    private void populateClasses(final Collection<? extends AnyTypeClass> anyTypeClasses) {
        synchronized (this) {
            if (getSchema() instanceof PlainSchema) {
                if (allowedPlainSchemas == null) {
                    allowedPlainSchemas = new HashSet<>();
                }
                for (AnyTypeClass anyTypeClass : anyTypeClasses) {
                    allowedPlainSchemas.addAll(anyTypeClass.getPlainSchemas());
                }
            } else if (getSchema() instanceof DerSchema) {
                if (allowedDerSchemas == null) {
                    allowedDerSchemas = new HashSet<>();
                }
                for (AnyTypeClass anyTypeClass : anyTypeClasses) {
                    allowedDerSchemas.addAll(anyTypeClass.getDerSchemas());
                }
            } else if (getSchema() instanceof VirSchema) {
                if (allowedVirSchemas == null) {
                    allowedVirSchemas = new HashSet<>();
                }
                for (AnyTypeClass anyTypeClass : anyTypeClasses) {
                    allowedVirSchemas.addAll(anyTypeClass.getVirSchemas());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<S> getAllowedSchemas() {
        Set<S> result = Collections.emptySet();

        if (getSchema() instanceof PlainSchema) {
            if (allowedPlainSchemas == null) {
                allowedPlainSchemas = new HashSet<>();
            }
            result = (Set<S>) allowedPlainSchemas;
        } else if (getSchema() instanceof DerSchema) {
            if (allowedDerSchemas == null) {
                allowedDerSchemas = new HashSet<>();
            }
            result = (Set<S>) allowedDerSchemas;
        } else if (getSchema() instanceof VirSchema) {
            if (allowedVirSchemas == null) {
                allowedVirSchemas = new HashSet<>();
            }
            result = (Set<S>) allowedVirSchemas;
        }

        return result;
    }

    protected void checkSchema(final S schema) {
        if (schema == null || getOwner() == null) {
            throw new IllegalStateException("First set owner then schema and finally add values");
        }

        populateClasses(getOwner().getType().getClasses());
        populateClasses(getOwner().getAuxClasses());
        if (getOwner() instanceof User) {
            for (UMembership memb : ((User) getOwner()).getMemberships()) {
                for (TypeExtension typeExtension : memb.getRightEnd().getTypeExtensions()) {
                    populateClasses(typeExtension.getAuxClasses());
                }
            }
        }
        if (getOwner() instanceof AnyObject) {
            for (AMembership memb : ((AnyObject) getOwner()).getMemberships()) {
                for (TypeExtension typeExtension : memb.getRightEnd().getTypeExtensions()) {
                    populateClasses(typeExtension.getAuxClasses());
                }
            }
        }

        if (!getAllowedSchemas().contains(schema)) {
            throw new IllegalArgumentException(schema + " not allowed for this instance");
        }
    }
}
