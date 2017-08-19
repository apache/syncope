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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;

public class PrefixItemTransformer implements ItemTransformer {

    public static final String PREFIX = "PREFIX_";

    @Override
    public List<PlainAttrValue> beforePropagation(
            final Item item,
            final Entity entity,
            final List<PlainAttrValue> values) {

        if (values == null || values.isEmpty() || values.get(0).getStringValue() == null) {
            return values;
        } else {
            String value = values.get(0).getStringValue();
            values.get(0).setStringValue(PREFIX + value);

            return values;
        }
    }

    @Override
    public List<Object> beforePull(
            final Item item,
            final EntityTO entityTO,
            final List<Object> values) {

        if (values == null || values.isEmpty() || values.get(0) == null) {
            return values;
        } else {
            List<Object> newValues = new ArrayList<>(values);
            newValues.set(0, StringUtils.substringAfter(values.get(0).toString(), PREFIX));

            return newValues;
        }
    }

}
