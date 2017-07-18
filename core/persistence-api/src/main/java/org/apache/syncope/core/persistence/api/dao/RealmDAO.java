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
import java.util.regex.Pattern;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;

public interface RealmDAO extends DAO<Realm> {

    Pattern PATH_PATTERN = Pattern.compile("^(/[A-Za-z0-9]+)+");

    Realm getRoot();

    Realm find(String key);

    Realm findByFullPath(String fullPath);

    List<Realm> findByName(String name);

    List<Realm> findByResource(ExternalResource resource);

    <T extends Policy> List<Realm> findByPolicy(T policy);

    List<Realm> findAncestors(Realm realm);

    List<Realm> findChildren(Realm realm);

    List<Realm> findDescendants(Realm realm);

    List<Realm> findAll();

    Realm save(Realm realm);

    void delete(Realm realm);

    void delete(String key);
}
