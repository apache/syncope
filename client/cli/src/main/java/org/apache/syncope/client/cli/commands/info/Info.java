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

import java.util.Optional;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.types.ImplementationType;
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
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.ACCOUNT_RULE);
        if (info.isPresent()) {
            try {
                infoResultManager.printAccountRules(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
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

    public void reconFilterBuilders() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.RECON_FILTER_BUILDER);
        if (info.isPresent()) {
            try {
                infoResultManager.printReconFilterBuilders(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void logicActions() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.LOGIC_ACTIONS);
        if (info.isPresent()) {
            try {
                infoResultManager.printLogicActions(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void itemTransformers() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.ITEM_TRANSFORMER);
        if (info.isPresent()) {
            try {
                infoResultManager.printItemTransformers(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void passwordRules() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.PASSWORD_RULE);
        if (info.isPresent()) {
            try {
                infoResultManager.printPasswordRules(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void propagationActions() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.PROPAGATION_ACTIONS);
        if (info.isPresent()) {
            try {
                infoResultManager.printPropagationActions(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void pushActions() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.PUSH_ACTIONS);
        if (info.isPresent()) {
            try {
                infoResultManager.printPushActions(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void reportletConfs() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.REPORTLET);
        if (info.isPresent()) {
            try {
                infoResultManager.printReportletConfs(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void pullActions() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.PULL_ACTIONS);
        if (info.isPresent()) {
            try {
                infoResultManager.printPullActions(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void pullCorrelationRules() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.PULL_CORRELATION_RULE);
        if (info.isPresent()) {
            try {
                infoResultManager.printCorrelationRules(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void taskJobs() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.TASKJOB_DELEGATE);
        if (info.isPresent()) {
            try {
                infoResultManager.printJobs(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void validators() {
        Optional<JavaImplInfo> info = platformInfo.getJavaImplInfo(ImplementationType.VALIDATOR);
        if (info.isPresent()) {
            try {
                infoResultManager.printValidators(info.get().getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        }
    }

    public void passwordGenerator() {
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
