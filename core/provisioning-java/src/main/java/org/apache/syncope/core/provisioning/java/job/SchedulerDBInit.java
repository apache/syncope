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
package org.apache.syncope.core.provisioning.java.job;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.util.Assert;

/**
 * Ensure Quartz database initialization occurs only if Quartz tables are not already present.
 *
 * @see org.springframework.jdbc.datasource.init.DataSourceInitializer
 */
public class SchedulerDBInit implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerDBInit.class);

    private DataSource dataSource;

    private DatabasePopulator databasePopulator;

    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setDatabasePopulator(final DatabasePopulator databasePopulator) {
        this.databasePopulator = databasePopulator;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.state(this.dataSource != null, "DataSource must be set");
        Assert.state(this.databasePopulator != null, "DatabasePopulator must be set");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
        boolean existingData;
        try {
            existingData = jdbcTemplate.queryForObject("SELECT COUNT(0) FROM QRTZ_SCHEDULER_STATE", Integer.class) > 0;
        } catch (BadSqlGrammarException e) {
            LOG.debug("Could not access to table QRTZ_SCHEDULER_STATE", e);
            existingData = false;
        }

        if (existingData) {
            LOG.info("Quartz tables found in the database, leaving untouched");
        } else {
            LOG.info("No Quartz tables found, creating");

            DatabasePopulatorUtils.execute(databasePopulator, this.dataSource);
        }
    }

}
