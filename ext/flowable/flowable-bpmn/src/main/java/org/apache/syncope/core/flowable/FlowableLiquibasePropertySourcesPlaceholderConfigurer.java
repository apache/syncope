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
package org.apache.syncope.core.flowable;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

/**
 * Needed with Flowable 6.5.0 or higer.
 *
 * For more information, check https://github.com/flowable/flowable-engine/issues/2142
 */
@Component
public class FlowableLiquibasePropertySourcesPlaceholderConfigurer
        extends PropertySourcesPlaceholderConfigurer
        implements EnvironmentAware, InitializingBean {

    private static final String LIQUIBASE_PROPERTY = "spring.liquibase.enabled";

    private ConfigurableEnvironment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = (ConfigurableEnvironment) env;
        super.setEnvironment(env);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!env.containsProperty(LIQUIBASE_PROPERTY)) {
            Map<String, Object> props = new HashMap<>();
            props.put(LIQUIBASE_PROPERTY, false);
            env.getPropertySources().addLast(new MapPropertySource("flowable-liquibase-override", props));
        }
    }
}
