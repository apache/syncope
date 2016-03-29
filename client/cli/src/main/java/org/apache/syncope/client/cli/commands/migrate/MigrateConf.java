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
package org.apache.syncope.client.cli.commands.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.syncope.client.cli.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateConf {

    private static final Logger LOG = LoggerFactory.getLogger(MigrateConf.class);

    private static final String HELP_MESSAGE = "migrate --conf {SRC} {DST}";

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MigrateResultManager migrateResultManager = new MigrateResultManager();

    private final Input input;

    private static void copyAttrs(
            final XMLStreamReader streamReader, final XMLStreamWriter streamWriter, final String... but)
            throws XMLStreamException {

        Set<String> exceptions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        exceptions.addAll(Arrays.asList(but));

        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
            String name = streamReader.getAttributeLocalName(i);
            if (!exceptions.contains(name)) {
                streamWriter.writeAttribute(name, streamReader.getAttributeValue(i));
            }
        }
    }

    private static String getAttributeValue(final XMLStreamReader streamReader, final String key) {
        String value = streamReader.getAttributeValue("", key);
        if (value == null || value.isEmpty()) {
            value = streamReader.getAttributeValue("", key.toUpperCase());
            if (value == null || value.isEmpty()) {
                value = streamReader.getAttributeValue("", key.toLowerCase());
            }
        }

        return value;
    }

    private static void exec(final String src, final String dst) throws XMLStreamException, IOException {
        XMLStreamWriter writer = new PrettyPrintXMLStreamWriter(
                OUTPUT_FACTORY.createXMLStreamWriter(new FileWriter(dst)), 2);
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("dataset");

        StringWriter reporterSW = new StringWriter();
        XMLStreamWriter reporter = new PrettyPrintXMLStreamWriter(
                OUTPUT_FACTORY.createXMLStreamWriter(reporterSW), 2);
        reporter.writeStartDocument("UTF-8", "1.0");
        reporter.writeStartElement("dataset");

        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new FileInputStream(src));
        reader.nextTag(); // root
        reader.nextTag(); // dataset

        writer.writeStartElement("AnyType");
        writer.writeAttribute("name", "USER");
        writer.writeAttribute("kind", "USER");
        writer.writeEndElement();

        writer.writeStartElement("AnyTypeClass");
        writer.writeAttribute("name", "BaseUser");
        writer.writeEndElement();

        writer.writeStartElement("AnyType_AnyTypeClass");
        writer.writeAttribute("anyType_name", "USER");
        writer.writeAttribute("anyTypeClass_name", "BaseUser");
        writer.writeEndElement();

        writer.writeStartElement("AnyType");
        writer.writeAttribute("name", "GROUP");
        writer.writeAttribute("kind", "GROUP");
        writer.writeEndElement();

        writer.writeStartElement("AnyTypeClass");
        writer.writeAttribute("name", "BaseGroup");
        writer.writeEndElement();

        writer.writeStartElement("AnyType_AnyTypeClass");
        writer.writeAttribute("anyType_name", "GROUP");
        writer.writeAttribute("anyTypeClass_name", "BaseGroup");
        writer.writeEndElement();

        writer.writeStartElement("AnyTypeClass");
        writer.writeAttribute("name", "BaseUMembership");
        writer.writeEndElement();

        Set<String> connInstanceCapabilities = new HashSet<>();

        String globalAccountPolicy = null;
        String globalPasswordPolicy = null;
        while (reader.hasNext()) {
            if (reader.isStartElement()) {
                switch (reader.getLocalName().toLowerCase()) {
                    case "syncopeconf":
                        writer.writeStartElement("SyncopeConf");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "cschema":
                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "cattr":
                        writer.writeStartElement("CPlainAttr");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "cattrvalue":
                        writer.writeStartElement("CPlainAttrValue");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "uschema":
                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("anyTypeClass_name", "BaseUser");
                        writer.writeEndElement();
                        break;

                    case "uderschema":
                        writer.writeStartElement("DerSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("anyTypeClass_name", "BaseUser");
                        writer.writeEndElement();
                        break;

                    case "uvirschema":
                        reporter.writeStartElement("VirSchema");
                        copyAttrs(reader, reporter);
                        reporter.writeEndElement();
                        break;

                    case "rschema":
                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("anyTypeClass_name", "BaseGroup");
                        writer.writeEndElement();
                        break;

                    case "rderschema":
                        writer.writeStartElement("DerSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("anyTypeClass_name", "BaseGroup");
                        writer.writeEndElement();
                        break;

                    case "rvirschema":
                        reporter.writeStartElement("VirSchema");
                        copyAttrs(reader, reporter);
                        reporter.writeEndElement();
                        break;

                    case "mschema":
                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("anyTypeClass_name", "BaseUMembership");
                        writer.writeEndElement();
                        break;

                    case "mderschema":
                        writer.writeStartElement("DerSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("anyTypeClass_name", "BaseUMembership");
                        writer.writeEndElement();
                        break;

                    case "mvirschema":
                        reporter.writeStartElement("VirSchema");
                        copyAttrs(reader, reporter);
                        reporter.writeEndElement();
                        break;

                    case "policy":
                        String policyId = getAttributeValue(reader, "id");
                        ObjectNode specification = (ObjectNode) OBJECT_MAPPER.readTree(
                                getAttributeValue(reader, "specification"));

                        switch (getAttributeValue(reader, "DTYPE")) {
                            case "SyncPolicy":
                                writer.writeStartElement("PullPolicy");
                                writer.writeAttribute("id", policyId);
                                writer.writeAttribute(
                                        "description", getAttributeValue(reader, "description"));
                                writer.writeEndElement();
                                break;

                            case "PasswordPolicy":
                                writer.writeStartElement("PasswordPolicy");
                                writer.writeAttribute("id", policyId);
                                writer.writeAttribute(
                                        "description", getAttributeValue(reader, "description"));

                                if ("GLOBAL_PASSWORD".equalsIgnoreCase(getAttributeValue(reader, "type"))) {
                                    globalPasswordPolicy = getAttributeValue(reader, "id");
                                }

                                JsonNode allowNullPassword = specification.get("allowNullPassword");
                                if (allowNullPassword != null) {
                                    writer.writeAttribute("allowNullPassword", allowNullPassword.asText());
                                    specification.remove("allowNullPassword");
                                }
                                JsonNode historyLength = specification.get("historyLength");
                                if (historyLength != null) {
                                    writer.writeAttribute("historyLength", historyLength.asText());
                                    specification.remove("historyLength");
                                }
                                specification.put(
                                        "@class", "org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf");
                                writer.writeEndElement();

                                writer.writeStartElement("PasswordRuleConfInstance");
                                writer.writeAttribute("id", policyId);
                                writer.writeAttribute("passwordPolicy_id", policyId);
                                writer.writeAttribute("serializedInstance", specification.toString());
                                writer.writeEndElement();
                                break;

                            case "AccountPolicy":
                                writer.writeStartElement("AccountPolicy");
                                writer.writeAttribute("id", policyId);
                                writer.writeAttribute(
                                        "description", getAttributeValue(reader, "description"));

                                if ("GLOBAL_ACCOUNT".equalsIgnoreCase(getAttributeValue(reader, "type"))) {
                                    globalAccountPolicy = getAttributeValue(reader, "id");
                                }

                                JsonNode propagateSuspension = specification.get("propagateSuspension");
                                if (propagateSuspension != null) {
                                    writer.writeAttribute("propagateSuspension", propagateSuspension.asText());
                                    specification.remove("propagateSuspension");
                                }
                                JsonNode permittedLoginRetries = specification.get("permittedLoginRetries");
                                if (permittedLoginRetries != null) {
                                    writer.writeAttribute(
                                            "maxAuthenticationAttempts", permittedLoginRetries.asText());
                                    specification.remove("permittedLoginRetries");
                                }
                                specification.put(
                                        "@class", "org.apache.syncope.common.lib.policy.DefaultAccountRuleConf");
                                writer.writeEndElement();

                                writer.writeStartElement("AccountRuleConfInstance");
                                writer.writeAttribute("id", policyId);
                                writer.writeAttribute("passwordPolicy_id", policyId);
                                writer.writeAttribute("serializedInstance", specification.toString());
                                writer.writeEndElement();
                                break;

                            default:
                        }
                        break;

                    case "conninstance":
                        writer.writeStartElement("ConnInstance");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "conninstance_capabilities":
                        String connInstanceId = getAttributeValue(reader, "ConnInstance_id");
                        String capabilities = getAttributeValue(reader, "capabilities");
                        if (capabilities.startsWith("ONE_PHASE_")) {
                            capabilities = capabilities.substring(10);
                        } else if (capabilities.startsWith("TWO_PHASES_")) {
                            capabilities = capabilities.substring(11);
                        }
                        if (!connInstanceCapabilities.contains(connInstanceId + capabilities)) {
                            writer.writeStartElement("ConnInstance_capabilities");
                            writer.writeAttribute("ConnInstance_id", connInstanceId);
                            writer.writeAttribute("capabilities", capabilities);
                            writer.writeEndElement();

                            connInstanceCapabilities.add(connInstanceId + capabilities);
                        }
                        break;

                    case "externalresource":
                        writer.writeStartElement("ExternalResource");
                        copyAttrs(reader, writer,
                                "syncTraceLevel", "userializedSyncToken", "rserializedSyncToken");
                        writer.writeAttribute(
                                "pullTraceLevel", getAttributeValue(reader, "syncTraceLevel"));
                        writer.writeEndElement();
                        break;

                    case "externalresource_propactions":
                        writer.writeStartElement("ExternalResource_PropActions");
                        copyAttrs(reader, writer, "element");

                        String propActionClassName = getAttributeValue(reader, "element");
                        switch (propActionClassName) {
                            case "org.apache.syncope.core.propagation.impl.LDAPMembershipPropagationActions":
                                propActionClassName = "org.apache.syncope.core.provisioning.java.propagation."
                                        + "LDAPMembershipPropagationActions";
                                break;

                            case "org.apache.syncope.core.propagation.impl.LDAPPasswordPropagationActions":
                                propActionClassName = "org.apache.syncope.core.provisioning.java.propagation."
                                        + "LDAPPasswordPropagationActions";
                                break;

                            case "org.apache.syncope.core.propagation.impl.DBPasswordPropagationActions":
                                propActionClassName = "org.apache.syncope.core.provisioning.java.propagation."
                                        + "DBPasswordPropagationActions";

                            default:
                        }
                        writer.writeAttribute("actionClassName", propActionClassName);
                        writer.writeEndElement();
                        break;

                    case "policy_externalresource":
                        writer.writeStartElement("AccountPolicy_ExternalResource");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "umapping":
                        String umappingId = getAttributeValue(reader, "id");
                        writer.writeStartElement("Provision");
                        writer.writeAttribute("id", umappingId);
                        writer.writeAttribute(
                                "resource_name", getAttributeValue(reader, "resource_name"));
                        writer.writeAttribute("anyType_name", "USER");
                        writer.writeAttribute("objectClass", "__ACCOUNT__");
                        writer.writeEndElement();

                        writer.writeStartElement("Mapping");
                        writer.writeAttribute("id", umappingId);
                        writer.writeAttribute("provision_id", umappingId);

                        String uaccountLink = getAttributeValue(reader, "accountlink");
                        if (uaccountLink != null && !uaccountLink.isEmpty()) {
                            writer.writeAttribute("connObjectLink", uaccountLink);
                        }
                        writer.writeEndElement();
                        break;

                    case "umappingitem":
                        String uIntMappingType = getAttributeValue(reader, "intMappingType");
                        if (uIntMappingType.endsWith("VirtualSchema")) {
                            reporter.writeStartElement("MappingItem");
                            copyAttrs(reader, reporter, "accountid");
                            reporter.writeEndElement();
                        } else {
                            writer.writeStartElement("MappingItem");
                            copyAttrs(reader, writer, "accountid");
                            writer.writeAttribute("connObjectKey", getAttributeValue(reader, "accountid"));
                            writer.writeEndElement();
                        }
                        break;

                    case "rmapping":
                        String rmappingId = getAttributeValue(reader, "id");
                        writer.writeStartElement("Provision");
                        writer.writeAttribute("id", rmappingId);
                        writer.writeAttribute(
                                "resource_name", getAttributeValue(reader, "resource_name"));
                        writer.writeAttribute("anyType_name", "GROUP");
                        writer.writeAttribute("objectClass", "__GROUP__");
                        writer.writeEndElement();

                        writer.writeStartElement("Mapping");
                        writer.writeAttribute("id", rmappingId);
                        writer.writeAttribute("provision_id", rmappingId);

                        String raccountLink = getAttributeValue(reader, "accountlink");
                        if (raccountLink != null && !raccountLink.isEmpty()) {
                            writer.writeAttribute("connObjectLink", raccountLink);
                        }
                        writer.writeEndElement();
                        break;

                    case "rmappingitem":
                        String rIntMappingType = getAttributeValue(reader, "intMappingType");
                        if (rIntMappingType.endsWith("VirtualSchema")) {
                            reporter.writeStartElement("MappingItem");
                            copyAttrs(reader, reporter, "accountid");
                            reporter.writeEndElement();
                        } else {
                            writer.writeStartElement("MappingItem");
                            copyAttrs(reader, writer, "accountid");
                            writer.writeAttribute("connObjectKey", getAttributeValue(reader, "accountid"));
                            writer.writeEndElement();
                        }
                        break;

                    case "task":
                        writer.writeStartElement("Task");
                        copyAttrs(reader, writer,
                                "DTYPE", "propagationMode", "subjectType", "subjectId", "xmlAttributes",
                                "jobClassName", "userTemplate", "roleTemplate", "userFilter", "roleFilter");

                        String taskId = getAttributeValue(reader, "id");

                        switch (getAttributeValue(reader, "DTYPE")) {
                            case "PropagationTask":
                                writer.writeAttribute("DTYPE", "PropagationTask");
                                writer.writeAttribute(
                                        "anyTypeKind", getAttributeValue(reader, "subjectType"));
                                writer.writeAttribute(
                                        "anyKey", getAttributeValue(reader, "subjectId"));
                                writer.writeAttribute(
                                        "attributes", getAttributeValue(reader, "xmlAttributes"));
                                writer.writeEndElement();
                                break;

                            case "SyncTask":
                                writer.writeAttribute("DTYPE", "PullTask");
                                writer.writeEndElement();

                                String userTemplate = getAttributeValue(reader, "userTemplate");
                                if (userTemplate != null && !userTemplate.isEmpty()) {
                                    writer.writeStartElement("AnyTemplatePullTask");
                                    writer.writeAttribute("id", taskId);
                                    writer.writeAttribute("pullTask_id", taskId);
                                    writer.writeAttribute("anyType_name", "USER");
                                    writer.writeAttribute("template", userTemplate);
                                    writer.writeEndElement();
                                }
                                String roleTemplate = getAttributeValue(reader, "roleTemplate");
                                if (roleTemplate != null && !roleTemplate.isEmpty()) {
                                    writer.writeStartElement("AnyTemplatePullTask");
                                    writer.writeAttribute("id", taskId);
                                    writer.writeAttribute("pullTask_id", taskId);
                                    writer.writeAttribute("anyType_name", "GROUP");
                                    writer.writeAttribute("template", roleTemplate);
                                    writer.writeEndElement();
                                }
                                break;

                            case "SchedTask":
                                writer.writeAttribute("DTYPE", "SchedTask");
                                writer.writeAttribute(
                                        "jobDelegateClassName", getAttributeValue(reader, "jobClassName"));
                                writer.writeEndElement();
                                break;

                            case "NotificationTask":
                                writer.writeAttribute("DTYPE", "NotificationTask");
                                writer.writeEndElement();
                                break;

                            case "PushTask":
                                writer.writeAttribute("DTYPE", "PushTask");
                                writer.writeEndElement();

                                String userFilter = getAttributeValue(reader, "userFilter");
                                if (userFilter != null && !userFilter.isEmpty()) {
                                    writer.writeStartElement("PushTaskAnyFilter");
                                    writer.writeAttribute("id", taskId);
                                    writer.writeAttribute("pusTask_id", taskId);
                                    writer.writeAttribute("anyType_name", "USER");
                                    writer.writeAttribute("fiql", userFilter);
                                    writer.writeEndElement();
                                }
                                String roleFilter = getAttributeValue(reader, "roleFilter");
                                if (roleFilter != null && !roleFilter.isEmpty()) {
                                    writer.writeStartElement("PushTaskAnyFilter");
                                    writer.writeAttribute("id", taskId);
                                    writer.writeAttribute("pusTask_id", taskId);
                                    writer.writeAttribute("anyType_name", "GROUP");
                                    writer.writeAttribute("fiql", roleFilter);
                                    writer.writeEndElement();
                                }
                                break;

                            default:
                        }
                        break;

                    case "taskexec":
                        writer.writeStartElement("TaskExec");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "synctask_actionsclassnames":
                        writer.writeStartElement("PullTask_actionsClassNames");
                        writer.writeAttribute(
                                "pullTask_id", getAttributeValue(reader, "syncTask_id"));

                        String syncActionClassName = getAttributeValue(reader, "element");
                        switch (syncActionClassName) {
                            case "org.apache.syncope.core.sync.impl.LDAPMembershipSyncActions":
                                syncActionClassName =
                                        "org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions";
                                break;

                            case "org.apache.syncope.core.sync.impl.LDAPPasswordSyncActions":
                                syncActionClassName =
                                        "org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions";
                                break;

                            case "org.apache.syncope.core.sync.impl.DBPasswordSyncActions":
                                syncActionClassName =
                                        "org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions";

                            default:
                        }
                        writer.writeAttribute("actionClassName", syncActionClassName);
                        writer.writeEndElement();
                        break;

                    case "notification":
                        writer.writeStartElement("Notification");
                        copyAttrs(reader, writer, "recipientAttrType", "template");
                        String recipientAttrType = getAttributeValue(reader, "recipientAttrType");
                        if ("UserSchema".equals(recipientAttrType)) {
                            recipientAttrType = "UserPlainSchema";
                        } else if ("RoleSchema".equals(recipientAttrType)) {
                            recipientAttrType = "GroupPlainSchema";
                        }
                        writer.writeAttribute("recipientAttrType", recipientAttrType);
                        writer.writeAttribute(
                                "template_name", getAttributeValue(reader, "template"));
                        writer.writeEndElement();

                        String notificationId = getAttributeValue(reader, "id");

                        String userAbout = getAttributeValue(reader, "userAbout");
                        if (userAbout != null && !userAbout.isEmpty()) {
                            writer.writeStartElement("AnyAbout");
                            writer.writeAttribute("id", notificationId);
                            writer.writeAttribute("notification_id", notificationId);
                            writer.writeAttribute("anyType_name", "USER");
                            writer.writeAttribute("filter", userAbout);
                            writer.writeEndElement();
                        }
                        String roleAbout = getAttributeValue(reader, "roleAbout");
                        if (roleAbout != null && !roleAbout.isEmpty()) {
                            writer.writeStartElement("AnyAbout");
                            writer.writeAttribute("id", notificationId);
                            writer.writeAttribute("notification_id", notificationId);
                            writer.writeAttribute("anyType_name", "GROUP");
                            writer.writeAttribute("filter", roleAbout);
                            writer.writeEndElement();
                        }
                        break;

                    case "notification_events":
                        writer.writeStartElement("Notification_events");
                        copyAttrs(reader, writer, "event");
                        writer.writeAttribute(
                                "event", getAttributeValue(reader, "events").
                                replaceAll("Controller", "Logic"));
                        writer.writeEndElement();
                        break;

                    case "notificationtask_recipients":
                        writer.writeStartElement("NotificationTask_recipients");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "report":
                        writer.writeStartElement("Report");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "reportletconfinstance":
                        writer.writeStartElement("ReportletConfInstance");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "reportexec":
                        writer.writeStartElement("ReportExec");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    case "securityquestion":
                        writer.writeStartElement("SecurityQuestion");
                        copyAttrs(reader, writer);
                        writer.writeEndElement();
                        break;

                    default:
                }
            }

            reader.next();
        }

        writer.writeStartElement("Realm");
        writer.writeAttribute("id", "1");
        writer.writeAttribute("name", "/");
        if (globalAccountPolicy != null) {
            writer.writeAttribute("accountPolicy_id", globalAccountPolicy);
        }
        if (globalPasswordPolicy != null) {
            writer.writeAttribute("passwordPolicy_id", globalPasswordPolicy);
        }
        writer.writeEndElement();

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();

        reporter.writeEndElement();
        reporter.writeEndDocument();
        reporter.close();
        System.out.println("\nVirtual items, require manual intervention:\n" + reporterSW.toString());
    }

    public MigrateConf(final Input input) {
        this.input = input;
    }

    public void migrate() {
        if (input.parameterNumber() == 2) {
            try {
                exec(input.firstParameter(), input.secondParameter());
                migrateResultManager.genericMessage(
                        "Migration completed; file successfully created under " + input.secondParameter());
            } catch (Exception e) {
                LOG.error("Error migrating configuration from {}", input.firstParameter(), e);
                migrateResultManager.genericError("Error performing configuration migration: " + e.getMessage());
            }
        } else {
            migrateResultManager.commandOptionError(HELP_MESSAGE);
        }
    }
}
