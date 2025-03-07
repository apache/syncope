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

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Needed with Flowable 6.5.0 or higher.
 *
 * For more information, check <a href="https://github.com/flowable/flowable-engine/issues/2142">here</a>
 */
public class FlowableLiquibaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String LIQUIBASE_PROPERTY = "spring.liquibase.enabled";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment env, final SpringApplication app) {
        if (!env.containsProperty(LIQUIBASE_PROPERTY)) {
            env.getPropertySources().addLast(
                    new MapPropertySource("flowable-liquibase-override", Map.of(LIQUIBASE_PROPERTY, false)));
        }
    }
}
