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
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepo
        extends PagingAndSortingRepository<JPAUser, String>, UserRepoExt, UserDAO {

    @Query("SELECT e.id FROM #{#entityName} e WHERE e.username = :username")
    @Override
    Optional<String> findKey(@Param("username") String username);

    @Query("SELECT e.username FROM #{#entityName} e WHERE e.id = :key")
    @Override
    Optional<String> findUsername(@Param("key") String key);

    @Query("SELECT e FROM #{#entityName} e WHERE e.token LIKE :token")
    @Override
    Optional<? extends User> findByToken(@Param("token") String token);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id IN (:keys)")
    @Override
    List<User> findByKeys(@Param("keys") List<String> keys);

    @Query("SELECT e FROM JPALinkedAccount e "
            + "WHERE e.resource = :resource AND e.connObjectKeyValue = :connObjectKeyValue")
    @Override
    Optional<? extends LinkedAccount> findLinkedAccount(
            @Param("resource") ExternalResource resource,
            @Param("connObjectKeyValue") String connObjectKeyValue);

    @Query("SELECT e FROM JPALinkedAccount e WHERE e.owner.id = :userKey")
    @Override
    List<LinkedAccount> findLinkedAccounts(@Param("userKey") String userKey);

    @Query("SELECT e FROM JPALinkedAccount e WHERE e.resource = :resource")
    @Override
    List<LinkedAccount> findLinkedAccountsByResource(@Param("resource") ExternalResource resource);
}
