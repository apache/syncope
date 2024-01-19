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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPADelegation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface DelegationRepo
        extends ListCrudRepository<JPADelegation, String>, DelegationDAO {

    @Query("SELECT e.id FROM #{#entityName} e "
            + "WHERE e.delegating.id = :delegating "
            + "AND e.delegated.id = :delegated "
            + "AND e.startDate <= :now AND (e.endDate IS NULL OR e.endDate >= :now)")
    @Override
    Optional<String> findValidFor(
            @Param("delegating") String delegating,
            @Param("delegated") String delegated,
            @Param("now") OffsetDateTime now);

    @Query("SELECT e.delegating.username FROM #{#entityName} e "
            + "WHERE e.delegated.id = :delegated "
            + "AND e.startDate <= :now AND (e.endDate IS NULL OR e.endDate >= :now)")
    @Override
    List<String> findValidDelegating(@Param("delegated") String delegated, @Param("now") OffsetDateTime now);
}
