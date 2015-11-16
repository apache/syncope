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
import org.apache.syncope.common.lib.to.SyncopeTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Info {

    private static final Logger LOG = LoggerFactory.getLogger(Info.class);

    private final SyncopeTO syncopeTO = SyncopeServices.get(SyncopeService.class).info();

    private final InfoResultManager infoResultManager = new InfoResultManager();

    public void version() {
        try {
            infoResultManager.printVersion(syncopeTO.getVersion());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pwdResetAllowed() {
        try {
            infoResultManager.printPwdResetAllowed(syncopeTO.isPwdResetAllowed());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void resetWithSecurityQuestion() {
        try {
            infoResultManager.printPwdResetRequiringSecurityQuestions(syncopeTO.isPwdResetRequiringSecurityQuestions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void selfRegistrationAllowed() {
        try {
            infoResultManager.printSelfRegistrationAllowed(syncopeTO.isSelfRegAllowed());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void provisioningManager() {
        try {
            infoResultManager.printProvisioningManager(
                    syncopeTO.getAnyObjectProvisioningManager(),
                    syncopeTO.getUserProvisioningManager(),
                    syncopeTO.getGroupProvisioningManager());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void workflowAdapter() {
        try {
            infoResultManager.printWorkflowAdapter(
                    syncopeTO.getAnyObjectWorkflowAdapter(),
                    syncopeTO.getUserWorkflowAdapter(),
                    syncopeTO.getGroupWorkflowAdapter());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void accountRules() {
        try {
            infoResultManager.printAccountRules(syncopeTO.getAccountRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void connidLocations() {
        try {
            infoResultManager.printConnidLocations(syncopeTO.getConnIdLocations());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void reconciliationFilterBuilders() {
        try {
            infoResultManager.printReconciliationFilterBuilders(syncopeTO.getReconciliationFilterBuilders());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void logicActions() {
        try {
            infoResultManager.printLogicActions(syncopeTO.getLogicActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void mailTemplates() {
        try {
            infoResultManager.printMailTemplates(syncopeTO.getMailTemplates());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void mappingItemTransformers() {
        try {
            infoResultManager.printMappingItemTransformers(syncopeTO.getMappingItemTransformers());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void passwordRules() {
        try {
            infoResultManager.printPasswordRules(syncopeTO.getPasswordRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void propagationActions() {
        try {
            infoResultManager.printPropagationActions(syncopeTO.getPropagationActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pushActions() {
        try {
            infoResultManager.printPushActions(syncopeTO.getPushActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void pushCorrelationActions() {
        try {
            infoResultManager.printCorrelationActions(syncopeTO.getPushCorrelationRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void reportlets() {
        try {
            infoResultManager.printReportlets(syncopeTO.getReportlets());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void syncActions() {
        try {
            infoResultManager.printSyncActions(syncopeTO.getSyncActions());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void syncCorrelationRules() {
        try {
            infoResultManager.printCorrelationRules(syncopeTO.getSyncCorrelationRules());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void taskJobs() {
        try {
            infoResultManager.printJobs(syncopeTO.getTaskJobs());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void validators() {
        try {
            infoResultManager.printValidators(syncopeTO.getValidators());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void passwordGenerators() {
        try {
            infoResultManager.printPasswordGenerator(syncopeTO.getPasswordGenerator());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void virAttrCache() {
        try {
            infoResultManager.printVirtualAttributeCacheClass(syncopeTO.getVirAttrCache());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }
}
