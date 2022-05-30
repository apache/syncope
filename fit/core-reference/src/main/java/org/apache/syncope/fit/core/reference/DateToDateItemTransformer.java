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
package org.apache.syncope.fit.core.reference;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;

public class DateToDateItemTransformer implements ItemTransformer {

    @Override
    public Pair<AttrSchemaType, List<PlainAttrValue>> beforePropagation(
            final Item item,
            final Entity entity,
            final AttrSchemaType schemaType,
            final List<PlainAttrValue> values) {

        if (values == null || values.isEmpty() || values.get(0).getDateValue() == null) {
            return ItemTransformer.super.beforePropagation(item, entity, schemaType, values);
        } else {
            values.get(0).setDateValue(values.get(0).getDateValue().plusDays(1));
            return Pair.of(schemaType, values);
        }
    }
}
