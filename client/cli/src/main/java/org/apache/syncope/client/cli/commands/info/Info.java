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
package org.apache.syncope.client.cli.commands.info;

import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Info {

    private static final Logger LOG = LoggerFactory.getLogger(Info.class);

    private final PlatformInfo platformInfo = SyncopeServices.get(SyncopeService.class).platform();

    private final InfoResultManager infoResultManager = new InfoResultManager();

    public void version() {
        try {
            infoResultManager.printVersion(platformInfo.getVersion());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pwdResetAllowed() {
        try {
            infoResultManager.printPwdResetAllowed(platformInfo.isPwdResetAllowed());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void resetWithSecurityQuestion() {
        try {
            infoResultManager.printPwdResetRequiringSecurityQuestions(
                    platformInfo.isPwdResetRequiringSecurityQuestions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void selfRegistrationAllowed() {
        try {
            infoResultManager.printSelfRegistrationAllowed(platformInfo.isSelfRegAllowed());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void provisioningManager() {
        try {
            infoResultManager.printProvisioningManager(
                    platformInfo.getAnyObjectProvisioningManager(),
                    platformInfo.getUserProvisioningManager(),
                    platformInfo.getGroupProvisioningManager());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void workflowAdapter() {
        try {
            infoResultManager.printWorkflowAdapter(
                    platformInfo.getAnyObjectWorkflowAdapter(),
                    platformInfo.getUserWorkflowAdapter(),
                    platformInfo.getGroupWorkflowAdapter());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void accountRules() {
        try {
            infoResultManager.printAccountRules(platformInfo.getAccountRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void connidLocations() {
        try {
            infoResultManager.printConnidLocations(platformInfo.getConnIdLocations());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void reconciliationFilterBuilders() {
        try {
            infoResultManager.printReconciliationFilterBuilders(platformInfo.getReconciliationFilterBuilders());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void logicActions() {
        try {
            infoResultManager.printLogicActions(platformInfo.getLogicActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void mappingItemTransformers() {
        try {
            infoResultManager.printMappingItemTransformers(platformInfo.getItemTransformers());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void passwordRules() {
        try {
            infoResultManager.printPasswordRules(platformInfo.getPasswordRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void propagationActions() {
        try {
            infoResultManager.printPropagationActions(platformInfo.getPropagationActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pushActions() {
        try {
            infoResultManager.printPushActions(platformInfo.getPushActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pushCorrelationActions() {
        try {
            infoResultManager.printCorrelationActions(platformInfo.getPushCorrelationRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void reportletConfs() {
        try {
            infoResultManager.printReportletConfs(platformInfo.getReportletConfs());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pullActions() {
        try {
            infoResultManager.printPullActions(platformInfo.getPullActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pullCorrelationRules() {
        try {
            infoResultManager.printCorrelationRules(platformInfo.getPullCorrelationRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void taskJobs() {
        try {
            infoResultManager.printJobs(platformInfo.getTaskJobs());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void validators() {
        try {
            infoResultManager.printValidators(platformInfo.getValidators());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void passwordGenerators() {
        try {
            infoResultManager.printPasswordGenerator(platformInfo.getPasswordGenerator());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void virAttrCache() {
        try {
            infoResultManager.printVirtualAttributeCacheClass(platformInfo.getVirAttrCache());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }
}
