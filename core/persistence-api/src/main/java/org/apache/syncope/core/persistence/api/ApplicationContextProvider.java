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
package org.apache.syncope.core.persistence.api;

import java.util.Optional;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationContextProvider implements ApplicationContextAware {

    private static ConfigurableApplicationContext CTX;

    private static DefaultListableBeanFactory BEAN_FACTORY;

    public static ConfigurableApplicationContext getApplicationContext() {
        return CTX;
    }

    public static void setApplicationContext(final ConfigurableApplicationContext ctx) {
        CTX = ctx;
    }

    public static DefaultListableBeanFactory getBeanFactory() {
        return Optional.ofNullable(BEAN_FACTORY).
                orElseGet(() -> Optional.ofNullable(CTX).
                map(ctx -> (DefaultListableBeanFactory) ctx.getBeanFactory()).
                orElse(null));
    }

    public static void setBeanFactory(final DefaultListableBeanFactory beanFactory) {
        BEAN_FACTORY = beanFactory;
    }

    /**
     * Wiring the ApplicationContext into a static method.
     *
     * @param ctx Spring application context
     */
    @Override
    public void setApplicationContext(final ApplicationContext ctx) {
        CTX = (ConfigurableApplicationContext) ctx;
    }
}
