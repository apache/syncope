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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class StreamPushJobDelegateTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    private SyncopeStreamPushExecutor executor;

    private SyncopeStreamPushExecutor executor() {
        synchronized (this) {
            if (executor == null) {
                executor = ApplicationContextProvider.getBeanFactory().createBean(StreamPushJobDelegate.class);
            }
        }
        return executor;
    }

    @Test
    public void push() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream os = new PipedOutputStream(in);

        PushTaskTO pushTask = new PushTaskTO();
        pushTask.setMatchingRule(MatchingRule.UPDATE);
        pushTask.setUnmatchingRule(UnmatchingRule.PROVISION);

        List<ProvisioningReport> results = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            try (CSVStreamConnector connector = new CSVStreamConnector(
                    null,
                    ";",
                    new CsvSchema.Builder().setUseHeader(true),
                    null,
                    os)) {

                return executor().push(
                        anyTypeDAO.getUser(),
                        userDAO.findAll(),
                        List.of("username", "firstname", "surname", "email", "status", "loginDate"),
                        connector,
                        List.of(),
                        pushTask,
                        "user");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(userDAO.count(), results.size());

        MappingIterator<Map<String, String>> reader =
                new CsvMapper().readerFor(Map.class).with(CsvSchema.emptySchema().withHeader()).readValues(in);

        for (int i = 0; i < results.size() && reader.hasNext(); i++) {
            Map<String, String> row = reader.next();

            assertEquals(results.get(i).getName(), row.get("username"));
            assertEquals(userDAO.findByUsername(row.get("username")).orElseThrow().getStatus(), row.get("status"));

            switch (row.get("username")) {
                case "rossini" -> {
                    assertEquals(StringUtils.EMPTY, row.get("email"));
                    assertTrue(row.get("loginDate").contains(";"));
                }

                case "verdi" -> {
                    assertEquals("verdi@syncope.org", row.get("email"));
                    assertEquals(StringUtils.EMPTY, row.get("loginDate"));
                }

                case "bellini" -> {
                    assertEquals(StringUtils.EMPTY, row.get("email"));
                    assertFalse(row.get("loginDate").contains(";"));
                }

                default -> {
                }
            }
        }
    }
}
