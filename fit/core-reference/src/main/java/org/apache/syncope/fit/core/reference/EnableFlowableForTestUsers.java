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

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        String procDef = null;
        try (Connection conn = datasource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                        "SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_=?")) {

            stmt.setString(1, "userWorkflow");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                procDef = rs.getString(1);
            }
        } catch (Exception e) {
            LOG.error("While attempting to read from ACT_RE_PROCDEF", e);
        }

        if (procDef == null) {
            LOG.error("Unable to determine Flowable process definition, aborting");
            return;
        }

        AtomicInteger counter = new AtomicInteger(0);
        for (User user : userDAO.findAll()) {
            int value = counter.addAndGet(1);
            try (Connection conn = datasource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                            """
                    INSERT INTO ACT_RU_EXECUTION(ID_,REV_,PROC_INST_ID_,BUSINESS_KEY_,PROC_DEF_ID_,ACT_ID_,
                    IS_ACTIVE_,IS_CONCURRENT_,IS_SCOPE_,IS_EVENT_SCOPE_,SUSPENSION_STATE_)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?)
                    """)) {

                stmt.setInt(1, value);
                stmt.setInt(2, 2);
                stmt.setInt(3, value);
                stmt.setString(4, "userWorkflow:" + user.getKey());
                stmt.setString(5, procDef);
                stmt.setString(6, "active");
                stmt.setBoolean(7, true);
                stmt.setBoolean(8, false);
                stmt.setBoolean(9, true);
                stmt.setBoolean(10, false);
                stmt.setInt(11, 0);

                stmt.executeUpdate();
            } catch (Exception e) {
                LOG.error("While attempting to update ACT_RU_EXECUTION", e);
            }

            value = counter.addAndGet(1);
            try (Connection conn = datasource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                            """
                    INSERT INTO ACT_RU_TASK(ID_,REV_,EXECUTION_ID_,PROC_INST_ID_,PROC_DEF_ID_,NAME_,TASK_DEF_KEY_,
                    PRIORITY_,CREATE_TIME_)
                    VALUES(?,?,?,?,?,?,?,?,?)
                    """)) {

                stmt.setInt(1, value);
                stmt.setInt(2, 2);
                stmt.setInt(3, value - 1);
                stmt.setInt(4, value - 1);
                stmt.setString(5, procDef);
                stmt.setString(6, "Active");
                stmt.setString(7, "active");
                stmt.setInt(8, 50);
                stmt.setDate(9, new Date(user.getCreationDate().toInstant().toEpochMilli()));

                stmt.executeUpdate();
            } catch (Exception e) {
                LOG.error("While attempting to update ACT_RU_EXECUTION", e);
            }
        }
    }
}
