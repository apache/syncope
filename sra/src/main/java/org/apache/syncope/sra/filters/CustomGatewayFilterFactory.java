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
package org.apache.syncope.sra.filters;

import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

/**
 * Base class for custom gateway filter factories.
 */
public abstract class CustomGatewayFilterFactory
        extends AbstractGatewayFilterFactory<CustomGatewayFilterFactory.Config> {

    public static final class Config {

        private String data;

        public String getData() {
            return data;
        }

        public void setData(final String data) {
            this.data = data;
        }
    }

    public CustomGatewayFilterFactory() {
        super(CustomGatewayFilterFactory.Config.class);
    }

    public Optional<Integer> getOrder() {
        return Optional.empty();
    }

    @Override
    public abstract GatewayFilter apply(Config config);
}
