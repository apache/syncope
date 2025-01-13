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
package org.apache.syncope.core.persistence.jpa.upgrade;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.jdbc.core.JdbcTemplate;

public class GenerateUpgradeSQL {

    private static final List<String> INIT_SQL_STATEMENTS = List.of(
            "ALTER TABLE PullPolicy RENAME TO InboundPolicy",
            "ALTER TABLE PullCorrelationRuleEntity RENAME TO InboundCorrelationRuleEntity",
            "ALTER TABLE ExternalResource RENAME COLUMN pullPolicy_id TO inboundPolicy_id",
            "ALTER TABLE InboundCorrelationRuleEntity RENAME COLUMN pullPolicy_id TO inboundPolicy_id");

    private static final String FINAL_SQL_STATEMENTS =
            """
            DROP TABLE IF EXISTS qrtz_blob_triggers CASCADE;
            DROP TABLE IF EXISTS qrtz_calendars CASCADE;
            DROP TABLE IF EXISTS qrtz_cron_triggers CASCADE;
            DROP TABLE IF EXISTS qrtz_fired_triggers CASCADE;
            DROP TABLE IF EXISTS qrtz_job_details CASCADE;
            DROP TABLE IF EXISTS qrtz_locks CASCADE;
            DROP TABLE IF EXISTS qrtz_paused_trigger_grps CASCADE;
            DROP TABLE IF EXISTS qrtz_scheduler_state CASCADE;
            DROP TABLE IF EXISTS qrtz_simple_triggers CASCADE;
            DROP TABLE IF EXISTS qrtz_simprop_triggers CASCADE;
            DROP TABLE IF EXISTS qrtz_triggers CASCADE;            
            """;

    private final JDBCConfiguration jdbcConf;

    private final JdbcTemplate jdbcTemplate;

    public GenerateUpgradeSQL(final JDBCConfiguration jdbcConf) {
        this.jdbcConf = jdbcConf;
        this.jdbcTemplate = new JdbcTemplate(jdbcConf.getDataSource2(null));
    }

    private String connInstances() {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> poolConfs = jdbcTemplate.queryForList(
                "SELECT id, maxidle, maxobjects, maxwait, minevictableidletimemillis, minidle FROM ConnInstance "
                + "WHERE maxidle IS NOT NULL OR maxobjects IS NOT NULL OR maxwait IS NOT NULL "
                + "OR minevictableidletimemillis IS NOT NULL OR minidle IS NOT NULL");

        poolConfs.forEach(poolConf -> {
            ConnPoolConf cpc = new ConnPoolConf();
            Optional.ofNullable(poolConf.get("maxidle")).ifPresent(v -> cpc.setMaxIdle((Integer) v));
            Optional.ofNullable(poolConf.get("maxobjects")).ifPresent(v -> cpc.setMaxObjects((Integer) v));
            Optional.ofNullable(poolConf.get("maxwait")).ifPresent(v -> cpc.setMaxWait((Long) v));
            Optional.ofNullable(poolConf.get("minevictableidletimemillis")).
                    ifPresent(v -> cpc.setMinEvictableIdleTimeMillis((Long) v));
            Optional.ofNullable(poolConf.get("minidle")).ifPresent(v -> cpc.setMinIdle((Integer) v));

            result.append(String.format(
                    "UPDATE ConnInstance SET poolConf='%s' WHERE id='%s';\n",
                    POJOHelper.serialize(cpc),
                    poolConf.get("id").toString()));
        });

        result.append("ALTER TABLE ConnInstance DROP COLUMN maxidle;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN maxobjects;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN maxwait;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN minevictableidletimemillis;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN minidle;\n");

        return result.toString();
    }

    private String resources() {
        StringBuilder result = new StringBuilder();

        result.append("ALTER TABLE ExternalResource DROP COLUMN overridecapabilities;\n");

        List<Map<String, Object>> accountPolicyResources = jdbcTemplate.queryForList(
                "SELECT accountpolicy_id, resource_id FROM AccountPolicy_ExternalResource");

        accountPolicyResources.forEach(acp -> {
            result.append(String.format(
                    "UPDATE ExternalResource SET accountPolicy_id='%s' WHERE id='%s';\n",
                    acp.get("accountpolicy_id").toString(),
                    acp.get("resource_id").toString()));
        });

        result.append("DROP TABLE AccountPolicy_ExternalResource;\n");

        return result.toString();
    }

    private String plainSchemas() {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> enumerations = jdbcTemplate.queryForList(
                "SELECT id, enumerationKeys, enumerationValues FROM PlainSchema "
                + "WHERE enumerationValues IS NOT NULL");

        enumerations.forEach(enumeration -> {
            String[] keys = enumeration.get("enumerationValues").toString().split(";");
            String[] values = Optional.ofNullable(enumeration.get("enumerationKeys")).
                    map(v -> v.toString().split(";")).
                    orElse(keys);

            Map<String, String> enumValues = new HashMap<>();
            for (int i = 0; i < keys.length; i++) {
                enumValues.put(keys[i], values.length > i ? values[i] : keys[i]);
            }

            result.append(String.format(
                    "UPDATE PlainSchema SET enumValues='%s' WHERE id='%s';\n",
                    POJOHelper.serialize(enumValues),
                    enumeration.get("id").toString()));
        });

        result.append("ALTER TABLE PlainSchema DROP COLUMN enumerationKeys;\n");
        result.append("ALTER TABLE PlainSchema DROP COLUMN enumerationValues;\n");

        return result.toString();
    }

    private String roles() {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> dynMembershipConds = jdbcTemplate.queryForList(
                "SELECT role_id AS id, fiql FROM DynRoleMembership");

        dynMembershipConds.forEach(cond -> {
            result.append(String.format(
                    "UPDATE SyncopeRole SET dynMembershipCond='%s' WHERE id='%s';\n",
                    cond.get("fiql").toString(),
                    cond.get("id").toString()));
        });

        result.append("DROP TABLE DynRoleMembership;\n");

        return result.toString();
    }

    public void run(final Writer out) throws IOException, SQLException {
        INIT_SQL_STATEMENTS.forEach(jdbcTemplate::execute);

        WiserSchemaTool schemaTool = new WiserSchemaTool(jdbcConf, SchemaTool.ACTION_ADD);
        schemaTool.setSchemaGroup(jdbcConf.getSchemaFactoryInstance().readSchema());
        schemaTool.setWriter(out);

        try (out) {
            // run OpenJPA's SchemaTool to get the update statements
            schemaTool.run();

            out.append(connInstances());
            out.append(resources());
            out.append(plainSchemas());
            out.append(roles());

            out.append(FINAL_SQL_STATEMENTS);
        }
    }
}
