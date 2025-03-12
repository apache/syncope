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
package org.apache.syncope.fit.ui;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.OpenFGAAccessPolicyConf;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.junit.jupiter.api.BeforeAll;

public class OpenFGAUIITCase extends OIDCC4UIITCase {

    private static final String OPENFGA_REINIT = "org.apache.syncope.core.provisioning.java.job.OpenFGAReinit";

    @BeforeAll
    public static void initOpenFga() {
        ImplementationTO delegate = null;
        try {
            delegate = IMPLEMENTATION_SERVICE.read(IdRepoImplementationType.TASKJOB_DELEGATE, OPENFGA_REINIT);
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                delegate = new ImplementationTO();
                delegate.setKey(OPENFGA_REINIT);
                delegate.setEngine(ImplementationEngine.JAVA);
                delegate.setType(IdRepoImplementationType.TASKJOB_DELEGATE);
                delegate.setBody(OPENFGA_REINIT);
                Response response = IMPLEMENTATION_SERVICE.create(delegate);
                delegate = IMPLEMENTATION_SERVICE.read(
                        delegate.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(delegate);
            }
        }
        assertNotNull(delegate);

        String taskKey = null;
        try {
            SchedTaskTO schedTask = new SchedTaskTO();
            schedTask.setJobDelegate(delegate.getKey());
            schedTask.setName(OPENFGA_REINIT);
            Response response = TASK_SERVICE.create(TaskType.SCHEDULED, schedTask);
            taskKey = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        } catch (SyncopeClientException e) {
            // ignore
        }

        if (taskKey == null) {
            return;
        }

        ExecSpecs execSpecs = new ExecSpecs.Builder().key(taskKey).build();
        TASK_SERVICE.execute(execSpecs);

        await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return !TASK_SERVICE.read(TaskType.SCHEDULED, execSpecs.getKey(), true).getExecutions().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }

    private static AccessPolicyTO getAccessPolicy() throws JsonProcessingException {
        JsonNode health = MAPPER.readTree(WebClient.create(
                StringUtils.substringBefore(CORE_ADDRESS, "/rest") + "/actuator/health",
                ANONYMOUS_USER, ANONYMOUS_KEY, null).
                get().readEntity(String.class));
        JsonNode openfga = health.get("components").get("openFga").get("details");
        String baseUri = openfga.get("baseUri").asText();
        String storeId = openfga.get(SyncopeConstants.MASTER_DOMAIN).get("storeId").asText();

        String description = "OpenFGA access policy";

        return POLICY_SERVICE.list(PolicyType.ACCESS).stream().
                map(AccessPolicyTO.class::cast).
                filter(policy -> description.equals(policy.getName())
                && policy.getConf() instanceof OpenFGAAccessPolicyConf).
                findFirst().
                orElseGet(() -> {
                    OpenFGAAccessPolicyConf policyConf = new OpenFGAAccessPolicyConf();
                    policyConf.setApiUrl(baseUri);
                    policyConf.setStoreId(storeId);
                    policyConf.setRelation("member");
                    policyConf.setObject("GROUP:managingDirector");

                    AccessPolicyTO policy = new AccessPolicyTO();
                    policy.setName(description);
                    policy.setConf(policyConf);

                    Response response = POLICY_SERVICE.create(PolicyType.ACCESS, policy);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail(() -> "Could not create " + description);
                    }

                    return POLICY_SERVICE.read(
                            PolicyType.ACCESS,
                            response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });
    }

    @BeforeAll
    public static void consoleOpenFGASetup() throws JsonProcessingException {
        ClientAppTO clientApp = CLIENT_APP_SERVICE.list(ClientAppType.OIDCRP).stream().
                filter(app -> getAppName(CONSOLE_ADDRESS).equals(app.getName())).
                findFirst().orElseThrow();

        clientApp.setAccessPolicy(getAccessPolicy().getKey());

        CLIENT_APP_SERVICE.update(ClientAppType.OIDCRP, clientApp);

        WA_CONFIG_SERVICE.pushToWA(WAConfigService.PushSubject.clientApps, List.of());
    }

    @Override
    public void createUnmatching() throws IOException {
        assumeFalse(true);
    }
}
