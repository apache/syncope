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
package org.apache.syncope.sra;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public final class ApplicationContextUtils {

    public static <T> T getOrCreateBean(
            final ConfigurableApplicationContext ctx,
            final String actualClazz,
            final Class<T> type) throws ClassNotFoundException {

        T bean;
        if (ctx.getBeanFactory().containsSingleton(actualClazz)) {
            bean = type.cast(ctx.getBeanFactory().getSingleton(actualClazz));
        } else {
            if (ApplicationListener.class.isAssignableFrom(type)) {
                RootBeanDefinition bd = new RootBeanDefinition(Class.forName(actualClazz));
                bd.setScope(BeanDefinition.SCOPE_SINGLETON);
                ((BeanDefinitionRegistry) ctx.getBeanFactory()).registerBeanDefinition(actualClazz, bd);
                bean = ctx.getBean(type);
            } else {
                bean = type.cast(ctx.getBeanFactory().createBean(Class.forName(actualClazz)));
                ctx.getBeanFactory().registerSingleton(actualClazz, bean);
            }
        }
        return bean;
    }

    private ApplicationContextUtils() {
        // private constructor for static utility class
    }
}
