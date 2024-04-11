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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;

public interface VirSchemaRepoExt {

    String CACHE = "virSchemaCache";

    Optional<? extends VirSchema> findById(String key);

    List<? extends VirSchema> findByIdLike(String keyword);

    List<? extends VirSchema> findByAnyTypeClasses(Collection<AnyTypeClass> anyTypeClasses);

    List<? extends VirSchema> findByResource(ExternalResource resource);

    List<VirSchema> findByResourceAndAnyType(String resource, String anyType);

    VirSchema save(VirSchema schema);

    void deleteById(String key);

    void delete(VirSchema schema);
}
