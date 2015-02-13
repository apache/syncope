package org.apache.syncope.core.provisioning.camel.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.core.misc.security.AuthContextUtil;
import org.apache.syncope.core.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class RoleCreateInSyncProcessor implements Processor {

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Override
    @SuppressWarnings("unchecked")
    public void process(final Exchange exchange) {
        WorkflowResult<Long> created = (WorkflowResult) exchange.getIn().getBody();

        RoleTO actual = exchange.getProperty("subject", RoleTO.class);
        Map<Long, String> roleOwnerMap = exchange.getProperty("roleOwnerMap", Map.class);
        Set<String> excludedResource = exchange.getProperty("excludedResources", Set.class);

        AttrTO roleOwner = actual.getPlainAttrMap().get(StringUtils.EMPTY);
        if (roleOwner != null) {
            roleOwnerMap.put(created.getResult(), roleOwner.getValues().iterator().next());
        }

        AuthContextUtil.extendAuthContext(
                created.getResult(), RoleEntitlementUtil.getEntitlementNameFromRoleKey(created.getResult()));

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(
                created, actual.getVirAttrs(), excludedResource);

        taskExecutor.execute(tasks);

        exchange.getOut().setBody(new AbstractMap.SimpleEntry<>(created.getResult(), null));
    }
}
