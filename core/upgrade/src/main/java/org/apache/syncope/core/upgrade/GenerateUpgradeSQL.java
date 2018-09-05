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
package org.apache.syncope.core.upgrade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.schema.FileSchemaFactory;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.jdbc.schema.SimpleDriverDataSource;

public final class GenerateUpgradeSQL {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Writer OUT = new PrintWriter(System.out);

    public static void setWriter(final Writer out) {
        GenerateUpgradeSQL.OUT = out;
    }

    public static void main(final String[] args) throws Exception {
        // parse args
        if (args.length < 5 || args.length > 6) {
            System.err.println("Unexpected arguments: " + Arrays.asList(args));
            System.out.println("Usage: <driverClassName> <jdbcURL> <username> <password>"
                    + "<h2|mariadb|mysql|oracle|postgres|sqlserver> [filename]");
            System.exit(1);
        }

        String driverClassName = args[0];
        String jdbcURL = args[1];
        String username = args[2];
        String password = args[3];
        String dbDictionary = args[4];
        if (args.length == 6) {
            setWriter(new FileWriter(args[5]));
        }

        // setup DataSource
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setConnectionDriverName(driverClassName);
        dataSource.setConnectionURL(jdbcURL);
        dataSource.setConnectionUserName(username);
        dataSource.setConnectionPassword(password);

        // setup OpenJPA
        JDBCConfiguration jdbcConf = new JDBCConfigurationImpl();
        jdbcConf.setConnection2DriverName(driverClassName);
        jdbcConf.setConnection2UserName(username);
        jdbcConf.setConnection2Password(password);
        jdbcConf.setDBDictionary(dbDictionary);
        jdbcConf.setConnectionFactory2(dataSource);

        FileSchemaFactory schemaFactory = new FileSchemaFactory();
        schemaFactory.setConfiguration(jdbcConf);
        schemaFactory.setFile("schema.xml");
        jdbcConf.setSchemaFactory(schemaFactory);

        WiserSchemaTool schemaTool = new WiserSchemaTool(jdbcConf, SchemaTool.ACTION_ADD);
        schemaTool.setSchemaGroup(schemaFactory.readSchema());
        schemaTool.setWriter(OUT);
        try {
            // run OpenJPA's SchemaTool to get the update statements
            schemaTool.run();

            // now proceed with manual update statements...
            Connection conn = jdbcConf.getDataSource2(null).getConnection();

            // User
            OUT.write("UPDATE SyncopeUser SET mustChangePassword=0 WHERE mustChangePassword IS NULL;\n");

            // VirSchema
            OUT.write("UPDATE VirSchema SET readonly=0 WHERE readonly IS NULL;\n");

            // ExternalResource
            OUT.write("UPDATE ExternalResource SET overrideCapabilities=0 WHERE overrideCapabilities IS NULL;\n");

            // OrgUnit
            OUT.write("UPDATE OrgUnit SET ignoreCaseMatch=0;\n");

            // OrgUnitItemTransformer
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT orgUnitItem_id,transformerClassName FROM OrgUnitItem_Transformer")) {

                while (rs.next()) {
                    String itemId = rs.getString(1);
                    String transformerClassName = rs.getString(2);

                    String implementationId = "OrgUnitItemTransformer_" + transformerClassName + "_" + itemId;
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + implementationId + "',"
                            + "'ITEM_TRANSFORMER',"
                            + "'JAVA',"
                            + "'" + transformerClassName + "');\n");
                    OUT.write("INSERT INTO OrgUnitItemTransformer(item_id,implementation_id) VALUES("
                            + "'" + itemId + "',"
                            + "'" + implementationId + "');\n");
                }
            }
            OUT.write("DROP TABLE OrgUnitItem_Transformer;\n");

            // PlainSchema
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT validatorClass FROM PlainSchema WHERE validatorClass IS NOT NULL")) {

                while (rs.next()) {
                    String validatorClass = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + validatorClass + "',"
                            + "'VALIDATOR',"
                            + "'JAVA',"
                            + "'" + validatorClass + "');\n");
                }
            }
            OUT.write("UPDATE PlainSchema SET validator_id=validatorClass;\n");
            OUT.write("ALTER TABLE PlainSchema DROP COLUMN validatorClass;\n");

            // Provision
            OUT.write("UPDATE Provision SET ignoreCaseMatch=0;\n");

            // PullPolicy
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT id,specification FROM PullPolicy WHERE specification IS NOT NULL")) {

                while (rs.next()) {
                    String id = rs.getString(1);
                    ObjectNode specification = (ObjectNode) MAPPER.readTree(rs.getString(2));

                    if (specification.has("conflictResolutionAction")) {
                        OUT.write("UPDATE PullPolicy SET "
                                + "conflictResolutionAction='"
                                + specification.get("conflictResolutionAction").asText() + "' "
                                + "WHERE id='" + id + "';\n");
                    }
                    if (specification.has("correlationRules")) {
                        specification.get("correlationRules").fields().forEachRemaining(entry -> {
                            ObjectNode body = MAPPER.createObjectNode();
                            body.put("@class", "org.apache.syncope.common.lib.policy.DefaultPullCorrelationRuleConf");
                            body.put("name", "org.apache.syncope.common.lib.policy.DefaultPullCorrelationRuleConf");
                            body.set("schemas", entry.getValue());

                            try {
                                String implementationId = "PullCorrelationRule_" + entry.getKey() + "_" + id;
                                OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                                        + "'" + implementationId + "',"
                                        + "'PULL_CORRELATION_RULE',"
                                        + "'JAVA',"
                                        + "'" + MAPPER.writeValueAsString(body) + "');\n");

                                OUT.write("INSERT INTO PullCorrelationRuleEntity"
                                        + "(id,pullPolicy_id,anyType_id,implementation_id) VALUES("
                                        + "'" + UUID.randomUUID().toString() + "',"
                                        + "'" + id + "',"
                                        + "'" + entry.getKey() + "',"
                                        + "'" + implementationId + "');\n");
                            } catch (IOException e) {
                                System.err.println("Unexpected error: " + e.getMessage());
                                System.exit(2);
                            }
                        });
                    }
                }
                OUT.write("ALTER TABLE PullPolicy DROP COLUMN specification;\n");
            }

            // AccountPolicy
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT id,accountPolicy_id,serializedInstance FROM AccountRuleConfInstance")) {

                while (rs.next()) {
                    String id = rs.getString(1);
                    String accountPolicyId = rs.getString(2);
                    String serializedInstance = rs.getString(3);

                    String implementationId = "AccountRule_" + accountPolicyId + "_" + id;
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + implementationId + "',"
                            + "'ACCOUNT_RULE',"
                            + "'JAVA',"
                            + "'" + serializedInstance + "');\n");
                    OUT.write("INSERT INTO AccountPolicyRule(policy_id,implementation_id) VALUES("
                            + "'" + accountPolicyId + "',"
                            + "'" + implementationId + "');\n");
                }
            }
            OUT.write("DROP TABLE AccountRuleConfInstance;\n");

            // PasswordPolicy
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT id,passwordPolicy_id,serializedInstance FROM PasswordRuleConfInstance")) {

                while (rs.next()) {
                    String id = rs.getString(1);
                    String passwordPolicyId = rs.getString(2);
                    String serializedInstance = rs.getString(3);

                    String implementationId = "PasswordRule_" + passwordPolicyId + "_" + id;
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + implementationId + "',"
                            + "'ACCOUNT_RULE',"
                            + "'JAVA',"
                            + "'" + serializedInstance + "');\n");
                    OUT.write("INSERT INTO PasswordPolicyRule(policy_id,implementation_id) VALUES("
                            + "'" + passwordPolicyId + "',"
                            + "'" + implementationId + "');\n");
                }
            }
            OUT.write("DROP TABLE PasswordRuleConfInstance;\n");

            // Task
            OUT.write("UPDATE Task SET remediation=0;\n");
            OUT.write("UPDATE Task SET active=0 WHERE active IS NULL;\n");

            OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                    + "'PullJobDelegate',"
                    + "'TASKJOB_DELEGATE',"
                    + "'JAVA',"
                    + "'org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate');\n");
            OUT.write("UPDATE Task SET jobDelegate_id='PullJobDelegate' WHERE DTYPE='PullTask';\n");

            OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                    + "'PushJobDelegate',"
                    + "'TASKJOB_DELEGATE',"
                    + "'JAVA',"
                    + "'org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate');\n");
            OUT.write("UPDATE Task SET jobDelegate_id='PushJobDelegate' WHERE DTYPE='PushTask';\n");

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT jobDelegateClassName FROM Task WHERE jobDelegateClassName IS NOT NULL")) {

                while (rs.next()) {
                    String jobDelegateClassName = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + jobDelegateClassName + "',"
                            + "'TASKJOB_DELEGATE',"
                            + "'JAVA',"
                            + "'" + jobDelegateClassName + "');\n");
                }
            }
            OUT.write("UPDATE Task SET jobDelegate_id=jobDelegateClassName;\n");
            OUT.write("ALTER TABLE Task DROP COLUMN jobDelegateClassName;\n");

            // PullActions
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT actionClassName FROM PullTask_actionsClassNames")) {

                while (rs.next()) {
                    String actionClassName = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + actionClassName + "',"
                            + "'PULL_ACTIONS',"
                            + "'JAVA',"
                            + "'" + actionClassName + "');\n");
                }
            }
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT pullTask_id,actionClassName FROM PullTask_actionsClassNames")) {

                while (rs.next()) {
                    String pullTaskId = rs.getString(1);
                    String actionClassName = rs.getString(2);
                    OUT.write("INSERT INTO PullTaskAction(task_id,implementation_id) VALUES("
                            + "'" + pullTaskId + "',"
                            + "'" + actionClassName + "');\n");
                }
            }
            OUT.write("DROP TABLE PullTask_actionsClassNames;\n");

            // PushActions
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT actionClassName FROM PushTask_actionsClassNames")) {

                while (rs.next()) {
                    String actionClassName = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + actionClassName + "',"
                            + "'PULL_ACTIONS',"
                            + "'JAVA',"
                            + "'" + actionClassName + "');\n");
                }
            }
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT pushTask_id,actionClassName FROM PushTask_actionsClassNames")) {

                while (rs.next()) {
                    String pushTaskId = rs.getString(1);
                    String actionClassName = rs.getString(2);
                    OUT.write("INSERT INTO PushTaskAction(task_id,implementation_id) VALUES("
                            + "'" + pushTaskId + "',"
                            + "'" + actionClassName + "');\n");
                }
            }
            OUT.write("DROP TABLE PushTask_actionsClassNames;\n");

            // PropagationActions
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT actionClassName FROM ExternalResource_PropActions")) {

                while (rs.next()) {
                    String actionClassName = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + actionClassName + "',"
                            + "'PROPAGATION_ACTIONS',"
                            + "'JAVA',"
                            + "'" + actionClassName + "');\n");
                }
            }
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT resource_id,actionClassName FROM ExternalResource_PropActions")) {

                while (rs.next()) {
                    String resourceId = rs.getString(1);
                    String actionClassName = rs.getString(2);
                    OUT.write("INSERT INTO ExternalResourcePropAction(resource_id,implementation_id) VALUES("
                            + "'" + resourceId + "',"
                            + "'" + actionClassName + "');\n");
                }
            }
            OUT.write("DROP TABLE ExternalResource_PropActions;\n");

            // LogicActions
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT actionClassName FROM Realm_actionsClassNames")) {

                while (rs.next()) {
                    String actionClassName = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + actionClassName + "',"
                            + "'PULL_ACTIONS',"
                            + "'JAVA',"
                            + "'" + actionClassName + "');\n");
                }
            }
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT realm_id,actionClassName FROM Realm_actionsClassNames")) {

                while (rs.next()) {
                    String realmId = rs.getString(1);
                    String actionClassName = rs.getString(2);
                    OUT.write("INSERT INTO RealmAction(realm_id,implementation_id) VALUES("
                            + "'" + realmId + "',"
                            + "'" + actionClassName + "');\n");
                }
            }
            OUT.write("DROP TABLE Realm_actionsClassNames;\n");

            // Reportlet
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT id,report_id,serializedInstance FROM ReportletConfInstance")) {

                while (rs.next()) {
                    String id = rs.getString(1);
                    String reportId = rs.getString(2);
                    String serializedInstance = rs.getString(3);

                    String implementationId = "Reportlet_" + reportId + "_" + id;
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + implementationId + "',"
                            + "'REPORTLET',"
                            + "'JAVA',"
                            + "'" + serializedInstance + "');\n");
                    OUT.write("INSERT INTO ReportReportlet(report_id,implementation_id) VALUES("
                            + "'" + reportId + "',"
                            + "'" + implementationId + "');\n");
                }
            }
            OUT.write("DROP TABLE ReportletConfInstance;\n");

            // MappingItemTransformer
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT mappingItem_id,transformerClassName FROM MappingItem_Transformer")) {

                while (rs.next()) {
                    String itemId = rs.getString(1);
                    String transformerClassName = rs.getString(2);

                    String implementationId = "MappingItemTransformer_" + transformerClassName + "_" + itemId;
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + implementationId + "',"
                            + "'ITEM_TRANSFORMER',"
                            + "'JAVA',"
                            + "'" + transformerClassName + "');\n");
                    OUT.write("INSERT INTO MappingItemTransformer(item_id,implementation_id) VALUES("
                            + "'" + itemId + "',"
                            + "'" + implementationId + "');\n");
                }
            }
            OUT.write("DROP TABLE MappingItem_Transformer;\n");

            // Notification
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT DISTINCT recipientsProviderClassName "
                            + "FROM Notification WHERE recipientsProviderClassName IS NOT NULL")) {

                while (rs.next()) {
                    String recipientsProviderClassName = rs.getString(1);
                    OUT.write("INSERT INTO Implementation(id,type,engine,body) VALUES("
                            + "'" + recipientsProviderClassName + "',"
                            + "'RECIPIENTS_PROVIDER',"
                            + "'JAVA',"
                            + "'" + recipientsProviderClassName + "');\n");
                }
            }
            OUT.write("UPDATE Notification SET recipientsProvider_id=recipientsProviderClassName;\n");
            OUT.write("ALTER TABLE Notification DROP COLUMN recipientsProviderClassName;\n");
        } finally {
            OUT.flush();
            OUT.close();
        }
    }

    private GenerateUpgradeSQL() {
        // private constructor
    }
}
