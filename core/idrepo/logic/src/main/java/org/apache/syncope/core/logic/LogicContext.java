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
package org.apache.syncope.core.logic;

import java.lang.reflect.InvocationTargetException;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@PropertySource("classpath:logic.properties")
@PropertySource(value = "file:${conf.directory}/logic.properties", ignoreResourceNotFound = true)
@ComponentScan("org.apache.syncope.core.logic")
@EnableAspectJAutoProxy
@Configuration
public class LogicContext implements EnvironmentAware {

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    @Bean
    public String version() {
        return env.getProperty("version");
    }

    @Bean
    public String buildNumber() {
        return env.getProperty("buildNumber");
    }

    @Bean
    public LogicInvocationHandler logicInvocationHandler()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
            NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

        return (LogicInvocationHandler) Class.forName(env.getProperty("logicInvocationHandler")).
                getDeclaredConstructor().newInstance();
    }

    @Bean
    public ImplementationLookup classPathScanImplementationLookup()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
            NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

        return (ImplementationLookup) Class.forName(env.getProperty("classPathScanImplementationLookup")).
                getDeclaredConstructor().newInstance();
    }
}
