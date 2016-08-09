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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.lang3.StringUtils;
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
        exceptions.add("id");
        exceptions.add("name");

        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
            String name = streamReader.getAttributeLocalName(i);
            if (!exceptions.contains(name)) {
                streamWriter.writeAttribute(name, streamReader.getAttributeValue(i));
            }
        }
    }

    private static String getAttributeValue(final XMLStreamReader streamReader, final String key) {
        String value = null;

        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
            String attrName = streamReader.getAttributeLocalName(i);
            if (attrName.equalsIgnoreCase(key)) {
                value = streamReader.getAttributeValue(i);
            }
        }

        return value;
    }

    private static void writeIntAttrName(
            final String intMappingType,
            final String intAttrNameKey,
            final String intAttrNameValue,
            final XMLStreamWriter writer)
            throws XMLStreamException {

        switch (intMappingType) {
            case "UserId":
            case "RoleId":
                writer.writeAttribute(intAttrNameKey, "key");
                break;

            case "Username":
                writer.writeAttribute(intAttrNameKey, "username");
                break;

            case "Password":
                writer.writeAttribute(intAttrNameKey, "password");
                break;

            case "RoleName":
                writer.writeAttribute(intAttrNameKey, "name");
                break;

            case "RoleOwnerSchema":
                writer.writeAttribute(intAttrNameKey, "userOwner");
                break;

            default:
                if (StringUtils.isNotBlank(intAttrNameValue)) {
                    writer.writeAttribute(intAttrNameKey, intAttrNameValue);
                }
        }
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
        writer.writeAttribute("id", "USER");
        writer.writeAttribute("kind", "USER");
        writer.writeEndElement();

        writer.writeStartElement("AnyTypeClass");
        writer.writeAttribute("id", "BaseUser");
        writer.writeEndElement();

        writer.writeStartElement("AnyType_AnyTypeClass");
        writer.writeAttribute("anyType_id", "USER");
        writer.writeAttribute("anyTypeClass_id", "BaseUser");
        writer.writeEndElement();

        writer.writeStartElement("AnyType");
        writer.writeAttribute("id", "GROUP");
        writer.writeAttribute("kind", "GROUP");
        writer.writeEndElement();

        writer.writeStartElement("AnyTypeClass");
        writer.writeAttribute("id", "BaseGroup");
        writer.writeEndElement();

        writer.writeStartElement("AnyType_AnyTypeClass");
        writer.writeAttribute("anyType_id", "GROUP");
        writer.writeAttribute("anyTypeClass_id", "BaseGroup");
        writer.writeEndElement();

        writer.writeStartElement("AnyTypeClass");
        writer.writeAttribute("id", "BaseUMembership");
        writer.writeEndElement();

        Set<String> connInstanceCapabilities = new HashSet<>();

        String lastUUID;
        String syncopeConf = UUID.randomUUID().toString();
        Map<String, String> cPlainAttrs = new HashMap<>();
        Map<String, String> policies = new HashMap<>();
        Map<String, String> connInstances = new HashMap<>();
        Map<String, String> provisions = new HashMap<>();
        Map<String, String> mappings = new HashMap<>();
        Map<String, String> tasks = new HashMap<>();
        Map<String, String> notifications = new HashMap<>();
        Map<String, String> reports = new HashMap<>();

        String globalAccountPolicy = null;
        String globalPasswordPolicy = null;
        while (reader.hasNext()) {
            if (reader.isStartElement()) {
                switch (reader.getLocalName().toLowerCase()) {
                    case "syncopeconf":
                        writer.writeStartElement("SyncopeConf");
                        writer.writeAttribute("id", syncopeConf);
                        writer.writeEndElement();
                        break;

                    case "cschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeEndElement();
                        break;

                    case "cattr":
                        writer.writeStartElement("CPlainAttr");
                        copyAttrs(reader, writer, "owner_id", "schema_name");
                        lastUUID = UUID.randomUUID().toString();
                        writer.writeAttribute("id", lastUUID);
                        writer.writeAttribute("owner_id", syncopeConf);
                        writer.writeAttribute("schema_id", getAttributeValue(reader, "schema_name"));
                        writer.writeEndElement();
                        cPlainAttrs.put(getAttributeValue(reader, "id"), lastUUID);
                        break;

                    case "cattrvalue":
                        writer.writeStartElement("CPlainAttrValue");
                        copyAttrs(reader, writer, "attribute_id");
                        writer.writeAttribute("id", UUID.randomUUID().toString());
                        writer.writeAttribute(
                                "attribute_id", cPlainAttrs.get(getAttributeValue(reader, "attribute_id")));
                        writer.writeEndElement();
                        break;

                    case "uschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute("anyTypeClass_id", "BaseUser");
                        writer.writeEndElement();
                        break;

                    case "uderschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("DerSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute("anyTypeClass_id", "BaseUser");
                        writer.writeEndElement();
                        break;

                    case "uvirschema":
                        reporter.writeStartElement("VirSchema");
                        copyAttrs(reader, reporter);
                        reporter.writeAttribute("key", getAttributeValue(reader, "name"));
                        reporter.writeEndElement();
                        break;

                    case "rschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute("anyTypeClass_id", "BaseGroup");
                        writer.writeEndElement();
                        break;

                    case "rderschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("DerSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute("anyTypeClass_id", "BaseGroup");
                        writer.writeEndElement();
                        break;

                    case "rvirschema":
                        reporter.writeStartElement("VirSchema");
                        reporter.writeAttribute("key", getAttributeValue(reader, "name"));
                        copyAttrs(reader, reporter);
                        reporter.writeEndElement();
                        break;

                    case "mschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("PlainSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute("anyTypeClass_id", "BaseUMembership");
                        writer.writeEndElement();
                        break;

                    case "mderschema":
                        writer.writeStartElement("SyncopeSchema");
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));

                        writer.writeStartElement("DerSchema");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute("anyTypeClass_id", "BaseUMembership");
                        writer.writeEndElement();
                        break;

                    case "mvirschema":
                        reporter.writeStartElement("VirSchema");
                        copyAttrs(reader, reporter);
                        reporter.writeAttribute("key", getAttributeValue(reader, "name"));
                        reporter.writeEndElement();
                        break;

                    case "policy":
                        String policyId = getAttributeValue(reader, "id");
                        lastUUID = UUID.randomUUID().toString();
                        policies.put(policyId, lastUUID);

                        ObjectNode specification = (ObjectNode) OBJECT_MAPPER.readTree(
                                getAttributeValue(reader, "specification"));

                        switch (getAttributeValue(reader, "DTYPE")) {
                            case "SyncPolicy":
                                writer.writeStartElement("PullPolicy");
                                writer.writeAttribute("id", lastUUID);
                                writer.writeAttribute(
                                        "description", getAttributeValue(reader, "description"));
                                writer.writeEndElement();
                                break;

                            case "PasswordPolicy":
                                writer.writeStartElement("PasswordPolicy");
                                writer.writeAttribute("id", lastUUID);
                                writer.writeAttribute(
                                        "description", getAttributeValue(reader, "description"));

                                if ("GLOBAL_PASSWORD".equalsIgnoreCase(getAttributeValue(reader, "type"))) {
                                    globalPasswordPolicy = lastUUID;
                                }

                                JsonNode allowNullPassword = specification.get("allowNullPassword");
                                if (allowNullPassword != null) {
                                    writer.writeAttribute(
                                            "allowNullPassword", allowNullPassword.asBoolean() ? "1" : "0");
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
                                writer.writeAttribute("id", lastUUID);
                                writer.writeAttribute("passwordPolicy_id", lastUUID);
                                writer.writeAttribute("serializedInstance", specification.toString());
                                writer.writeEndElement();
                                break;

                            case "AccountPolicy":
                                writer.writeStartElement("AccountPolicy");
                                writer.writeAttribute("id", lastUUID);
                                writer.writeAttribute(
                                        "description", getAttributeValue(reader, "description"));

                                if ("GLOBAL_ACCOUNT".equalsIgnoreCase(getAttributeValue(reader, "type"))) {
                                    globalAccountPolicy = lastUUID;
                                }

                                JsonNode propagateSuspension = specification.get("propagateSuspension");
                                if (propagateSuspension != null) {
                                    writer.writeAttribute(
                                            "propagateSuspension", propagateSuspension.asBoolean() ? "1" : "0");
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
                                writer.writeAttribute("id", lastUUID);
                                writer.writeAttribute("accountPolicy_id", lastUUID);
                                writer.writeAttribute("serializedInstance", specification.toString());
                                writer.writeEndElement();
                                break;

                            default:
                        }
                        break;

                    case "conninstance":
                        lastUUID = UUID.randomUUID().toString();
                        connInstances.put(getAttributeValue(reader, "id"), lastUUID);

                        writer.writeStartElement("ConnInstance");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", lastUUID);
                        writer.writeEndElement();
                        break;

                    case "conninstance_capabilities":
                        String connInstanceId = getAttributeValue(reader, "connInstance_id");
                        String connInstanceKey = connInstances.get(connInstanceId);

                        String capabilities = getAttributeValue(reader, "capabilities");
                        if (capabilities.startsWith("ONE_PHASE_")) {
                            capabilities = capabilities.substring(10);
                        } else if (capabilities.startsWith("TWO_PHASES_")) {
                            capabilities = capabilities.substring(11);
                        }
                        if (!connInstanceCapabilities.contains(connInstanceId + capabilities)) {
                            writer.writeStartElement("ConnInstance_capabilities");
                            writer.writeAttribute("connInstance_id", connInstanceKey);
                            writer.writeAttribute("capability", capabilities);
                            writer.writeEndElement();

                            connInstanceCapabilities.add(connInstanceId + capabilities);
                        }
                        break;

                    case "externalresource":
                        writer.writeStartElement("ExternalResource");
                        copyAttrs(reader, writer,
                                "syncTraceLevel", "userializedSyncToken", "rserializedSyncToken",
                                "propagationMode", "propagationPrimary", "connector_id", "syncPolicy_id",
                                "passwordPolicy_id",
                                "creator", "lastModifier", "creationDate", "lastChangeDate");

                        writer.writeAttribute("id", getAttributeValue(reader, "name"));
                        writer.writeAttribute(
                                "connector_id", connInstances.get(getAttributeValue(reader, "connector_id")));

                        writer.writeAttribute("provisioningTraceLevel", getAttributeValue(reader, "syncTraceLevel"));

                        String syncPolicyKey = policies.get(getAttributeValue(reader, "syncPolicy_id"));
                        if (StringUtils.isNotBlank(syncPolicyKey)) {
                            writer.writeAttribute("pullPolicy_id", syncPolicyKey);
                        }

                        String passwordPolicyKey = policies.get(getAttributeValue(reader, "passwordPolicy_id"));
                        if (StringUtils.isNotBlank(passwordPolicyKey)) {
                            writer.writeAttribute("passwordPolicy_id", passwordPolicyKey);
                        }

                        writer.writeEndElement();
                        break;

                    case "externalresource_propactions":
                        writer.writeStartElement("ExternalResource_PropActions");

                        writer.writeAttribute("resource_id", getAttributeValue(reader, "externalResource_name"));

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
                                break;

                            default:
                        }
                        writer.writeAttribute("actionClassName", propActionClassName);
                        writer.writeEndElement();
                        break;

                    case "policy_externalresource":
                        writer.writeStartElement("AccountPolicy_ExternalResource");
                        writer.writeAttribute(
                                "accountPolicy_id", policies.get(getAttributeValue(reader, "account_policy_id")));
                        writer.writeAttribute("resource_id", getAttributeValue(reader, "resource_name"));
                        writer.writeEndElement();
                        break;

                    case "umapping":
                        String umappingId = getAttributeValue(reader, "id");
                        lastUUID = UUID.randomUUID().toString();
                        provisions.put(umappingId, lastUUID);

                        writer.writeStartElement("Provision");
                        writer.writeAttribute("id", lastUUID);
                        writer.writeAttribute("resource_id", getAttributeValue(reader, "resource_name"));
                        writer.writeAttribute("anyType_id", "USER");
                        writer.writeAttribute("objectClass", "__ACCOUNT__");
                        writer.writeEndElement();

                        lastUUID = UUID.randomUUID().toString();
                        mappings.put(umappingId, lastUUID);

                        writer.writeStartElement("Mapping");
                        writer.writeAttribute("id", lastUUID);
                        writer.writeAttribute("provision_id", provisions.get(umappingId));

                        String uaccountLink = getAttributeValue(reader, "accountlink");
                        if (StringUtils.isNotBlank(uaccountLink)) {
                            writer.writeAttribute("connObjectLink", uaccountLink);
                        }
                        writer.writeEndElement();
                        break;

                    case "umappingitem":
                        String uIntMappingType = getAttributeValue(reader, "intMappingType");
                        if (uIntMappingType.endsWith("VirtualSchema")) {
                            reporter.writeStartElement("MappingItem");
                            copyAttrs(reader, reporter, "accountid", "intMappingType");
                            reporter.writeEndElement();
                        } else {
                            writer.writeStartElement("MappingItem");
                            copyAttrs(reader, writer,
                                    "accountid", "intMappingType", "mapping_id", "intMappingType", "intAttrName");
                            writer.writeAttribute("id", UUID.randomUUID().toString());
                            writer.writeAttribute("mapping_id", mappings.
                                    get(getAttributeValue(reader, "mapping_id")));
                            writer.writeAttribute("connObjectKey", getAttributeValue(reader, "accountid"));

                            writeIntAttrName(
                                    uIntMappingType,
                                    "intAttrName",
                                    mappings.get(getAttributeValue(reader, "intAttrName")),
                                    writer);

                            writer.writeEndElement();
                        }
                        break;

                    case "rmapping":
                        String rmappingId = "10" + getAttributeValue(reader, "id");
                        lastUUID = UUID.randomUUID().toString();
                        provisions.put(rmappingId, lastUUID);

                        writer.writeStartElement("Provision");
                        writer.writeAttribute("id", lastUUID);
                        writer.writeAttribute("resource_id", getAttributeValue(reader, "resource_name"));
                        writer.writeAttribute("anyType_id", "GROUP");
                        writer.writeAttribute("objectClass", "__GROUP__");
                        writer.writeEndElement();

                        lastUUID = UUID.randomUUID().toString();
                        mappings.put(rmappingId, lastUUID);

                        writer.writeStartElement("Mapping");
                        writer.writeAttribute("id", lastUUID);
                        writer.writeAttribute("provision_id", provisions.get(rmappingId));

                        String raccountLink = getAttributeValue(reader, "accountlink");
                        if (StringUtils.isNotBlank(raccountLink)) {
                            writer.writeAttribute("connObjectLink", raccountLink);
                        }
                        writer.writeEndElement();
                        break;

                    case "rmappingitem":
                        String rIntMappingType = getAttributeValue(reader, "intMappingType");
                        if (rIntMappingType.endsWith("VirtualSchema")) {
                            reporter.writeStartElement("MappingItem");
                            copyAttrs(reader, reporter, "accountid", "intMappingType");
                            reporter.writeEndElement();
                        } else {
                            writer.writeStartElement("MappingItem");
                            copyAttrs(reader, writer,
                                    "accountid", "intMappingType", "mapping_id", "intAttrName");
                            writer.writeAttribute("id", UUID.randomUUID().toString());
                            writer.writeAttribute(
                                    "mapping_id", mappings.get("10" + getAttributeValue(reader, "mapping_id")));
                            writer.writeAttribute("connObjectKey", getAttributeValue(reader, "accountid"));

                            writeIntAttrName(
                                    rIntMappingType,
                                    "intAttrName",
                                    mappings.get(getAttributeValue(reader, "intAttrName")),
                                    writer);

                            writer.writeEndElement();
                        }
                        break;

                    case "task":
                        writer.writeStartElement("Task");
                        copyAttrs(reader, writer,
                                "DTYPE", "propagationMode", "subjectType", "subjectId", "xmlAttributes",
                                "jobClassName", "userTemplate", "roleTemplate", "userFilter", "roleFilter",
                                "propagationOperation", "syncStatus", "fullReconciliation", "resource_name");

                        lastUUID = UUID.randomUUID().toString();
                        tasks.put(getAttributeValue(reader, "id"), lastUUID);

                        writer.writeAttribute("id", lastUUID);

                        String resourceName = getAttributeValue(reader, "resource_name");
                        if (StringUtils.isNotBlank(resourceName)) {
                            writer.writeAttribute("resource_id", resourceName);
                        }

                        String name = getAttributeValue(reader, "name");
                        if (StringUtils.isNotBlank(name)) {
                            writer.writeAttribute("name", name);
                        }

                        switch (getAttributeValue(reader, "DTYPE")) {
                            case "PropagationTask":
                                writer.writeAttribute("DTYPE", "PropagationTask");
                                writer.writeAttribute(
                                        "anyTypeKind", getAttributeValue(reader, "subjectType"));
                                writer.writeAttribute(
                                        "anyKey", getAttributeValue(reader, "subjectId"));
                                writer.writeAttribute(
                                        "attributes", getAttributeValue(reader, "xmlAttributes"));
                                writer.writeAttribute(
                                        "operation", getAttributeValue(reader, "propagationOperation"));
                                writer.writeEndElement();
                                break;

                            case "SyncTask":
                                writer.writeAttribute("DTYPE", "PullTask");
                                writer.writeAttribute("syncStatus", getAttributeValue(reader, "syncStatus"));

                                String fullReconciliation = getAttributeValue(reader, "fullReconciliation");
                                if ("1".equals(fullReconciliation)) {
                                    writer.writeAttribute("pullMode", "FULL_RECONCILIATION");
                                } else if ("0".equals(fullReconciliation)) {
                                    writer.writeAttribute("pullMode", "INCREMENTAL");
                                }

                                writer.writeEndElement();

                                String userTemplate = getAttributeValue(reader, "userTemplate");
                                if (StringUtils.isNotBlank(userTemplate)) {
                                    ObjectNode template = (ObjectNode) OBJECT_MAPPER.readTree(userTemplate);
                                    JsonNode plainAttrs = template.remove("attrs");
                                    template.set("plainAttrs", plainAttrs);

                                    writer.writeStartElement("AnyTemplatePullTask");
                                    writer.writeAttribute("id", UUID.randomUUID().toString());
                                    writer.writeAttribute("pullTask_id", lastUUID);
                                    writer.writeAttribute("anyType_id", "USER");
                                    writer.writeAttribute("template", template.toString());
                                    writer.writeEndElement();
                                }
                                String roleTemplate = getAttributeValue(reader, "roleTemplate");
                                if (StringUtils.isNotBlank(roleTemplate)) {
                                    ObjectNode template = (ObjectNode) OBJECT_MAPPER.readTree(roleTemplate);
                                    JsonNode plainAttrs = template.remove("attrs");
                                    template.set("plainAttrs", plainAttrs);

                                    writer.writeStartElement("AnyTemplatePullTask");
                                    writer.writeAttribute("id", UUID.randomUUID().toString());
                                    writer.writeAttribute("pullTask_id", lastUUID);
                                    writer.writeAttribute("anyType_id", "GROUP");
                                    writer.writeAttribute("template", template.toString());
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
                                if (StringUtils.isNotBlank(userFilter)) {
                                    writer.writeStartElement("PushTaskAnyFilter");
                                    writer.writeAttribute("id", UUID.randomUUID().toString());
                                    writer.writeAttribute("pushTask_id", lastUUID);
                                    writer.writeAttribute("anyType_id", "USER");
                                    writer.writeAttribute("fiql", userFilter);
                                    writer.writeEndElement();
                                }
                                String roleFilter = getAttributeValue(reader, "roleFilter");
                                if (StringUtils.isNotBlank(roleFilter)) {
                                    writer.writeStartElement("PushTaskAnyFilter");
                                    writer.writeAttribute("id", UUID.randomUUID().toString());
                                    writer.writeAttribute("pushTask_id", lastUUID);
                                    writer.writeAttribute("anyType_id", "GROUP");
                                    writer.writeAttribute("fiql", roleFilter);
                                    writer.writeEndElement();
                                }
                                break;

                            default:
                        }
                        break;

                    case "taskexec":
                        writer.writeStartElement("TaskExec");
                        copyAttrs(reader, writer, "task_id");
                        writer.writeAttribute("id", UUID.randomUUID().toString());
                        writer.writeAttribute("task_id", tasks.get(getAttributeValue(reader, "task_id")));
                        writer.writeEndElement();
                        break;

                    case "synctask_actionsclassnames":
                        writer.writeStartElement("PullTask_actionsClassNames");
                        writer.writeAttribute(
                                "pullTask_id", tasks.get(getAttributeValue(reader, "syncTask_id")));

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
                                break;

                            default:
                        }
                        writer.writeAttribute("actionClassName", syncActionClassName);
                        writer.writeEndElement();
                        break;

                    case "notification":
                        writer.writeStartElement("Notification");

                        lastUUID = UUID.randomUUID().toString();
                        notifications.put(getAttributeValue(reader, "id"), lastUUID);

                        writer.writeAttribute("id", lastUUID);

                        copyAttrs(reader, writer,
                                "recipientAttrType", "template", "userAbout", "roleAbout", "recipients",
                                "recipientAttrName");

                        String recipientAttrType = getAttributeValue(reader, "recipientAttrType");
                        writeIntAttrName(
                                recipientAttrType,
                                "recipientAttrName",
                                mappings.get(getAttributeValue(reader, "recipientAttrName")),
                                writer);

                        String recipients = getAttributeValue(reader, "recipients");
                        if (StringUtils.isNotBlank(recipients)) {
                            writer.writeAttribute("recipientsFIQL", getAttributeValue(reader, "recipients"));
                        }
                        writer.writeAttribute("template_id", getAttributeValue(reader, "template"));
                        writer.writeEndElement();

                        String userAbout = getAttributeValue(reader, "userAbout");
                        if (StringUtils.isNotBlank(userAbout)) {
                            writer.writeStartElement("AnyAbout");
                            writer.writeAttribute("id", UUID.randomUUID().toString());
                            writer.writeAttribute("notification_id", lastUUID);
                            writer.writeAttribute("anyType_id", "USER");
                            writer.writeAttribute("filter", userAbout);
                            writer.writeEndElement();
                        }
                        String roleAbout = getAttributeValue(reader, "roleAbout");
                        if (StringUtils.isNotBlank(roleAbout)) {
                            writer.writeStartElement("AnyAbout");
                            writer.writeAttribute("id", UUID.randomUUID().toString());
                            writer.writeAttribute("notification_id", lastUUID);
                            writer.writeAttribute("anyType_id", "GROUP");
                            writer.writeAttribute("filter", roleAbout);
                            writer.writeEndElement();
                        }
                        break;

                    case "notification_events":
                        writer.writeStartElement("Notification_events");
                        copyAttrs(reader, writer, "notification_id", "events");
                        writer.writeAttribute(
                                "notification_id", notifications.get(getAttributeValue(reader, "notification_id")));
                        writer.writeAttribute(
                                "event", getAttributeValue(reader, "events").
                                replaceAll("Controller", "Logic"));
                        writer.writeEndElement();
                        break;

                    case "notificationtask_recipients":
                        writer.writeStartElement("NotificationTask_recipients");
                        copyAttrs(reader, writer, "notificationTask_id");
                        writer.writeAttribute(
                                "notificationTask_id",
                                tasks.get(getAttributeValue(reader, "notificationTask_id")));
                        writer.writeEndElement();
                        break;

                    case "report":
                        writer.writeStartElement("Report");
                        copyAttrs(reader, writer);

                        lastUUID = UUID.randomUUID().toString();
                        reports.put(getAttributeValue(reader, "id"), lastUUID);

                        writer.writeAttribute("id", lastUUID);
                        writer.writeAttribute("name", getAttributeValue(reader, "name"));

                        writer.writeEndElement();
                        break;

                    case "reportletconfinstance":
                        writer.writeStartElement("ReportletConfInstance");
                        copyAttrs(reader, writer, "report_id");

                        writer.writeAttribute("id", UUID.randomUUID().toString());
                        writer.writeAttribute("report_id", reports.get(getAttributeValue(reader, "report_id")));

                        writer.writeEndElement();
                        break;

                    case "reportexec":
                        writer.writeStartElement("ReportExec");
                        copyAttrs(reader, writer, "report_id");

                        writer.writeAttribute("id", UUID.randomUUID().toString());
                        writer.writeAttribute("report_id", reports.get(getAttributeValue(reader, "report_id")));

                        writer.writeEndElement();
                        break;

                    case "securityquestion":
                        writer.writeStartElement("SecurityQuestion");
                        copyAttrs(reader, writer);
                        writer.writeAttribute("id", UUID.randomUUID().toString());
                        writer.writeEndElement();
                        break;

                    default:
                }
            }

            reader.next();
        }

        writer.writeStartElement("Realm");
        writer.writeAttribute("id", UUID.randomUUID().toString());
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
