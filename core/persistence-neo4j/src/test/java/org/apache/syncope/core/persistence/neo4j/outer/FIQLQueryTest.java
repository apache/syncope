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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class FIQLQueryTest extends AbstractTest {

    @Autowired
    private FIQLQueryDAO fiqlQueryDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void findByOwner() {
        FIQLQuery query = entityFactory.newEntity(FIQLQuery.class);
        query.setOwner(userDAO.findByUsername("rossini").orElseThrow());
        query.setName("name");
        query.setFIQL("id!=$null");
        query.setTarget("target");
        query = fiqlQueryDAO.save(query);

        FIQLQuery fiqlQuery = entityFactory.newEntity(FIQLQuery.class);
        fiqlQuery.setOwner(userDAO.findByUsername("rossini").orElseThrow());

        assertEquals(
                List.of(query),
                fiqlQueryDAO.findByOwner(userDAO.findByUsername("rossini").orElseThrow(), null));
        assertEquals(
                List.of(query),
                fiqlQueryDAO.findByOwner(userDAO.findByUsername("rossini").orElseThrow(), "target"));
    }
}
