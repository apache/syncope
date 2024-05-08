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
package org.apache.syncope.core.persistence.api.dao;

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;

public interface ExternalResourceDAO extends DAO<ExternalResource> {

    ExternalResource authFind(String key);

    default boolean anyItemHaving(Implementation transformer) {
        return findAll().stream().
                flatMap(resource -> resource.getProvisions().stream()).
                flatMap(provision -> provision.getMapping().getItems().stream()).
                filter(item -> item.getTransformers().contains(transformer.getKey())).
                count() > 0;
    }

    List<ExternalResource> findByConnInstance(String connInstance);

    List<ExternalResource> findByProvisionSorter(Implementation provisionSorter);

    List<ExternalResource> findByPropagationActionsContaining(Implementation propagationActions);

    List<ExternalResource> findByPolicy(Policy policy);

    void deleteMapping(String schemaKey);
}
