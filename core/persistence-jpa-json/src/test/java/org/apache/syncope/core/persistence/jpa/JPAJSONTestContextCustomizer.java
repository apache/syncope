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
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;

public class JPAJSONTestContextCustomizer implements ContextCustomizer {

    private static BeanDefinitionRegistry getBeanDefinitionRegistry(final ApplicationContext ctx) {
        if (ctx instanceof BeanDefinitionRegistry) {
            return (BeanDefinitionRegistry) ctx;
        }
        if (ctx instanceof ConfigurableApplicationContext) {
            return (BeanDefinitionRegistry) ((ConfigurableApplicationContext) ctx).getBeanFactory();
        }
        throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
    }

    @Override
    public void customizeContext(final ConfigurableApplicationContext ctx, final MergedContextConfiguration cfg) {
        switch (System.getProperty("profileId")) {
            case "pgjsonb":
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                        ctx,
                        "provisioning.quartz.sql=tables_postgres.sql");
                break;

            case "myjson":
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                        ctx,
                        "provisioning.quartz.sql=tables_mysql_innodb.sql");
                break;

            case "ojson":
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                        ctx,
                        "provisioning.quartz.sql=tables_oracle.sql");
                break;

            default:
        }

        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(getBeanDefinitionRegistry(ctx));
        reader.registerBean(PGJPAJSONPersistenceContext.class, "PGJPAJSONPersistenceContext");
        reader.registerBean(MyJPAJSONPersistenceContext.class, "MyJPAJSONPersistenceContext");
        reader.registerBean(OJPAJSONPersistenceContext.class, "OJPAJSONPersistenceContext");
    }
}
