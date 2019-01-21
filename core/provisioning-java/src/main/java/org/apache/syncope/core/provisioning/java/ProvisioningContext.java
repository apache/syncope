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
package org.apache.syncope.core.provisioning.java;

import java.lang.reflect.InvocationTargetException;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@PropertySource("classpath:connid.properties")
@PropertySource("classpath:mail.properties")
@PropertySource("classpath:provisioning.properties")
@PropertySource(value = "file:${conf.directory}/connid.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${conf.directory}/mail.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${conf.directory}/provisioning.properties", ignoreResourceNotFound = true)
@ImportResource("classpath:/provisioningContext.xml")
@ComponentScan("org.apache.syncope.core.provisioning.java")
@Configuration
public class ProvisioningContext implements EnvironmentAware {

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    @Bean
    public PropagationTaskExecutor propagationTaskExecutor()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (PropagationTaskExecutor) Class.forName(env.getProperty("propagationTaskExecutor")).
                getConstructor().newInstance();
    }

    @Bean
    public UserProvisioningManager userProvisioningManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (UserProvisioningManager) Class.forName(env.getProperty("userProvisioningManager")).
                getConstructor().newInstance();
    }

    @Bean
    public GroupProvisioningManager groupProvisioningManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (GroupProvisioningManager) Class.forName(env.getProperty("groupProvisioningManager")).
                getConstructor().newInstance();
    }

    @Bean
    public AnyObjectProvisioningManager anyObjectProvisioningManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (AnyObjectProvisioningManager) Class.forName(env.getProperty("anyObjectProvisioningManager")).
                getConstructor().newInstance();
    }

    @Bean
    public VirAttrCache virAttrCache()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (VirAttrCache) Class.forName(env.getProperty("virAttrCache")).
                getConstructor().newInstance();
    }

    @Bean
    public NotificationManager notificationManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (NotificationManager) Class.forName(env.getProperty("notificationManager")).
                getConstructor().newInstance();
    }

    @Bean
    public AuditManager auditManager()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (AuditManager) Class.forName(env.getProperty("auditManager")).
                getConstructor().newInstance();
    }
}
