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
package org.apache.syncope.types;

import java.util.EnumSet;

public final class AuditElements {

    private AuditElements() {
    }

    public enum Category {

        authentication(AuthenticationSubCategory.class),
        configuration(ConfigurationSubCategory.class),
        connector(ConnectorSubCategory.class),
        logger(LoggerSubCategory.class),
        notification(NotificationSubCategory.class),
        policy(PolicySubCategory.class),
        report(ReportSubCategory.class),
        resource(ResourceSubCategory.class),
        role(RoleSubCategory.class),
        schema(SchemaSubCategory.class),
        task(TaskSubCategory.class),
        user(UserSubCategory.class),
        userRequest(UserRequestSubCategory.class),
        workflow(WorkflowSubCategory.class);

        private Class<? extends Enum<?>> subCategory;

        Category(final Class<? extends Enum<?>> subCategory) {
            this.subCategory = subCategory;
        }

        public Class<? extends Enum> getSubCategory() {
            return subCategory;
        }

        public EnumSet<? extends Enum<?>> getSubCategoryElements() {
            return EnumSet.allOf(getSubCategory());
        }
    }

    public enum Result {

        success,
        failure

    }

    public enum AuthenticationSubCategory {

        login,
        getEntitlements

    }

    public enum ConfigurationSubCategory {

        list,
        create,
        read,
        update,
        delete,
        getMailTemplates,
        getValidators,
        dbExport

    }

    public enum ConnectorSubCategory {

        list,
        create,
        read,
        update,
        delete,
        getBundles,
        getSchemaNames,
        getConfigurationProperties,
        check,
        readConnectorBean

    }

    public enum LoggerSubCategory {

        list,
        setLevel,
        delete

    }

    public enum NotificationSubCategory {

        list,
        create,
        read,
        update,
        delete,
        sent

    }

    public enum PolicySubCategory {

        list,
        create,
        read,
        update,
        delete

    }

    public enum ReportSubCategory {

        list,
        listExecutions,
        create,
        read,
        readExecution,
        update,
        delete,
        deleteExecution,
        getReportletConfClasses,
        execute,
        exportExecutionResult

    }

    public enum ResourceSubCategory {

        list,
        create,
        read,
        update,
        delete,
        getObject,
        getRoleResourcesMapping,
        getPropagationActionsClasses

    }

    public enum RoleSubCategory {

        list,
        create,
        read,
        selfRead,
        update,
        delete,
        parent,
        children

    }

    public enum SchemaSubCategory {

        list,
        create,
        read,
        update,
        delete,
        listDerived,
        createDerived,
        readDerived,
        updateDerived,
        deleteDerived,
        listVirtual,
        createVirtual,
        readVirtual,
        updateVirtual,
        deleteVirtual

    }

    public enum TaskSubCategory {

        list,
        create,
        read,
        update,
        delete,
        listExecutions,
        getJobClasses,
        getSyncActionsClasses,
        readExecution,
        execute,
        report,
        deleteExecution

    }

    public enum UserSubCategory {

        list,
        create,
        read,
        update,
        delete,
        verifyPassword,
        search,
        setStatus,
        executeWorkflow,
        getForms,
        getFormForUser,
        claimForm,
        submitForm

    }

    public enum UserRequestSubCategory {

        list,
        create,
        read,
        update,
        delete,
        isCreateAllowed,}

    public enum WorkflowSubCategory {

        getDefinition,
        updateDefinition,
        getDefinedTasks

    }
}
