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
package org.apache.syncope.core.persistence.neo4j;

import java.io.IOException;
import java.io.InputStream;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@EnableConfigurationProperties(PersistenceProperties.class)
@Configuration(proxyBeanMethods = false)
public class MasterDomain {

    @ConditionalOnMissingBean(name = "MasterDriver")
    @Bean(name = "MasterDriver")
    public Driver masterDriver(final PersistenceProperties props) {
        return GraphDatabase.driver(
                props.getDomain().getFirst().getUri(),
                AuthTokens.basic(props.getDomain().getFirst().getUsername(),
                    props.getDomain().getFirst().getPassword()),
                Config.builder().
                        withMaxConnectionPoolSize(props.getDomain().getFirst().getMaxConnectionPoolSize()).
                        withDriverMetrics().
                        withLogging(Logging.slf4j()).build());
    }

    @Bean(name = "MasterContentXML")
    public InputStream masterContentXML(
            final ResourceLoader resourceLoader,
            final PersistenceProperties props) throws IOException {

        return resourceLoader.getResource(props.getDomain().getFirst().getContent()).getInputStream();
    }

    @Bean(name = "MasterKeymasterConfParamsJSON")
    public InputStream masterKeymasterConfParamsJSON(
            final ResourceLoader resouceLoader,
            final PersistenceProperties props) throws IOException {

        return resouceLoader.getResource(props.getDomain().getFirst().getKeymasterConfParams()).getInputStream();
    }
}
