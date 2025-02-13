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
package org.apache.syncope.core.provisioning.api;

import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;

public interface DerAttrHandler {

    /**
     * Calculates derived attribute value associated to the given any, for the given derived schema.
     *
     * @param any any object
     * @param schema derived schema
     * @return derived attribute value
     */
    String getValue(Any any, DerSchema schema);

    /**
     * Calculates derived attributes values associated to the given any.
     *
     * @param any any object
     * @return derived attribute values
     */
    Map<DerSchema, String> getValues(Any any);

    /**
     * Calculates derived attribute value associated to the given any, for the given membership and
     * derived schema.
     *
     * @param any any object
     * @param membership membership
     * @param schema derived schema
     * @return derived attribute value
     */
    String getValue(Any any, Membership<?> membership, DerSchema schema);

    /**
     * Calculates derived attributes values associated to the given any, for the given membership.
     *
     * @param any any object
     * @param membership membership
     * @return derived attribute values
     */
    Map<DerSchema, String> getValues(Groupable<?, ?, ?, ?> any, Membership<?> membership);
}
