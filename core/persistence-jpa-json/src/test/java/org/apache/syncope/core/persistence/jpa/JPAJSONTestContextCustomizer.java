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
package org.apache.syncope.core.persistence.jpa;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

public class JPAJSONTestContextCustomizer implements ContextCustomizer {

    private static BeanDefinitionRegistry getBeanDefinitionRegistry(final ApplicationContext ctx) {
        if (ctx instanceof BeanDefinitionRegistry) {
            return (BeanDefinitionRegistry) ctx;
        }
        if (ctx instanceof AbstractApplicationContext) {
            return (BeanDefinitionRegistry) ((AbstractApplicationContext) ctx).getBeanFactory();
        }
        throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
    }

    @Override
    public void customizeContext(final ConfigurableApplicationContext ctx, final MergedContextConfiguration cfg) {
        if ("pgjsonb".equals(System.getProperty("profileId"))) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    ctx,
                    "provisioning.quartz.delegate=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        } else if ("myjson".equals(System.getProperty("profileId"))) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    ctx,
                    "provisioning.quartz.delegate=org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        }

        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(getBeanDefinitionRegistry(ctx));
        reader.registerBean(PGJPAJSONPersistenceContext.class, "PGJPAJSONPersistenceContext");
        reader.registerBean(MyJPAJSONPersistenceContext.class, "MyJPAJSONPersistenceContext");
    }
}
