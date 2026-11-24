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
package org.apache.syncope.core.flowable.support;

import java.util.AbstractMap;
import java.util.Set;
import org.apache.syncope.core.flowable.impl.FlowableWorkflowUtils;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Read-only {@link java.util.Map} exposing only the Spring beans which are allowed to be referenced from UEL
 * expressions, that is {@link JavaDelegate} implementations (service tasks) and {@link FlowableWorkflowUtils}.
 * <p>
 * Beans are resolved lazily by name, so that beans depending on {@link DomainProcessEngine} - such as
 * {@code flowableUtils} - can be resolved without having to be instantiated while the engine itself is being built.
 */
public class AllowedBeansMap extends AbstractMap<Object, Object> {

    protected final ApplicationContext ctx;

    public AllowedBeansMap(final ApplicationContext ctx) {
        this.ctx = ctx;
    }

    protected boolean isAllowed(final String name) {
        if (!ctx.containsBean(name)) {
            return false;
        }

        try {
            Class<?> type = ctx.getType(name);
            return type != null
                    && (JavaDelegate.class.isAssignableFrom(type)
                    || FlowableWorkflowUtils.class.isAssignableFrom(type));
        } catch (BeansException e) {
            return false;
        }
    }

    @Override
    public boolean containsKey(final Object key) {
        return key instanceof String && isAllowed((String) key);
    }

    @Override
    public Object get(final Object key) {
        return containsKey(key) ? ctx.getBean((String) key) : null;
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        throw new UnsupportedOperationException("Allowed beans are resolved lazily");
    }
}
