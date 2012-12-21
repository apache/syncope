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
package org.apache.syncope.client.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.apache.syncope.client.report.UserReportletConf;
import org.apache.syncope.client.search.AttributeCond;
import org.apache.syncope.client.search.MembershipCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.ReportTO;
import org.apache.syncope.client.to.SchemaTO;
import org.apache.syncope.client.to.WorkflowFormPropertyTO;
import org.apache.syncope.types.AuditElements;
import org.apache.syncope.types.AuditLoggerName;

public class JSONTest {

    @Test
    public void testSearchCondition() throws IOException {

        final AttributeCond usernameCond = new AttributeCond(AttributeCond.Type.LIKE);
        usernameCond.setSchema("username");
        usernameCond.setExpression("%o%");

        final MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleName("root");

        final NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getLeafCond(usernameCond), NodeCond.getLeafCond(
                membershipCond));

        assertTrue(searchCondition.isValid());

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, searchCondition);

        NodeCond actual = mapper.readValue(writer.toString(), NodeCond.class);
        assertEquals(searchCondition, actual);
    }

    @Test
    public void testLists() throws IOException {

        List<SchemaTO> schemas = new ArrayList<SchemaTO>();
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("name1");
        schemas.add(schemaTO);
        schemaTO = new SchemaTO();
        schemaTO.setName("name2");
        schemas.add(schemaTO);

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, schemas);

        List<SchemaTO> unserializedSchemas = Arrays.asList(mapper.readValue(writer.toString(), SchemaTO[].class));
        for (SchemaTO unserializedSchema : unserializedSchemas) {
            assertNotNull(unserializedSchema);
        }
    }

    @Test
    public void testMap() throws IOException {

        WorkflowFormPropertyTO prop = new WorkflowFormPropertyTO();
        prop.putEnumValue("key1", "value1");
        prop.putEnumValue("key2", "value2");

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, prop);

        WorkflowFormPropertyTO unserializedProp = mapper.readValue(writer.toString(), WorkflowFormPropertyTO.class);
        assertEquals(prop, unserializedProp);
    }

    @Test
    public void testReportletConfImplementations() throws IOException {

        ReportTO report = new ReportTO();
        report.setName("testReportForCreate");
        report.addReportletConf(new UserReportletConf("first"));
        report.addReportletConf(new UserReportletConf("second"));

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, report);

        ReportTO actual = mapper.readValue(writer.toString(), ReportTO.class);
        assertEquals(report, actual);
    }

    @Test
    public void testAuditLoggerName() throws IOException {
        AuditLoggerName auditLoggerName = new AuditLoggerName(AuditElements.Category.report,
                AuditElements.ReportSubCategory.create, AuditElements.Result.failure);

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, auditLoggerName);

        AuditLoggerName actual = mapper.readValue(writer.toString(), AuditLoggerName.class);
        assertEquals(auditLoggerName, actual);
    }
}
