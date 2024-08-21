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
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface GroupRepo
        extends PagingAndSortingRepository<JPAGroup, String>, GroupRepoExt, GroupDAO {

    @Query("SELECT e.id FROM #{#entityName} e WHERE e.name = :name")
    @Override
    Optional<String> findKey(@Param("name") String name);

    @Query("SELECT e.id FROM #{#entityName} e WHERE LOWER(e.name) LIKE :pattern")
    @Override
    List<String> findKeysByNamePattern(@Param("pattern") String pattern);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id IN (:keys)")
    @Override
    List<Group> findByKeys(@Param("keys") List<String> keys);

    @Query("SELECT e FROM #{#entityName} e WHERE e.groupOwner.id = :groupKey")
    @Override
    List<Group> findOwnedByGroup(@Param("groupKey") String groupKey);

    @Query("SELECT DISTINCT e.leftEnd.id FROM JPAAMembership e WHERE e.rightEnd.id = :groupKey")
    @Override
    List<String> findAMembers(@Param("groupKey") String groupKey);

    @Query("SELECT DISTINCT e.leftEnd.id FROM JPAUMembership e WHERE e.rightEnd.id = :groupKey")
    @Override
    List<String> findUMembers(@Param("groupKey") String groupKey);

    @Query("SELECT COUNT(DISTINCT e.leftEnd.id) FROM JPAAMembership e WHERE e.rightEnd.id = :groupKey")
    @Override
    long countAMembers(@Param("groupKey") String groupKey);

    @Query("SELECT COUNT(DISTINCT e.leftEnd.id) FROM JPAUMembership e WHERE e.rightEnd.id = :groupKey")
    @Override
    long countUMembers(@Param("groupKey") String groupKey);
}
