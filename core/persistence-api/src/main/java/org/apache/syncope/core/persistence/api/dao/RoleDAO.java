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
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface RoleDAO extends DAO<Role> {

    int count();

    Role find(String key);

    List<Role> findByRealm(Realm realm);

    List<Role> findByPrivilege(Privilege privilege);

    List<Role> findAll();

    Role save(Role role);

    Role saveAndRefreshDynMemberships(Role role);

    void delete(Role role);

    void delete(String key);

    List<String> findDynMembers(Role role);

    void clearDynMembers(Role role);

    void refreshDynMemberships(User user);

    void removeDynMemberships(String key);

}
