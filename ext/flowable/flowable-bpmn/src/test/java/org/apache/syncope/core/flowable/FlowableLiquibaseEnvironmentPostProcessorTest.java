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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class FlowableLiquibaseEnvironmentPostProcessorTest {

    private static final String LIQUIBASE_PROPERTY = "spring.liquibase.enabled";

    private final FlowableLiquibaseEnvironmentPostProcessor postProcessor =
            new FlowableLiquibaseEnvironmentPostProcessor();

    @Test
    void defaultDisabled() {
        StandardEnvironment environment = new StandardEnvironment();

        postProcessor.postProcessEnvironment(environment, null);

        assertTrue(environment.containsProperty(LIQUIBASE_PROPERTY));
        assertEquals("false", environment.getProperty(LIQUIBASE_PROPERTY));
        assertTrue(environment.getPropertySources().contains("flowable-liquibase-override"));
    }

    @Test
    void keepExplicitValue() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of(LIQUIBASE_PROPERTY, "true")));

        postProcessor.postProcessEnvironment(environment, null);

        assertEquals("true", environment.getProperty(LIQUIBASE_PROPERTY));
        assertFalse(environment.getPropertySources().contains("flowable-liquibase-override"));
    }
}
