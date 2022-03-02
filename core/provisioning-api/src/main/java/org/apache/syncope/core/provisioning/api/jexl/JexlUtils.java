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
package org.apache.syncope.core.provisioning.api.jexl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * JEXL <a href="http://commons.apache.org/jexl/reference/index.html">reference</a> is available.
 */
@SuppressWarnings({ "squid:S3008", "squid:S3776", "squid:S1141" })
public final class JexlUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JexlUtils.class);

    private static final String[] IGNORE_FIELDS = { "password", "clearPassword", "serialVersionUID", "class" };

    private static final Map<Class<?>, Set<Pair<PropertyDescriptor, Field>>> FIELD_CACHE =
            Collections.synchronizedMap(new HashMap<>());

    private static JexlEngine JEXL_ENGINE;

    private static JexlEngine getEngine() {
        synchronized (LOG) {
            if (JEXL_ENGINE == null) {
                JEXL_ENGINE = new JexlBuilder().
                        uberspect(new SandboxUberspect()).
                        loader(new EmptyClassLoader()).
                        namespaces(Map.of("syncope", new SyncopeJexlFunctions())).
                        cache(512).
                        silent(false).
                        strict(false).
                        create();
            }
        }

        return JEXL_ENGINE;
    }

    public static JxltEngine newJxltEngine() {
        return getEngine().createJxltEngine(false);
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
        String result = StringUtils.EMPTY;

        if (StringUtils.isNotBlank(expression) && jexlContext != null) {
            try {
                JexlExpression jexlExpression = getEngine().createExpression(expression);
                Object evaluated = jexlExpression.evaluate(jexlContext);
                if (evaluated != null) {
                    result = evaluated.toString();
                }
            } catch (Exception e) {
                LOG.error("Error while evaluating JEXL expression: " + expression, e);
            }
        } else {
            LOG.debug("Expression not provided or invalid context");
        }

        return result;
    }

    public static void addFieldsToContext(final Object object, final JexlContext jexlContext) {
        Set<Pair<PropertyDescriptor, Field>> cached = FIELD_CACHE.get(object.getClass());
        if (cached == null) {
            FIELD_CACHE.put(object.getClass(), Collections.synchronizedSet(new HashSet<>()));

            List<Class<?>> classes = ClassUtils.getAllSuperclasses(object.getClass());
            classes.add(object.getClass());
            classes.forEach(clazz -> {
                try {
                    for (PropertyDescriptor desc : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
                        if (!desc.getName().startsWith("pc")
                                && !ArrayUtils.contains(IGNORE_FIELDS, desc.getName())
                                && !Collection.class.isAssignableFrom(desc.getPropertyType())
                                && !Map.class.isAssignableFrom(desc.getPropertyType())
                                && !desc.getPropertyType().isArray()) {

                            Field field = null;
                            try {
                                field = clazz.getDeclaredField(desc.getName());
                            } catch (NoSuchFieldException | SecurityException e) {
                                LOG.debug("Could not get field {} from {}", desc.getName(), clazz.getName(), e);
                            }

                            FIELD_CACHE.get(object.getClass()).add(Pair.of(desc, field));
                        }
                    }
                } catch (IntrospectionException e) {
                    LOG.warn("Could not introspect {}", clazz.getName(), e);
                }
            });

            cached = FIELD_CACHE.get(object.getClass());
        }

        cached.forEach(fd -> {
            String fieldName = fd.getLeft().getName();
            Class<?> fieldType = fd.getLeft().getPropertyType();

            try {
                Object fieldValue = null;
                if (fd.getLeft().getReadMethod() == null) {
                    if (fd.getRight() != null) {
                        ReflectionUtils.makeAccessible(fd.getRight());
                        fieldValue = fd.getRight().get(object);
                    }
                } else {
                    fieldValue = fd.getLeft().getReadMethod().invoke(object);
                }
                if (fieldValue == null) {
                    fieldValue = StringUtils.EMPTY;
                } else {
                    fieldValue = TemporalAccessor.class.isAssignableFrom(fieldType)
                            ? FormatUtils.format((TemporalAccessor) fieldValue)
                            : fieldValue;
                }

                jexlContext.set(fieldName, fieldValue);

                LOG.debug("Add field {} with value {}", fieldName, fieldValue);
            } catch (Exception iae) {
                LOG.error("Reading '{}' value error", fieldName, iae);
            }
        });

        if (object instanceof Any && ((Any<?>) object).getRealm() != null) {
            jexlContext.set("realm", ((Any<?>) object).getRealm().getFullPath());
        } else if (object instanceof AnyTO && ((AnyTO) object).getRealm() != null) {
            jexlContext.set("realm", ((AnyTO) object).getRealm());
        } else if (object instanceof Realm) {
            jexlContext.set("fullPath", ((Realm) object).getFullPath());
        } else if (object instanceof RealmTO) {
            jexlContext.set("fullPath", ((RealmTO) object).getFullPath());
        }
    }

    public static void addAttrsToContext(final Collection<Attr> attrs, final JexlContext jexlContext) {
        attrs.stream().filter(attr -> attr.getSchema() != null).forEach(attr -> {
            Object value;
            if (attr.getValues().isEmpty()) {
                value = StringUtils.EMPTY;
            } else {
                value = attr.getValues().size() == 1
                        ? attr.getValues().get(0)
                        : attr.getValues();
            }

            LOG.debug("Add attribute {} with value {}", attr.getSchema(), value);

            jexlContext.set(attr.getSchema(), value);
        });
    }

    public static void addPlainAttrsToContext(
            final Collection<? extends PlainAttr<?>> attrs, final JexlContext jexlContext) {

        attrs.stream().filter(attr -> attr.getSchema() != null).forEach(attr -> {
            List<String> attrValues = attr.getValuesAsStrings();
            Object value;
            if (attrValues.isEmpty()) {
                value = StringUtils.EMPTY;
            } else {
                value = attrValues.size() == 1
                        ? attrValues.get(0)
                        : attrValues;
            }

            LOG.debug("Add attribute {} with value {}", attr.getSchema().getKey(), value);

            jexlContext.set(attr.getSchema().getKey(), value);
        });
    }

    public static void addDerAttrsToContext(
            final Any<?> any,
            final DerAttrHandler derAttrHandler,
            final JexlContext jexlContext) {

        Map<DerSchema, String> derAttrs = derAttrHandler.getValues(any);

        derAttrs.forEach((schema, value) -> jexlContext.set(schema.getKey(), value));
    }

    public static boolean evaluateMandatoryCondition(
            final String mandatoryCondition,
            final Any<?> any,
            final DerAttrHandler derAttrHandler) {

        JexlContext jexlContext = new MapContext();
        addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
        addDerAttrsToContext(any, derAttrHandler, jexlContext);

        return Boolean.parseBoolean(evaluate(mandatoryCondition, jexlContext));
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private JexlUtils() {
    }
}
