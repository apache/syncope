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
package org.apache.syncope.core.persistence.api;

import javax.sql.DataSource;
import org.springframework.core.Ordered;

@FunctionalInterface
public interface SyncopeCoreLoader extends Ordered {

    /**
     * Perform generic (not related to any domain) initialization operations.
     */
    default void load() {
        // nothing to do
    }

    /**
     * Perform initialization operations on the given domain.
     *
     * @param domain domain to initialize
     * @param datasource db access for the given domain
     */
    default void load(String domain, DataSource datasource) {
        // nothing to do        
    }

    /**
     * Perform closing operations on the given domain.
     *
     * @param domain domain to unload
     */
    default void unload(String domain) {
        // nothing to do        
    }
}
