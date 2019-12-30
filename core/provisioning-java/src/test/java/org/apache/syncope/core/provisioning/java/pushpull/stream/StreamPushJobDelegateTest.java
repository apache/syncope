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
package org.apache.syncope.core.provisioning.java.pushpull.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.pushpull.stream.StreamConnector;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class StreamPushJobDelegateTest extends AbstractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private SyncopeStreamPushExecutor streamPushExecutor;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void push() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream os = new PipedOutputStream(in);

        PushTaskTO pushTask = new PushTaskTO();
        pushTask.setMatchingRule(MatchingRule.UPDATE);
        pushTask.setUnmatchingRule(UnmatchingRule.PROVISION);

        List<ProvisioningReport> results = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            try (SequenceWriter writer = MAPPER.writer().forType(Map.class).writeValues(os)) {
                writer.init(true);

                return streamPushExecutor.push(
                        anyTypeDAO.findUser(),
                        userDAO.findAll(1, 100),
                        Arrays.asList("username", "firstname", "surname", "email", "status", "loginDate"),
                        new StreamConnector(null, null, null, writer),
                        pushTask,
                        "user");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(userDAO.count(), results.size());

        MappingIterator<Map<String, String>> reader = MAPPER.readerFor(Map.class).readValues(in);

        for (int i = 0; i < results.size() && reader.hasNext(); i++) {
            Map<String, String> row = reader.next();

            assertEquals(results.get(i).getName(), row.get("username"));
            assertEquals(userDAO.findByUsername(row.get("username")).getStatus(), row.get("status"));

            switch (row.get("username")) {
                case "rossini":
                    assertNull(row.get("email"));
                    assertTrue(row.get("loginDate").contains(","));
                    break;

                case "verdi":
                    assertEquals("verdi@syncope.org", row.get("email"));
                    assertNull(row.get("loginDate"));
                    break;

                case "bellini":
                    assertNull(row.get("email"));
                    assertFalse(row.get("loginDate").contains(","));
                    break;

                default:
                    break;
            }
        }
    }
}
