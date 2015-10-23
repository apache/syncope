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

public class Info {

    private final SyncopeTO syncopeTO = SyncopeServices.get(SyncopeService.class).info();
    
    private final InfoResultManager infoResultManager = new InfoResultManager();

    public void version() {
        try {
            infoResultManager.generic("Syncope version: " + syncopeTO.getVersion());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void pwdResetAllowed() {
        try {
            infoResultManager.generic("Password reset allowed: " + syncopeTO.isPwdResetAllowed());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void resetWithSecurityQuestion() {
        try {
            infoResultManager.generic("Password reset requiring security question: "
                    + syncopeTO.isPwdResetRequiringSecurityQuestions());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void selfRegistrationAllowed() {
        try {
            infoResultManager.generic("Self registration allowed: " + syncopeTO.isSelfRegAllowed());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void provisioningManager() {
        try {
            infoResultManager.generic(
                    "Any object provisioning manager class: " + syncopeTO.getAnyObjectProvisioningManager(),
                    "User       provisioning manager class: " + syncopeTO.getUserProvisioningManager(),
                    "Group      provisioning manager class: " + syncopeTO.getGroupProvisioningManager());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void workflowAdapter() {
        try {
            infoResultManager.generic(
                    "Any object workflow adapter class: " + syncopeTO.getAnyObjectWorkflowAdapter(),
                    "User       workflow adapter class: " + syncopeTO.getUserWorkflowAdapter(),
                    "Group      workflow adapter class: " + syncopeTO.getGroupWorkflowAdapter());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void accountRules() {
        try {
            for (final String accountRule : syncopeTO.getAccountRules()) {
                infoResultManager.generic("Account rule: " + accountRule);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void connidLocation() {
        try {
            for (final String location : syncopeTO.getConnIdLocations()) {
                infoResultManager.generic("ConnId location: " + location);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void logicActions() {
        try {
            for (final String logic : syncopeTO.getLogicActions()) {
                infoResultManager.generic("Logic action: " + logic);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void mailTemplates() {
        try {
            for (final String template : syncopeTO.getMailTemplates()) {
                infoResultManager.generic("Mail template: " + template);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void mappingItemTransformers() {
        try {
            for (final String tranformer : syncopeTO.getMappingItemTransformers()) {
                infoResultManager.generic("Mapping item tranformer: " + tranformer);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void passwordRules() {
        try {
            for (final String rules : syncopeTO.getPasswordRules()) {
                infoResultManager.generic("Password rule: " + rules);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void propagationActions() {
        try {
            for (final String action : syncopeTO.getPropagationActions()) {
                infoResultManager.generic("Propagation action: " + action);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void pushActions() {
        try {
            for (final String action : syncopeTO.getPushActions()) {
                infoResultManager.generic("Push action: " + action);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void pushCorrelationActions() {
        try {
            for (final String rule : syncopeTO.getPushCorrelationRules()) {
                infoResultManager.generic("Push correlation rule: " + rule);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void reportlets() {
        try {
            for (final String reportlet : syncopeTO.getReportlets()) {
                infoResultManager.generic("Reportlet: " + reportlet);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void syncActions() {
        try {
            for (final String action : syncopeTO.getSyncActions()) {
                infoResultManager.generic("Sync action: " + action);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void syncCorrelationRules() {
        try {
            for (final String rule : syncopeTO.getSyncCorrelationRules()) {
                infoResultManager.generic("Sync correlation rule: " + rule);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void taskJobs() {
        try {
            for (final String job : syncopeTO.getTaskJobs()) {
                infoResultManager.generic("Task job: " + job);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void validators() {
        try {
            for (final String validator : syncopeTO.getValidators()) {
                infoResultManager.generic("Validator: " + validator);
            }
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void passwordGenerators() {
        try {
            infoResultManager.generic(
                    "Password generator class: " + syncopeTO.getPasswordGenerator());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }

    public void virAttrCache() {
        try {
            infoResultManager.generic(
                    "Virtual attribute cache class: " + syncopeTO.getVirAttrCache());
        } catch (final Exception ex) {
            infoResultManager.generic(ex.getMessage());
        }
    }
}
