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

import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepo
        extends PagingAndSortingRepository<Neo4jUser, String>, UserRepoExt, UserDAO {

    @Query("MATCH (n:" + Neo4jUser.NODE + ") WHERE n.username = $username RETURN n.id")
    @Override
    Optional<String> findKey(@Param("username") String username);

    @Query("MATCH (n:" + Neo4jUser.NODE + ") WHERE n.id = $key RETURN n.username")
    @Override
    Optional<String> findUsername(@Param("key") String key);
}
