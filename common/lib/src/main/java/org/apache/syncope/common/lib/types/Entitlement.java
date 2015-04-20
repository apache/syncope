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
package org.apache.syncope.common.lib.types;

public enum Entitlement {

    ANONYMOUS,
    REALM_LIST,
    REALM_CREATE,
    REALM_UPDATE,
    REALM_DELETE,
    ROLE_LIST,
    ROLE_CREATE,
    ROLE_READ,
    ROLE_UPDATE,
    ROLE_DELETE,
    SCHEMA_LIST,
    SCHEMA_CREATE,
    SCHEMA_READ,
    SCHEMA_UPDATE,
    SCHEMA_DELETE,
    USER_SEARCH,
    USER_LIST,
    USER_CREATE,
    USER_READ,
    USER_UPDATE,
    USER_DELETE,
    USER_VIEW,
    GROUP_SEARCH,
    GROUP_CREATE,
    GROUP_READ,
    GROUP_UPDATE,
    GROUP_DELETE,
    RESOURCE_LIST,
    RESOURCE_CREATE,
    RESOURCE_READ,
    RESOURCE_UPDATE,
    RESOURCE_DELETE,
    RESOURCE_GETCONNECTOROBJECT,
    CONNECTOR_LIST,
    CONNECTOR_CREATE,
    CONNECTOR_READ,
    CONNECTOR_UPDATE,
    CONNECTOR_DELETE,
    CONNECTOR_RELOAD,
    CONFIGURATION_EXPORT,
    CONFIGURATION_LIST,
    CONFIGURATION_SET,
    CONFIGURATION_DELETE,
    TASK_LIST,
    TASK_CREATE,
    TASK_READ,
    TASK_UPDATE,
    TASK_DELETE,
    TASK_EXECUTE,
    POLICY_LIST,
    POLICY_CREATE,
    POLICY_READ,
    POLICY_UPDATE,
    POLICY_DELETE,
    WORKFLOW_DEF_READ,
    WORKFLOW_DEF_UPDATE,
    WORKFLOW_TASK_LIST,
    WORKFLOW_FORM_LIST,
    WORKFLOW_FORM_READ,
    WORKFLOW_FORM_CLAIM,
    WORKFLOW_FORM_SUBMIT,
    NOTIFICATION_LIST,
    NOTIFICATION_CREATE,
    NOTIFICATION_READ,
    NOTIFICATION_UPDATE,
    NOTIFICATION_DELETE,
    REPORT_LIST,
    REPORT_READ,
    REPORT_CREATE,
    REPORT_UPDATE,
    REPORT_DELETE,
    REPORT_EXECUTE,
    LOG_LIST,
    LOG_SET_LEVEL,
    LOG_DELETE,
    AUDIT_LIST,
    AUDIT_ENABLE,
    AUDIT_DISABLE,
    SECURITY_QUESTION_CREATE,
    SECURITY_QUESTION_UPDATE,
    SECURITY_QUESTION_DELETE,
    ROUTE_READ,
    ROUTE_LIST,
    ROUTE_UPDATE;

}
