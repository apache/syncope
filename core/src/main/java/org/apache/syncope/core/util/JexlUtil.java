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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see http://commons.apache.org/jexl/reference/index.html
 */
public final class JexlUtil {

    /**
     * Logger.
     *
     */
    private static final Logger LOG = LoggerFactory.getLogger(JexlUtil.class);

    private static final String[] IGNORE_FIELDS = { "password", "clearPassword", "serialVersionUID", "class" };

    private static JexlEngine jexlEngine;

    private static JexlEngine getEngine() {
        synchronized (LOG) {
            if (jexlEngine == null) {
                jexlEngine = ApplicationContextProvider.getApplicationContext().getBean(JexlEngine.class);
            }
        }

        return jexlEngine;
    }

    public static boolean isExpressionValid(final String expression) {
        boolean result;
        try {
            getEngine().createExpression(expression);
            result = true;
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: " + expression, e);
            result = false;
        }

        return result;
    }

    public static String evaluate(final String expression, final JexlContext jexlContext) {
        String result = "";

        if (StringUtils.isNotBlank(expression) && jexlContext != null) {
            try {
                Expression jexlExpression = getEngine().createExpression(expression);
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

    public static JexlContext addFieldsToContext(final Object attributable, final JexlContext jexlContext) {
        JexlContext context = jexlContext == null ? new MapContext() : jexlContext;

        try {
            for (PropertyDescriptor desc : Introspector.getBeanInfo(attributable.getClass()).getPropertyDescriptors()) {
                final Class<?> type = desc.getPropertyType();
                final String fieldName = desc.getName();

                if ((!fieldName.startsWith("pc"))
                        && (!ArrayUtils.contains(IGNORE_FIELDS, fieldName))
                        && (!Iterable.class.isAssignableFrom(type))
                        && (!type.isArray())) {
                    try {
                        final Method getter = desc.getReadMethod();

                        final Object fieldValue;

                        if (getter == null) {
                            final Field field = attributable.getClass().getDeclaredField(fieldName);
                            field.setAccessible(true);
                            fieldValue = field.get(attributable);
                        } else {
                            fieldValue = getter.invoke(attributable);
                        }

                        context.set(fieldName, fieldValue == null
                                ? ""
                                : (type.equals(Date.class)
                                ? DataFormat.format((Date) fieldValue, false)
                                : fieldValue));

                        LOG.debug("Add field {} with value {}", fieldName, fieldValue);

                    } catch (Exception iae) {
                        LOG.error("Reading '{}' value error", fieldName, iae);
                    }
                }
            }
        } catch (IntrospectionException ie) {
            LOG.error("Reading class attributes error", ie);
        }

        return context;
    }

    public static JexlContext addAttrsToContext(final Collection<? extends AbstractAttr> attrs,
            final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        for (AbstractAttr attr : attrs) {
            if (attr.getSchema() != null) {
                List<String> attrValues = attr.getValuesAsStrings();
                String expressionValue = attrValues.isEmpty()
                        ? ""
                        : attrValues.get(0);

                LOG.debug("Add attribute {} with value {}", attr.getSchema().getName(), expressionValue);

                context.set(attr.getSchema().getName(), expressionValue);
            }
        }

        return context;
    }

    public static JexlContext addDerAttrsToContext(final Collection<? extends AbstractDerAttr> derAttrs,
            final Collection<? extends AbstractAttr> attrs, final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        for (AbstractDerAttr derAttr : derAttrs) {
            if (derAttr.getSchema() != null) {
                String expressionValue = derAttr.getValue(attrs);
                if (expressionValue == null) {
                    expressionValue = "";
                }

                LOG.debug("Add derived attribute {} with value {}", derAttr.getSchema().getName(), expressionValue);

                context.set(derAttr.getSchema().getName(), expressionValue);
            }
        }

        return context;
    }

    public static JexlContext addVirAttrsToContext(final Collection<? extends AbstractVirAttr> virAttrs,
            final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext()
                : jexlContext;

        for (AbstractVirAttr virAttr : virAttrs) {
            if (virAttr.getSchema() != null) {
                List<String> attrValues = virAttr.getValues();
                String expressionValue = attrValues.isEmpty()
                        ? ""
                        : attrValues.get(0);

                LOG.debug("Add virtual attribute {} with value {}", virAttr.getSchema().getName(), expressionValue);

                context.set(virAttr.getSchema().getName(), expressionValue);
            }
        }

        return context;
    }

    public static String evaluate(final String expression, final AbstractAttributableTO attributableTO) {
        final JexlContext context = new MapContext();

        addFieldsToContext(attributableTO, context);

        for (AttributeTO attr : attributableTO.getAttrs()) {
            List<String> values = attr.getValues();
            String expressionValue = values.isEmpty()
                    ? ""
                    : values.get(0);

            LOG.debug("Add attribute {} with value {}", attr.getSchema(), expressionValue);

            context.set(attr.getSchema(), expressionValue);
        }
        for (AttributeTO derAttr : attributableTO.getDerAttrs()) {
            List<String> values = derAttr.getValues();
            String expressionValue = values.isEmpty()
                    ? ""
                    : values.get(0);

            LOG.debug("Add derived attribute {} with value {}", derAttr.getSchema(), expressionValue);

            context.set(derAttr.getSchema(), expressionValue);
        }
        for (AttributeTO virAttr : attributableTO.getVirAttrs()) {
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

    public static boolean evaluateMandatoryCondition(final String mandatoryCondition,
            final AbstractAttributable attributable) {

        JexlContext jexlContext = new MapContext();
        addAttrsToContext(attributable.getAttrs(), jexlContext);
        addDerAttrsToContext(attributable.getDerAttrs(), attributable.getAttrs(), jexlContext);
        addVirAttrsToContext(attributable.getVirAttrs(), jexlContext);

        return Boolean.parseBoolean(evaluate(mandatoryCondition, jexlContext));
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private JexlUtil() {
    }

}
