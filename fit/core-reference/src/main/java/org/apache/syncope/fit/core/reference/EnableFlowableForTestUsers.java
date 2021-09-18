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
package org.apache.syncope.fit.core.reference;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class EnableFlowableForTestUsers {

    private static final Logger LOG = LoggerFactory.getLogger(EnableFlowableForTestUsers.class);

    private final UserDAO userDAO;

    public EnableFlowableForTestUsers(final UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Transactional
    public void init(final DataSource datasource) {
        LOG.debug("Enabling Flowable processing for test users");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
        String procDef = jdbcTemplate.queryForObject(
                "SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_=?", String.class, "userWorkflow");
        LOG.debug("User workflow ID_ found: {}", procDef);

        AtomicInteger counter = new AtomicInteger(0);
        userDAO.findAll(0, 1000).forEach(user -> {
            int value = counter.addAndGet(1);
            jdbcTemplate.update("INSERT INTO "
                    + "ACT_RU_EXECUTION(ID_,REV_,PROC_INST_ID_,BUSINESS_KEY_,PROC_DEF_ID_,ACT_ID_,"
                    + "IS_ACTIVE_,IS_CONCURRENT_,IS_SCOPE_,IS_EVENT_SCOPE_,SUSPENSION_STATE_) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    value, 2, value, "userWorkflow:" + user.getKey(), procDef, "active",
                    true, false, true, false, true);

            value = counter.addAndGet(1);
            jdbcTemplate.update("INSERT INTO "
                    + "ACT_RU_TASK(ID_,REV_,EXECUTION_ID_,PROC_INST_ID_,PROC_DEF_ID_,NAME_,TASK_DEF_KEY_,PRIORITY_,"
                    + "CREATE_TIME_) "
                    + "VALUES(?,?,?,?,?,?,?,?,?)",
                    value, 2, value - 1, value - 1, procDef, "Active", "active", 50, new Date());
        });
    }
}
