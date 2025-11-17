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
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.data.JEXLItemTransformer;
import org.apache.syncope.core.provisioning.api.jexl.JexlContextBuilder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.springframework.beans.factory.annotation.Autowired;

public class JEXLItemTransformerImpl implements JEXLItemTransformer {

    @Autowired
    private JexlTools jexlTools;

    @Autowired
    private DerAttrHandler derAttrHandler;

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
            final Any any,
            final AttrSchemaType schemaType,
            final PlainAttrValue value) {

        JexlContextBuilder builder = new JexlContextBuilder();
        if (any != null) {
            builder.fields(any).
                    plainAttrs(any.getPlainAttrs()).
                    derAttrs(any, derAttrHandler);
        }
        JexlContext jexlContext = builder.build();

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
            case Dropdown:
            case String:
            default:
                oValue = value.getStringValue();
        }
        jexlContext.set("value", oValue);

        Object tValue = jexlTools.evaluateExpression(propagationJEXL, jexlContext);

        value.setBinaryValue(null);
        value.setBooleanValue(null);
        value.setDateValue(null);
        value.setDoubleValue(null);
        value.setLongValue(null);
        value.setStringValue(null);

        if (tValue instanceof byte[] bs) {
            value.setBinaryValue(bs);
            return AttrSchemaType.Binary;
        }

        if (tValue instanceof Boolean aBoolean) {
            value.setBooleanValue(aBoolean);
            return AttrSchemaType.Boolean;
        }

        if (tValue instanceof OffsetDateTime offsetDateTime) {
            value.setDateValue(offsetDateTime);
            return AttrSchemaType.Date;
        }

        if (tValue instanceof Double aDouble) {
            value.setDoubleValue(aDouble);
            return AttrSchemaType.Double;
        }

        if (tValue instanceof Long aLong) {
            value.setLongValue(aLong);
            return AttrSchemaType.Long;
        }

        if (tValue != null) {
            value.setStringValue(tValue.toString());
        }
        return AttrSchemaType.String;
    }

    @Override
    public MappingManager.IntValues beforePropagation(
            final Item item,
            final Any any,
            final AttrSchemaType schemaType,
            final List<PlainAttrValue> values) {

        if (StringUtils.isBlank(propagationJEXL)) {
            return JEXLItemTransformer.super.beforePropagation(item, any, schemaType, values);
        }

        Mutable<AttrSchemaType> tType = new MutableObject<>();
        if (values.isEmpty()) {
            PlainAttrValue value = new PlainAttrValue();
            tType.setValue(beforePropagation(any, schemaType, value));
            values.add(value);
        } else {
            values.forEach(value -> tType.setValue(beforePropagation(any, schemaType, value)));
        }

        return new MappingManager.IntValues(tType.get(), values);
    }

    @Override
    public List<Object> beforePull(
            final Item item,
            final EntityTO entityTO,
            final List<Object> values) {

        if (StringUtils.isNotBlank(pullJEXL) && values != null) {
            List<Object> newValues = new ArrayList<>(values.size());
            values.forEach(value -> {
                JexlContextBuilder builder = new JexlContextBuilder().with("value", value).fields(entityTO);
                if (entityTO instanceof AnyTO anyTO) {
                    builder.attrs(anyTO.getPlainAttrs());
                    builder.attrs(anyTO.getDerAttrs());
                }
                JexlContext jexlContext = builder.build();

                newValues.add(jexlTools.evaluateExpression(pullJEXL, jexlContext));
            });

            return newValues;
        }

        return JEXLItemTransformer.super.beforePull(item, entityTO, values);
    }
}
