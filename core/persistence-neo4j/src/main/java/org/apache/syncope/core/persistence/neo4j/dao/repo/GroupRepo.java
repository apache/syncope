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

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface GroupRepo
        extends PagingAndSortingRepository<Neo4jGroup, String>, GroupRepoExt, GroupDAO {

    @Query("MATCH (n:" + Neo4jGroup.NODE + ") WHERE n.name = $name RETURN n.id")
    @Override
    Optional<String> findKey(@Param("name") String name);

    @Query("MATCH (n:" + Neo4jGroup.NODE + ") WHERE toLower(n.name) =~ $pattern RETURN n.id")
    @Override
    List<String> findKeysByNamePattern(@Param("pattern") String pattern);

    @Query("MATCH (a:" + Neo4jAnyObject.NODE + " {id: $anyObjectKey})-[]-"
            + "(n:" + Neo4jAMembership.NODE + ")-[]-"
            + "(g:" + Neo4jGroup.NODE + " {id: $groupKey}) "
            + "RETURN COUNT(n) > 0")
    @Override
    boolean existsAMembership(String anyObjectKey, String groupKey);

    @Query("MATCH (u:" + Neo4jUser.NODE + " {id: $userKey})-[]-"
            + "(n:" + Neo4jUMembership.NODE + ")-[]-"
            + "(g:" + Neo4jGroup.NODE + " {id: $groupKey}) "
            + "RETURN COUNT(n) > 0")
    @Override
    boolean existsUMembership(String userKey, String groupKey);

    @Query("MATCH (a:" + Neo4jAnyObject.NODE + ")-[]-"
            + "(n:" + Neo4jAMembership.NODE + ")-[]-"
            + "(g:" + Neo4jGroup.NODE + " {id: $groupKey}) "
            + "RETURN a.id")
    @Override
    List<String> findAMembers(@Param("groupKey") String groupKey);

    @Query("MATCH (u:" + Neo4jUser.NODE + ")-[]-"
            + "(n:" + Neo4jUMembership.NODE + ")-[]-"
            + "(g:" + Neo4jGroup.NODE + " {id: $groupKey}) "
            + "RETURN u.id")
    @Override
    List<String> findUMembers(@Param("groupKey") String groupKey);

    @Query("MATCH (a:" + Neo4jAnyObject.NODE + ")-[]-"
            + "(n:" + Neo4jAMembership.NODE + ")-[]-"
            + "(g:" + Neo4jGroup.NODE + " {id: $groupKey}) "
            + "RETURN COUNT(DISTINCT a)")
    @Override
    long countAMembers(@Param("groupKey") String groupKey);

    @Query("MATCH (u:" + Neo4jUser.NODE + ")-[]-"
            + "(n:" + Neo4jUMembership.NODE + ")-[]-"
            + "(g:" + Neo4jGroup.NODE + " {id: $groupKey}) "
            + "RETURN COUNT(DISTINCT u)")
    @Override
    long countUMembers(@Param("groupKey") String groupKey);
}
