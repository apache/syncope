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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;

public interface RealmRepoExt {

    Realm getRoot();

    Optional<Realm> findByFullPath(String fullPath);

    List<Realm> findByName(String name);

    long countDescendants(String base, String keyword);

    List<Realm> findDescendants(String base, String keyword, int page, int itemsPerPage);

    List<String> findDescendants(String base, String prefix);

    <T extends Policy> List<Realm> findByPolicy(T policy);

    List<Realm> findByLogicActions(Implementation logicActions);

    List<Realm> findAncestors(Realm realm);

    List<Realm> findChildren(Realm realm);

    List<String> findAllKeys(int page, int itemsPerPage);

    Realm save(Realm realm);

    void delete(Realm realm);
}
