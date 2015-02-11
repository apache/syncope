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

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;

public abstract class AbstractAttributable<P extends PlainAttr, D extends DerAttr, V extends VirAttr>
        extends AbstractAnnotatedEntity<Long> implements Attributable<P, D, V> {

    private static final long serialVersionUID = -4801685541488201119L;

    @Override
    public P getPlainAttr(final String plainSchemaName) {
        P result = null;
        for (P plainAttr : getPlainAttrs()) {
            if (plainAttr != null && plainAttr.getSchema() != null
                    && plainSchemaName.equals(plainAttr.getSchema().getKey())) {

                result = plainAttr;
            }
        }
        return result;
    }

    @Override
    public D getDerAttr(final String derSchemaName) {
        D result = null;
        for (D derAttr : getDerAttrs()) {
            if (derAttr != null && derAttr.getSchema() != null
                    && derSchemaName.equals(derAttr.getSchema().getKey())) {

                result = derAttr;
            }
        }

        return result;
    }

    @Override
    public V getVirAttr(final String virSchemaName) {
        V result = null;
        for (V virAttr : getVirAttrs()) {
            if (virAttr != null && virAttr.getSchema() != null
                    && virSchemaName.equals(virAttr.getSchema().getKey())) {

                result = virAttr;
            }
        }

        return result;
    }

    protected Map<PlainSchema, P> getPlainAttrMap() {
        final Map<PlainSchema, P> map = new HashMap<>();

        for (P attr : getPlainAttrs()) {
            map.put(attr.getSchema(), attr);
        }

        return map;
    }

    protected Map<DerSchema, D> getDerAttrMap() {
        final Map<DerSchema, D> map = new HashMap<>();

        for (D attr : getDerAttrs()) {
            map.put(attr.getSchema(), attr);
        }

        return map;
    }

    protected Map<VirSchema, V> getVirAttrMap() {
        final Map<VirSchema, V> map = new HashMap<>();

        for (V attr : getVirAttrs()) {
            map.put(attr.getSchema(), attr);
        }

        return map;
    }
}
