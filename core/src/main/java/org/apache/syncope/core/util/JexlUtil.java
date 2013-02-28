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
package org.apache.syncope.core.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @see http://commons.apache.org/jexl/reference/index.html
 */
public class JexlUtil {

    /**
     * Logger.
     *
     */
    private static final Logger LOG = LoggerFactory.getLogger(JexlUtil.class);

    private static final String[] IGNORE_FIELDS = {"password", "clearPassword", "serialVersionUID"};

    @Autowired
    private JexlEngine jexlEngine;

    public boolean isExpressionValid(final String expression) {
        boolean result;
        try {
            jexlEngine.createExpression(expression);
            result = true;
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: " + expression, e);
            result = false;
        }

        return result;
    }

    public String evaluate(final String expression, final JexlContext jexlContext) {
        String result = "";

        if (StringUtils.isNotBlank(expression) && jexlContext != null) {
            try {
                Expression jexlExpression = jexlEngine.createExpression(expression);
                Object evaluated = jexlExpression.evaluate(jexlContext);
                if (evaluated != null) {
                    result = evaluated.toString();
                }
            } catch (JexlException e) {
                LOG.error("Invalid jexl expression: " + expression, e);
            }
        } else {
            LOG.debug("Expression not provided or invalid context");
        }

        return result;
    }

    public JexlContext addFieldsToContext(final Object attributable, final JexlContext jexlContext) {
        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        final Field[] fields = attributable.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                field.setAccessible(true);
                final String fieldName = field.getName();
                if ((!field.isSynthetic()) && (!fieldName.startsWith("pc"))
                        && (!ArrayUtils.contains(IGNORE_FIELDS, fieldName))
                        && (!Iterable.class.isAssignableFrom(field.getType()))
                        && (!field.getType().isArray())) {

                    final Object fieldValue = field.get(attributable);

                    context.set(fieldName, fieldValue == null
                            ? ""
                            : (field.getType().equals(Date.class)
                            ? DataFormat.format((Date) fieldValue, false)
                            : fieldValue));

                    LOG.debug("Add field {} with value {}", fieldName, fieldValue);
                }
            } catch (Exception e) {
                LOG.error("Reading class attributes error", e);
            }
        }

        return context;
    }

    public JexlContext addAttrsToContext(final Collection<? extends AbstractAttr> attributes,
            final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        for (AbstractAttr attr : attributes) {
            List<String> attributeValues = attr.getValuesAsStrings();
            String expressionValue = attributeValues.isEmpty()
                    ? ""
                    : attributeValues.get(0);

            LOG.debug("Add attribute {} with value {}", attr.getSchema().getName(), expressionValue);

            context.set(attr.getSchema().getName(), expressionValue);
        }

        return context;
    }

    public JexlContext addDerAttrsToContext(final Collection<? extends AbstractDerAttr> derAttrs,
            final Collection<? extends AbstractAttr> attrs, final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        for (AbstractDerAttr derAttr : derAttrs) {
            String expressionValue = derAttr.getValue(attrs);
            if (expressionValue == null) {
                expressionValue = "";
            }

            LOG.debug("Add derived attribute {} with value {}", derAttr.getDerivedSchema().getName(), expressionValue);

            context.set(derAttr.getDerivedSchema().getName(), expressionValue);
        }

        return context;
    }

    public JexlContext addVirAttrsToContext(final Collection<? extends AbstractVirAttr> virAttrs,
            final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        for (AbstractVirAttr virAttr : virAttrs) {
            List<String> attributeValues = virAttr.getValues();
            String expressionValue = attributeValues.isEmpty()
                    ? ""
                    : attributeValues.get(0);

            LOG.debug("Add virtual attribute {} with value {}", virAttr.getVirtualSchema().getName(), expressionValue);

            context.set(virAttr.getVirtualSchema().getName(), expressionValue);
        }

        return context;
    }

    public String evaluate(final String expression, final AbstractAttributableTO attributableTO) {
        final JexlContext context = new MapContext();

        addFieldsToContext(attributableTO, context);

        for (AttributeTO attr : attributableTO.getAttributes()) {
            List<String> values = attr.getValues();
            String expressionValue = values.isEmpty()
                    ? ""
                    : values.get(0);

            LOG.debug("Add attribute {} with value {}", attr.getSchema(), expressionValue);

            context.set(attr.getSchema(), expressionValue);
        }
        for (AttributeTO derAttr : attributableTO.getDerivedAttributes()) {
            List<String> values = derAttr.getValues();
            String expressionValue = values.isEmpty()
                    ? ""
                    : values.get(0);

            LOG.debug("Add derived attribute {} with value {}", derAttr.getSchema(), expressionValue);

            context.set(derAttr.getSchema(), expressionValue);
        }
        for (AttributeTO virAttr : attributableTO.getVirtualAttributes()) {
            List<String> values = virAttr.getValues();
            String expressionValue = values.isEmpty()
                    ? ""
                    : values.get(0);

            LOG.debug("Add virtual attribute {} with value {}", virAttr.getSchema(), expressionValue);

            context.set(virAttr.getSchema(), expressionValue);
        }

        // Evaluate expression using the context prepared before
        return evaluate(expression, context);
    }
}
