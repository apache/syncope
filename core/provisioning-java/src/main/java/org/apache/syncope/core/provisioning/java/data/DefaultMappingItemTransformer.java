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
package org.apache.syncope.core.provisioning.java.data;

import java.util.List;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default (empty) implementation of {@link MappingItemTransformer}.
 */
public class DefaultMappingItemTransformer implements MappingItemTransformer {

    @Transactional(readOnly = true)
    @Override
    public List<PlainAttrValue> beforePropagation(
            final MappingItem mappingItem,
            final Any<?> any,
            final List<PlainAttrValue> values) {

        return values;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Object> beforePull(
            final MappingItem mappingItem,
            final AnyTO anyTO,
            final List<Object> values) {

        return values;
    }

}
