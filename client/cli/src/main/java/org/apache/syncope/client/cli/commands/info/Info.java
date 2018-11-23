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
                    platformInfo.getProvisioningInfo().getAnyObjectProvisioningManager(),
                    platformInfo.getProvisioningInfo().getUserProvisioningManager(),
                    platformInfo.getProvisioningInfo().getGroupProvisioningManager());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }

    public void accountRules() {
        platformInfo.getJavaImplInfo(ImplementationType.ACCOUNT_RULE).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
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
        platformInfo.getJavaImplInfo(ImplementationType.RECON_FILTER_BUILDER).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void logicActions() {
        platformInfo.getJavaImplInfo(ImplementationType.LOGIC_ACTIONS).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void itemTransformers() {
        platformInfo.getJavaImplInfo(ImplementationType.ITEM_TRANSFORMER).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void passwordRules() {
        platformInfo.getJavaImplInfo(ImplementationType.PASSWORD_RULE).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void propagationActions() {
        platformInfo.getJavaImplInfo(ImplementationType.PROPAGATION_ACTIONS).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void pushActions() {
        platformInfo.getJavaImplInfo(ImplementationType.PUSH_ACTIONS).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void reportletConfs() {
        platformInfo.getJavaImplInfo(ImplementationType.REPORTLET).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void pullActions() {
        platformInfo.getJavaImplInfo(ImplementationType.PULL_ACTIONS).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void pullCorrelationRules() {
        platformInfo.getJavaImplInfo(ImplementationType.PULL_CORRELATION_RULE).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void taskJobs() {
        platformInfo.getJavaImplInfo(ImplementationType.TASKJOB_DELEGATE).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
    }

    public void validators() {
        platformInfo.getJavaImplInfo(ImplementationType.VALIDATOR).ifPresent(info -> {
            try {
                infoResultManager.printAccountRules(info.getClasses());
            } catch (final Exception ex) {
                LOG.error("Information error", ex);
                infoResultManager.genericError(ex.getMessage());
            }
        });
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
            infoResultManager.printVirtualAttributeCacheClass(platformInfo.getProvisioningInfo().getVirAttrCache());
        } catch (final Exception ex) {
            LOG.error("Information error", ex);
            infoResultManager.genericError(ex.getMessage());
        }
    }
}
