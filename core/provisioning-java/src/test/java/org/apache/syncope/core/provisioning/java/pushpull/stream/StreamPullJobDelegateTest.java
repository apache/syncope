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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class StreamPullJobDelegateTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    private SyncopeStreamPullExecutor executor;

    private SyncopeStreamPullExecutor executor() {
        synchronized (this) {
            if (executor == null) {
                executor = ApplicationContextProvider.getBeanFactory().createBean(StreamPullJobDelegate.class);
            }
        }
        return executor;
    }

    @Test
    public void pull() throws JobExecutionException, IOException {
        List<String> columns = List.of(
                "username",
                "email",
                "surname",
                "firstname",
                "fullname",
                "userId");

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", columns));
        csv.append('\n');
        csv.append("donizetti,");
        csv.append("donizetti@apache.org,");
        csv.append("Donizetti,");
        csv.append("Gaetano,");
        csv.append("Gaetano Donizetti,");
        csv.append("donizetti@apache.org");
        csv.append('\n');

        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setRemediation(false);
        pullTask.setMatchingRule(MatchingRule.UPDATE);
        pullTask.setUnmatchingRule(UnmatchingRule.PROVISION);

        List<ProvisioningReport> results = AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            try (CSVStreamConnector connector = new CSVStreamConnector(
                    "username",
                    ";",
                    new CsvSchema.Builder().setUseHeader(true),
                    new ByteArrayInputStream(csv.toString().getBytes()),
                    null)) {

                List<String> csvColumns = connector.getColumns(new CSVPullSpec());
                assertEquals(columns, csvColumns);

                return executor().pull(
                        anyTypeDAO.getUser(),
                        "username",
                        columns,
                        ConflictResolutionAction.IGNORE,
                        null,
                        connector,
                        pullTask,
                        "whoever");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(1, results.size());

        assertEquals(AnyTypeKind.USER.name(), results.getFirst().getAnyType());
        assertNotNull(results.getFirst().getKey());
        assertEquals("donizetti", results.getFirst().getName());
        assertEquals("donizetti", results.getFirst().getUidValue());
        assertEquals(ResourceOperation.CREATE, results.getFirst().getOperation());
        assertEquals(ProvisioningReport.Status.SUCCESS, results.getFirst().getStatus());

        User donizetti = userDAO.findById(results.getFirst().getKey()).orElseThrow();
        assertNotNull(donizetti);
        assertEquals("donizetti", donizetti.getUsername());
        assertEquals("Gaetano", donizetti.getPlainAttr("firstname").get().getValuesAsStrings().getFirst());
    }
}
