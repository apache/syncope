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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.api.data.JEXLItemTransformer;
import org.springframework.beans.factory.annotation.Autowired;

public class JEXLItemTransformerImpl implements JEXLItemTransformer {

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

    @Override
    public Pair<AttrSchemaType, List<PlainAttrValue>> beforePropagation(
            final Item item,
            final Entity entity,
            final AttrSchemaType schemaType,
            final List<PlainAttrValue> values) {

        if (StringUtils.isNotBlank(propagationJEXL) && values != null) {
            values.forEach(value -> {
                Object originalValue = value.getValue();
                if (originalValue != null) {
                    JexlContext jexlContext = new MapContext();
                    if (entity != null) {
                        JexlUtils.addFieldsToContext(entity, jexlContext);
                        if (entity instanceof Any) {
                            JexlUtils.addPlainAttrsToContext(((Any<?>) entity).getPlainAttrs(), jexlContext);
                            JexlUtils.addDerAttrsToContext(((Any<?>) entity), derAttrHandler, jexlContext);
                        }
                    }
                    jexlContext.set("value", originalValue);

                    value.setBinaryValue(null);
                    value.setBooleanValue(null);
                    value.setDateValue(null);
                    value.setDoubleValue(null);
                    value.setLongValue(null);
                    value.setStringValue(JexlUtils.evaluate(propagationJEXL, jexlContext));
                }
            });

            return Pair.of(AttrSchemaType.String, values);
        }

        return JEXLItemTransformer.super.beforePropagation(item, entity, schemaType, values);
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
                if (entityTO instanceof AnyTO) {
                    JexlUtils.addFieldsToContext((AnyTO) entityTO, jexlContext);
                    JexlUtils.addAttrsToContext(((AnyTO) entityTO).getPlainAttrs(), jexlContext);
                    JexlUtils.addAttrsToContext(((AnyTO) entityTO).getDerAttrs(), jexlContext);
                    JexlUtils.addAttrsToContext(((AnyTO) entityTO).getVirAttrs(), jexlContext);
                } else if (entityTO instanceof RealmTO) {
                    JexlUtils.addFieldsToContext((RealmTO) entityTO, jexlContext);
                }

                newValues.add(JexlUtils.evaluate(pullJEXL, jexlContext));
            });

            return newValues;
        }

        return JEXLItemTransformer.super.beforePull(item, entityTO, values);
    }
}
