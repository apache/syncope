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
package org.apache.syncope.core.provisioning.java.data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.data.JEXLItemTransformer;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class JEXLItemTransformerImpl implements JEXLItemTransformer {

    @Autowired
    private DerAttrHandler derAttrHandler;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    private String propagationJEXL;

    private String pullJEXL;

    @Override
    public void setPropagationJEXL(final String propagationJEXL) {
        this.propagationJEXL = propagationJEXL;
    }

    @Override
    public void setPullJEXL(final String pullJEXL) {
        this.pullJEXL = pullJEXL;
    }

    protected AttrSchemaType beforePropagation(
            final Any<?> any,
            final AttrSchemaType schemaType,
            final PlainAttrValue value) {

        JexlContext jexlContext = new MapContext();
        if (any != null) {
            JexlUtils.addFieldsToContext(any, jexlContext);
            JexlUtils.addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
            JexlUtils.addDerAttrsToContext(any, derAttrHandler, jexlContext);
        }

        Object oValue;
        switch (schemaType) {
            case Binary:
            case Encrypted:
                oValue = value.getBinaryValue();
                break;

            case Boolean:
                oValue = value.getBooleanValue();
                break;

            case Date:
                oValue = value.getDateValue();
                break;

            case Double:
                oValue = value.getDoubleValue();
                break;

            case Long:
                oValue = value.getLongValue();
                break;

            case Enum:
            case String:
            default:
                oValue = value.getStringValue();
        }
        jexlContext.set("value", oValue);

        Object tValue = JexlUtils.evaluate(propagationJEXL, jexlContext);

        value.setBinaryValue(null);
        value.setBooleanValue(null);
        value.setDateValue(null);
        value.setDoubleValue(null);
        value.setLongValue(null);
        value.setStringValue(null);

        if (tValue instanceof byte[]) {
            value.setBinaryValue((byte[]) tValue);
            return AttrSchemaType.Binary;
        }

        if (tValue instanceof Boolean) {
            value.setBooleanValue((Boolean) tValue);
            return AttrSchemaType.Boolean;
        }

        if (tValue instanceof OffsetDateTime) {
            value.setDateValue((OffsetDateTime) tValue);
            return AttrSchemaType.Date;
        }

        if (tValue instanceof Double) {
            value.setDoubleValue((Double) tValue);
            return AttrSchemaType.Double;
        }

        if (tValue instanceof Long) {
            value.setLongValue((Long) tValue);
            return AttrSchemaType.Long;
        }

        if (tValue != null) {
            value.setStringValue(tValue.toString());
        }
        return AttrSchemaType.String;
    }

    @Override
    public Pair<AttrSchemaType, List<PlainAttrValue>> beforePropagation(
            final Item item,
            final Any<?> any,
            final AttrSchemaType schemaType,
            final List<PlainAttrValue> values) {

        if (StringUtils.isBlank(propagationJEXL)) {
            return JEXLItemTransformer.super.beforePropagation(item, any, schemaType, values);
        }

        AtomicReference<AttrSchemaType> tType = new AtomicReference<>();
        if (values.isEmpty()) {
            PlainAttrValue value = anyUtilsFactory.getInstance(any).newPlainAttrValue();
            tType.set(beforePropagation(any, schemaType, value));
            values.add(value);
        } else {
            values.forEach(value -> tType.set(beforePropagation(any, schemaType, value)));
        }

        return Pair.of(tType.get(), values);
    }

    @Override
    public List<Object> beforePull(
            final Item item,
            final EntityTO entityTO,
            final List<Object> values) {

        if (StringUtils.isNotBlank(pullJEXL) && values != null) {
            List<Object> newValues = new ArrayList<>(values.size());
            values.forEach(value -> {
                JexlContext jexlContext = new MapContext();
                jexlContext.set("value", value);
                JexlUtils.addFieldsToContext(entityTO, jexlContext);
                if (entityTO instanceof AnyTO) {
                    JexlUtils.addAttrsToContext(((AnyTO) entityTO).getPlainAttrs(), jexlContext);
                    JexlUtils.addAttrsToContext(((AnyTO) entityTO).getDerAttrs(), jexlContext);
                    JexlUtils.addAttrsToContext(((AnyTO) entityTO).getVirAttrs(), jexlContext);
                }

                newValues.add(JexlUtils.evaluate(pullJEXL, jexlContext));
            });

            return newValues;
        }

        return JEXLItemTransformer.super.beforePull(item, entityTO, values);
    }
}
