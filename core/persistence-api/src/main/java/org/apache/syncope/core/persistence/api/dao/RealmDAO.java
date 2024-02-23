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
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RealmDAO extends DAO<Realm> {

    Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9~]+");

    Pattern PATH_PATTERN = Pattern.compile("^(/[A-Za-z0-9~]+)+");

    Realm getRoot();

    List<Realm> findByResources(ExternalResource resource);

    <T extends Policy> List<Realm> findByPolicy(T policy);

    List<Realm> findByActionsContaining(Implementation logicActions);

    Page<? extends Realm> findAll(Pageable pageable);
}
