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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.springframework.jdbc.core.JdbcTemplate;

public class GenerateUpgradeSQL {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static final String INIT_SQL_STATEMENTS =
            """
            INSERT INTO InboundPolicy SELECT * FROM PullPolicy;
            UPDATE ExternalResource SET inboundPolicy_id=pullPolicy_id;
            ALTER TABLE ExternalResource DROP COLUMN pullPolicy_id;

            INSERT INTO InboundCorrelationRuleEntity(id, inboundPolicy_id, anyType_id, implementation_id) 
            SELECT id, pullPolicy_id, anyType_id, implementation_id FROM PullCorrelationRuleEntity;

            DROP TABLE PullCorrelationRuleEntity;
            DROP TABLE PullPolicy;
            """;

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

    private String connInstances() throws JsonProcessingException {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> poolConfs = jdbcTemplate.queryForList(
                "SELECT id, maxIdle, maxObjects, maxWait, minEvictableIdleTimeMillis, minIdle FROM ConnInstance "
                + "WHERE maxidle IS NOT NULL OR maxobjects IS NOT NULL OR maxWait IS NOT NULL "
                + "OR minEvictableIdleTimeMillis IS NOT NULL OR minIdle IS NOT NULL");

        for (Map<String, Object> poolConf : poolConfs) {
            ObjectNode cpc = MAPPER.createObjectNode();
            Optional.ofNullable(poolConf.get("maxIdle")).ifPresent(v -> cpc.put("maxIdle", (Integer) v));
            Optional.ofNullable(poolConf.get("maxObjects")).ifPresent(v -> cpc.put("maxObjects", (Integer) v));
            Optional.ofNullable(poolConf.get("maxWait")).ifPresent(v -> cpc.put("maxWait", (Long) v));
            Optional.ofNullable(poolConf.get("minEvictableIdleTimeMillis")).
                    ifPresent(v -> cpc.put("minEvictableIdleTimeMillis", (Long) v));
            Optional.ofNullable(poolConf.get("minIdle")).ifPresent(v -> cpc.put("minIdle", (Integer) v));

            result.append(String.format(
                    "UPDATE ConnInstance SET poolConf='%s' WHERE id='%s';\n",
                    MAPPER.writeValueAsString(cpc),
                    poolConf.get("id").toString()));
        }

        result.append("ALTER TABLE ConnInstance DROP COLUMN maxidle;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN maxobjects;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN maxwait;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN minevictableidletimemillis;\n");
        result.append("ALTER TABLE ConnInstance DROP COLUMN minidle;\n");

        return result.toString();
    }

    private String resources() throws JsonProcessingException {
        StringBuilder result = new StringBuilder();

        result.append("ALTER TABLE ExternalResource DROP COLUMN overridecapabilities;\n");

        List<Map<String, Object>> resources = jdbcTemplate.queryForList(
                "SELECT id, provisions FROM ExternalResource WHERE provisions IS NOT NULL");
        for (Map<String, Object> resource : resources) {
            JsonNode provisions = MAPPER.readTree(resource.get("provisions").toString());
            for (JsonNode provision : provisions) {
                if (provision.has("virSchemas")) {
                    ((ObjectNode) provision).remove("virSchemas");
                }
                if (provision.has("mapping") && provision.get("mapping").has("linkingItems")) {
                    ((ObjectNode) provision.get("mapping")).remove("linkingItems");
                }
            }

            result.append(String.format(
                    "UPDATE ExternalResource SET provisions='%s' WHERE id='%s';\n",
                    MAPPER.writeValueAsString(provisions).replace("'", "''"),
                    resource.get("id").toString()));
        }

        List<Map<String, Object>> accountPolicyResources = jdbcTemplate.queryForList(
                "SELECT accountpolicy_id, resource_id FROM AccountPolicy_ExternalResource");
        accountPolicyResources.forEach(acp -> result.append(String.format(
                "UPDATE ExternalResource SET accountPolicy_id='%s' WHERE id='%s';\n",
                acp.get("accountpolicy_id").toString(),
                acp.get("resource_id").toString())));

        result.append("DROP TABLE AccountPolicy_ExternalResource;\n");

        return result.toString();
    }

    private String plainSchemas() throws JsonProcessingException {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> enumerations = jdbcTemplate.queryForList(
                "SELECT id, enumerationKeys, enumerationValues FROM PlainSchema "
                + "WHERE enumerationValues IS NOT NULL");

        for (Map<String, Object> enumeration : enumerations) {
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
                    MAPPER.writeValueAsString(enumValues),
                    enumeration.get("id").toString()));
        }

        result.append("ALTER TABLE PlainSchema DROP COLUMN enumerationKeys;\n");
        result.append("ALTER TABLE PlainSchema DROP COLUMN enumerationValues;\n");

        return result.toString();
    }

    private String roles() throws JsonProcessingException {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> dynMembershipConds = jdbcTemplate.queryForList(
                "SELECT role_id AS id, fiql FROM DynRoleMembership");

        dynMembershipConds.forEach(cond -> result.append(String.format(
                "UPDATE SyncopeRole SET dynMembershipCond='%s' WHERE id='%s';\n",
                cond.get("fiql").toString(),
                cond.get("id").toString())));

        result.append("DROP TABLE DynRoleMembership;\n");

        List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                "SELECT id, anyLayout from SyncopeRole WHERE anyLayout IS NOT NULL");

        for (Map<String, Object> role : roles) {
            JsonNode anyLayout = MAPPER.readTree(role.get("anyLayout").toString());
            for (JsonNode child : anyLayout) {
                if (child.isObject()) {
                    if (child.has("virAttrs")) {
                        ((ObjectNode) child).remove("virAttrs");
                    }
                    if (child.has("whichVirAttrs")) {
                        ((ObjectNode) child).remove("whichVirAttrs");
                    }
                }
            }

            result.append(String.format(
                    "UPDATE SyncopeRole SET anyLayout='%s' WHERE id='%s';\n",
                    MAPPER.writeValueAsString(anyLayout).replace("'", "''"),
                    role.get("id").toString()));
        }

        return result.toString();
    }

    private String relationshipTypes() {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> relationshipTypes = jdbcTemplate.queryForList("SELECT id FROM RelationshipType");

        jdbcTemplate.setMaxRows(1);
        relationshipTypes.forEach(relationshipType -> {
            List<Map<String, Object>> anyObjects = jdbcTemplate.queryForList(
                    "SELECT anyobject_id FROM URelationship WHERE type_id=?",
                    relationshipType.get("id"));
            if (anyObjects.isEmpty()) {
                anyObjects = jdbcTemplate.queryForList(
                        "SELECT left_anyobject_id, right_anyobject_id FROM ARelationship WHERE type_id=?",
                        relationshipType.get("id"));
                if (!anyObjects.isEmpty()) {
                    String leftEndAnyType = jdbcTemplate.queryForObject(
                            "SELECT type_id from AnyObject WHERE id=?",
                            String.class,
                            anyObjects.getFirst().get("left_anyobject_id"));
                    String rightEndAnyType = jdbcTemplate.queryForObject(
                            "SELECT type_id from AnyObject WHERE id=?",
                            String.class,
                            anyObjects.getFirst().get("right_anyobject_id"));

                    result.append("UPDATE RelationshipType ").
                            append("SET leftEndAnyType_id='").append(leftEndAnyType).append("', ").
                            append("rightEndAnyType_id='").append(rightEndAnyType).append("' ").
                            append("WHERE id='").append(relationshipType.get("id")).append("';\n");
                }
            } else {
                String rightEndAnyType = jdbcTemplate.queryForObject(
                        "SELECT type_id from AnyObject WHERE id=?",
                        String.class,
                        anyObjects.getFirst().get("anyobject_id"));

                result.append("UPDATE RelationshipType ").
                        append("SET leftEndAnyType_id='USER', ").
                        append("rightEndAnyType_id='").append(rightEndAnyType).append("' ").
                        append("WHERE id='").append(relationshipType.get("id")).append("';\n");
            }
        });
        jdbcTemplate.setMaxRows(-1);

        result.append("UPDATE RelationshipType ").
                append("SET leftEndAnyType_id='USER', rightEndAnyType_id='USER' ").
                append("WHERE leftEndAnyType_id IS NULL AND rightEndAnyType_id IS NULL;\n");

        return result.toString();
    }

    private String implementations() {
        StringBuilder result = new StringBuilder();

        result.append("UPDATE Implementation ").
                append("SET type='INBOUND_ACTIONS' WHERE type='PULL_ACTIONS';\n");
        result.append("UPDATE Implementation ").
                append("SET type='INBOUND_CORRELATION_RULE' WHERE type='PULL_CORRELATION_RULE';\n");

        List<Map<String, Object>> implementations = jdbcTemplate.queryForList(
                "SELECT id, body from Implementation "
                + "WHERE body LIKE 'org.apache.syncope.core.persistence.jpa.attrvalue.validation.%'");

        implementations.forEach(implementation -> result.append(String.format(
                "UPDATE Implementation SET body='%s' WHERE id='%s';\n",
                implementation.get("body").toString().replace(
                        "org.apache.syncope.core.persistence.jpa.attrvalue.validation.",
                        "org.apache.syncope.core.persistence.common.attrvalue."),
                implementation.get("id").toString())));

        return result.toString();
    }

    private String anyTemplates() throws JsonProcessingException {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> templates = jdbcTemplate.queryForList(
                "SELECT id, template from AnyTemplateRealm");

        for (Map<String, Object> template : templates) {
            JsonNode t = MAPPER.readTree(template.get("template").toString());
            if (t.has("virAttrs")) {
                ((ObjectNode) t).remove("virAttrs");

                result.append(String.format(
                        "UPDATE AnyTemplateRealm SET template='%s' WHERE id='%s';\n",
                        MAPPER.writeValueAsString(t).replace("'", "''"),
                        template.get("id").toString()));
            }
        }

        templates = jdbcTemplate.queryForList(
                "SELECT id, template from AnyTemplatePullTask");

        for (Map<String, Object> template : templates) {
            JsonNode t = MAPPER.readTree(template.get("template").toString());
            if (t.has("virAttrs")) {
                ((ObjectNode) t).remove("virAttrs");

                result.append(String.format(
                        "UPDATE AnyTemplatePullTask SET template='%s' WHERE id='%s';\n",
                        MAPPER.writeValueAsString(t).replace("'", "''"),
                        template.get("id").toString()));
            }
        }

        return result.toString();
    }

    private String audit() {
        StringBuilder result = new StringBuilder();

        List<Map<String, Object>> auditConf = jdbcTemplate.queryForList(
                "SELECT id from AuditConf");

        auditConf.forEach(conf -> result.append(String.format(
                "UPDATE AuditConf SET id='%s' WHERE id='%s';\n",
                conf.get("id").toString().replace("syncope.audit.", ""),
                conf.get("id").toString())));

        return result.toString();
    }

    public void run(final Writer out) throws IOException, SQLException {
        WiserSchemaTool schemaTool = new WiserSchemaTool(jdbcConf, SchemaTool.ACTION_ADD);
        schemaTool.setSchemaGroup(jdbcConf.getSchemaFactoryInstance().readSchema());
        schemaTool.setWriter(out);

        try (out) {
            // run OpenJPA's SchemaTool to get the update statements
            schemaTool.run();

            out.append('\n').append(INIT_SQL_STATEMENTS).append('\n');

            out.append(connInstances()).append('\n');
            out.append(resources()).append('\n');
            out.append(plainSchemas()).append('\n');
            out.append(roles()).append('\n');
            out.append(relationshipTypes()).append('\n');
            out.append(implementations()).append('\n');
            out.append(anyTemplates()).append('\n');
            out.append(audit()).append('\n');

            out.append(FINAL_SQL_STATEMENTS);
        }
    }
}
