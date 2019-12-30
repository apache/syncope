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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.MappingIterator;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.stream.StreamConnector;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class StreamPullJobDelegateTest extends AbstractTest {

    @Autowired
    private SyncopeStreamPullExecutor streamPullExecutor;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void pull() throws JobExecutionException, IOException {
        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setRemediation(false);
        pullTask.setMatchingRule(MatchingRule.UPDATE);
        pullTask.setUnmatchingRule(UnmatchingRule.PROVISION);

        Map<String, String> user = new HashMap<>();
        user.put("username", "donizetti");
        user.put("email", "donizetti@apache.org");
        user.put("surname", "Donizetti");
        user.put("firstname", "Gaetano");
        user.put("fullname", "Gaetano Donizetti");
        user.put("userId", "donizetti@apache.org");
        Iterator<Map<String, String>> backing = Collections.singletonList(user).iterator();

        @SuppressWarnings("unchecked")
        MappingIterator<Map<String, String>> itor = mock(MappingIterator.class);
        when(itor.hasNext()).thenAnswer(invocation -> backing.hasNext());
        when(itor.next()).thenAnswer(invocation -> backing.next());

        List<String> columns = user.keySet().stream().collect(Collectors.toList());

        List<ProvisioningReport> results = AuthContextUtils.execWithAuthContext(SyncopeConstants.MASTER_DOMAIN, () -> {
            try {
                return streamPullExecutor.pull(
                        anyTypeDAO.findUser(),
                        "username",
                        columns,
                        ConflictResolutionAction.IGNORE,
                        null,
                        new StreamConnector("username", null, itor, null),
                        pullTask);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(1, results.size());

        assertEquals(AnyTypeKind.USER.name(), results.get(0).getAnyType());
        assertNotNull(results.get(0).getKey());
        assertEquals("donizetti", results.get(0).getName());
        assertEquals("donizetti", results.get(0).getUidValue());
        assertEquals(ResourceOperation.CREATE, results.get(0).getOperation());
        assertEquals(ProvisioningReport.Status.SUCCESS, results.get(0).getStatus());

        User donizetti = userDAO.find(results.get(0).getKey());
        assertNotNull(donizetti);
        assertEquals("donizetti", donizetti.getUsername());
        assertEquals("Gaetano", donizetti.getPlainAttr("firstname").get().getValuesAsStrings().get(0));
    }
}
