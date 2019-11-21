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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = {
    "classpath:testJDBCEnv.xml"
})
public class GeneratedUpgradeSQLTest {

    @Resource(name = "driverClassName")
    private String driverClassName;

    @Resource(name = "jdbcURL")
    private String jdbcURL;

    @Resource(name = "username")
    private String username;

    @Resource(name = "password")
    private String password;

    @Autowired
    private DataSource syncope20DataSource;

    private static final String TEST_PULL_RULE = "org.apache.syncope.fit.core.reference.TestPullRuleConf";

    @Test
    public void upgradefrom20() throws Exception {
        StringWriter out = new StringWriter();
        GenerateUpgradeSQL.setWriter(out);

        String[] args = new String[] { driverClassName, jdbcURL, username, password, "h2" };
        GenerateUpgradeSQL.main(args);

        String upgradeSQL = out.toString();

        try {
            DataSourceInitializer adminUsersInit = new DataSourceInitializer();
            adminUsersInit.setDataSource(syncope20DataSource);
            adminUsersInit.setDatabasePopulator(
                    new ResourceDatabasePopulator(new ByteArrayResource(upgradeSQL.getBytes(StandardCharsets.UTF_8))));
            adminUsersInit.afterPropertiesSet();
        } catch (Exception e) {
            fail("Unexpected error while upgrading", e);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(syncope20DataSource);

        // check that custom JAVA PullCorrelationRule(s) have been moved to correct implementation with PullCorrelationRuleConf
        List<Map<String, Object>> implementations = jdbcTemplate.queryForList("SELECT * FROM Implementation");
        assertNotNull(implementations);
        assertEquals(16, implementations.size());
        assertTrue(implementations.stream()
                .anyMatch(impl -> {
                    try {
                        ObjectNode body = (ObjectNode) new ObjectMapper().readTree(impl.get("body").toString());
                        return "PULL_CORRELATION_RULE".equals(impl.get("type"))
                                && TEST_PULL_RULE.equals(body.get("@class").asText())
                                && TEST_PULL_RULE.equals(body.get("name").asText());
                    } catch (JsonProcessingException e) {
                    }
                    return false;
                }));

        Integer pullTaskActions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PullTaskAction", Integer.class);
        assertNotNull(pullTaskActions);
        assertEquals(1, pullTaskActions.intValue());

        Integer pushTaskActions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PushTaskAction", Integer.class);
        assertNotNull(pushTaskActions);
        assertEquals(0, pushTaskActions.intValue());

        Integer propagationTaskActions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ExternalResourcePropAction", Integer.class);
        assertNotNull(propagationTaskActions);
        assertEquals(1, propagationTaskActions.intValue());

        Integer realmActions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM RealmAction", Integer.class);
        assertNotNull(realmActions);
        assertEquals(0, realmActions.intValue());

        Integer accountPolicyRules = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AccountPolicyRule", Integer.class);
        assertNotNull(accountPolicyRules);
        assertEquals(2, accountPolicyRules.intValue());

        Integer passwordPolicyRules = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PasswordPolicyRule", Integer.class);
        assertNotNull(passwordPolicyRules);
        assertEquals(3, passwordPolicyRules.intValue());

        Integer pullCorrelationRuleEntities = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PullCorrelationRuleEntity", Integer.class);
        assertNotNull(pullCorrelationRuleEntities);
        assertEquals(2, pullCorrelationRuleEntities.intValue());

        Integer pushCorrelationRuleEntities = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PushCorrelationRuleEntity", Integer.class);
        assertNotNull(pushCorrelationRuleEntities);
        assertEquals(0, pushCorrelationRuleEntities.intValue());
    }

    @Test
    public void upgradeFlowableTo212() throws Exception {
        StringWriter out = new StringWriter();
        GenerateUpgradeSQL.setWriter(out);

        String[] args = new String[] { driverClassName, jdbcURL, username, password, "h2", "-flowable-2.1.2" };
        GenerateUpgradeSQL.main(args);

        String upgradeSQL = out.toString();

        try {
            DataSourceInitializer adminUsersInit = new DataSourceInitializer();
            adminUsersInit.setDataSource(syncope20DataSource);
            adminUsersInit.setDatabasePopulator(
                    new ResourceDatabasePopulator(new ByteArrayResource(upgradeSQL.getBytes(StandardCharsets.UTF_8))));
            adminUsersInit.afterPropertiesSet();
        } catch (Exception e) {
            fail("Unexpected error while upgrading Flowable to 2.1.2", e);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(syncope20DataSource);

        Integer processInstances = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ACT_RU_EXECUTION WHERE BUSINESS_KEY_ IS NOT NULL", Integer.class);
        assertNotNull(processInstances);
        assertEquals(5, processInstances.intValue());
    }
}
