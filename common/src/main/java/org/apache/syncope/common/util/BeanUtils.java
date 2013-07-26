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
package org.apache.syncope.common.util;

import static org.springframework.beans.BeanUtils.getPropertyDescriptor;
import static org.springframework.beans.BeanUtils.getPropertyDescriptors;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.util.Assert;

/**
 * Overrides Spring's BeanUtils not using collection setters but instead getters + addAll() / putAll(),
 * in a JAXB friendly way.
 *
 * @see org.springframework.beans.BeanUtils
 * @see https://issues.apache.org/jira/browse/SYNCOPE-246
 */
public final class BeanUtils {

    private BeanUtils() {
        // Empty private constructor for static utility classes
    }

    /**
     * Copy the property values of the given source bean into the target bean.
     * <p>Note: The source and target classes do not have to match or even be derived
     * from each other, as long as the properties match. Any bean properties that the
     * source bean exposes but the target bean does not will silently be ignored.
     * <p>This is just a convenience method. For more complex transfer needs,
     * consider using a full BeanWrapper.
     *
     * @param source the source bean
     * @param target the target bean
     * @throws BeansException if the copying failed
     * @see org.springframework.beans.BeanWrapper
     */
    public static void copyProperties(final Object source, final Object target) throws BeansException {
        copyProperties(source, target, null);
    }

    /**
     * Copy the property values of the given source bean into the given target bean.
     * <p>Note: The source and target classes do not have to match or even be derived
     * from each other, as long as the properties match. Any bean properties that the
     * source bean exposes but the target bean does not will silently be ignored.
     *
     * @param source the source bean
     * @param target the target bean
     * @param ignoreProperties array of property names to ignore
     * @throws BeansException if the copying failed
     * @see org.springframework.beans.BeanWrapper
     */
    @SuppressWarnings("unchecked")
    public static void copyProperties(final Object source, final Object target, final String[] ignoreProperties)
            throws BeansException {

        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");

        for (PropertyDescriptor targetPd : getPropertyDescriptors(target.getClass())) {
            if (!ArrayUtils.contains(ignoreProperties, targetPd.getName())) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null && sourcePd.getReadMethod() != null) {
                    try {
                        Method readMethod = sourcePd.getReadMethod();
                        if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                            readMethod.setAccessible(true);
                        }
                        Object value = readMethod.invoke(source);

                        Method writeMethod = targetPd.getWriteMethod();
                        // Diverts from Spring's BeanUtils: if no write method is found and property is collection,
                        // try to use addAll() / putAll().
                        if (writeMethod == null) {
                            Method targetReadMethod = targetPd.getReadMethod();
                            if (targetReadMethod != null) {
                                if (!Modifier.isPublic(targetReadMethod.getDeclaringClass().getModifiers())) {
                                    targetReadMethod.setAccessible(true);
                                }
                                Object destValue = targetReadMethod.invoke(target);

                                if (value instanceof Collection && destValue instanceof Collection) {
                                    ((Collection) destValue).clear();
                                    ((Collection) destValue).addAll((Collection) value);
                                } else if (value instanceof Map && destValue instanceof Map) {
                                    ((Map) destValue).clear();
                                    ((Map) destValue).putAll((Map) value);
                                }
                            }
                        } else {
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(target, value);
                        }
                    } catch (Throwable ex) {
                        throw new FatalBeanException("Could not copy properties from source to target", ex);
                    }
                }
            }
        }
    }
}
