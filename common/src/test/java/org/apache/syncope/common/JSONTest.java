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
package org.apache.syncope.common;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.syncope.common.report.UserReportletConf;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.junit.Test;

public class JSONTest {

    @Test
    public void map() throws IOException {
        WorkflowFormPropertyTO prop = new WorkflowFormPropertyTO();
        prop.getEnumValues().put("key1", "value1");
        prop.getEnumValues().put("key2", "value2");

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, prop);

        WorkflowFormPropertyTO unserializedProp = mapper.readValue(writer.toString(), WorkflowFormPropertyTO.class);
        assertEquals(prop, unserializedProp);
    }

    @Test
    public void reportletConfImplementations() throws IOException {
        ReportTO report = new ReportTO();
        report.setName("testReportForCreate");
        report.getReportletConfs().add(new UserReportletConf("first"));
        report.getReportletConfs().add(new UserReportletConf("second"));

        ObjectMapper mapper = new ObjectMapper();

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, report);

        ReportTO actual = mapper.readValue(writer.toString(), ReportTO.class);
        assertEquals(report, actual);
    }
}
