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
package org.apache.syncope.core.provisioning.api.data;

import java.util.List;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;

/**
 * Transforms values to be propagated to (or pulling from) external resources right before they leave (or enter)
 * the Syncope internal storage.
 *
 * These transformations are not applied to virtual attribute values.
 */
public interface MappingItemTransformer {

    /**
     * Invoked while preparing attribute values to be sent out to external resource during propagation.
     *
     * @param mappingItem mapping item
     * @param any any object
     * @param values original values
     * @return transformed values
     */
    List<PlainAttrValue> beforePropagation(
            MappingItem mappingItem,
            Any<?> any,
            List<PlainAttrValue> values);

    /**
     * Invoked while reading attribute values from external resource during pull.
     *
     * @param mappingItem mapping item
     * @param anyTO any object
     * @param values original values
     * @return transformed values
     */
    List<Object> beforePull(
            MappingItem mappingItem,
            AnyTO anyTO,
            List<Object> values);
}
