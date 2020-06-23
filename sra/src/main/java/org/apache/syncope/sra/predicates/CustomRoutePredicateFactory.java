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
package org.apache.syncope.sra.predicates;

import java.util.function.Predicate;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.web.server.ServerWebExchange;

/**
 * Base class for custom predicate factories.
 */
public abstract class CustomRoutePredicateFactory
        extends AbstractRoutePredicateFactory<CustomRoutePredicateFactory.Config> {

    public static class Config {

        private String data;

        public String getData() {
            return data;
        }

        public void setData(final String data) {
            this.data = data;
        }
    }

    public CustomRoutePredicateFactory() {
        super(CustomRoutePredicateFactory.Config.class);
    }

    @Override
    public abstract AsyncPredicate<ServerWebExchange> applyAsync(Config config);

    @Override
    public Predicate<ServerWebExchange> apply(final Config config) {
        throw new UnsupportedOperationException(getClass().getName() + " is only async.");
    }
}
