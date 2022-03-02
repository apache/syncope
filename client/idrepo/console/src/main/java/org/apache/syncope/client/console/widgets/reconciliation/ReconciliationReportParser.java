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
package org.apache.syncope.client.console.widgets.reconciliation;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public final class ReconciliationReportParser {

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    public static ReconciliationReport parse(final OffsetDateTime run, final InputStream in)
            throws XMLStreamException, NumberFormatException {

        XMLStreamReader streamReader = XML_INPUT_FACTORY.createXMLStreamReader(in);
        streamReader.nextTag(); // root
        streamReader.nextTag(); // report
        streamReader.nextTag(); // reportlet

        ReconciliationReport report = new ReconciliationReport(run);

        List<Missing> missing = new ArrayList<>();
        List<Misaligned> misaligned = new ArrayList<>();
        Set<String> onSyncope = null;
        Set<String> onResource = null;

        Any user = null;
        Any group = null;
        Any anyObject = null;
        String lastAnyType = null;
        while (streamReader.hasNext()) {
            if (streamReader.isStartElement()) {
                switch (streamReader.getLocalName()) {
                    case "users":
                        Anys users = new Anys();
                        users.setTotal(Integer.parseInt(streamReader.getAttributeValue("", "total")));
                        report.setUsers(users);
                        break;

                    case "user":
                        user = new Any();
                        user.setType(AnyTypeKind.USER.name());
                        user.setKey(streamReader.getAttributeValue("", Constants.KEY_FIELD_NAME));
                        user.setName(streamReader.getAttributeValue("", "username"));
                        report.getUsers().getAnys().add(user);
                        break;

                    case "groups":
                        Anys groups = new Anys();
                        groups.setTotal(Integer.parseInt(streamReader.getAttributeValue("", "total")));
                        report.setGroups(groups);
                        break;

                    case "group":
                        group = new Any();
                        group.setType(AnyTypeKind.GROUP.name());
                        group.setKey(streamReader.getAttributeValue("", Constants.KEY_FIELD_NAME));
                        group.setName(streamReader.getAttributeValue("", "groupName"));
                        report.getGroups().getAnys().add(group);
                        break;

                    case "anyObjects":
                        lastAnyType = streamReader.getAttributeValue("", "type");
                        Anys anyObjects = new Anys();
                        anyObjects.setAnyType(lastAnyType);
                        anyObjects.setTotal(Integer.parseInt(streamReader.getAttributeValue("", "total")));
                        report.getAnyObjects().add(anyObjects);
                        break;

                    case "anyObject":
                        anyObject = new Any();
                        anyObject.setType(lastAnyType);
                        anyObject.setKey(streamReader.getAttributeValue("", Constants.KEY_FIELD_NAME));
                        final String anyType = lastAnyType;
                        Optional<Anys> anyReport = report.getAnyObjects().stream().
                                filter(anys -> anyType.equals(anys.getAnyType())).
                                findFirst();
                        if (anyReport.isPresent()) {
                            anyReport.get().getAnys().add(anyObject);
                        }
                        break;

                    case "missing":
                        missing.add(new Missing(
                                streamReader.getAttributeValue("", "resource"),
                                streamReader.getAttributeValue("", "connObjectKeyValue")));
                        break;

                    case "misaligned":
                        misaligned.add(new Misaligned(
                                streamReader.getAttributeValue("", "resource"),
                                streamReader.getAttributeValue("", "connObjectKeyValue"),
                                streamReader.getAttributeValue("", "name")));
                        break;

                    case "onSyncope":
                        onSyncope = new HashSet<>();
                        break;

                    case "onResource":
                        onResource = new HashSet<>();
                        break;

                    case "value":
                        Set<String> set = Optional.ofNullable(onSyncope).orElse(onResource);
                        set.add(streamReader.getElementText());
                        break;

                    default:
                }
            } else if (streamReader.isEndElement()) {
                switch (streamReader.getLocalName()) {
                    case "user":
                        Optional.ofNullable(user).ifPresent(u -> {
                            u.getMissing().addAll(missing);
                            u.getMisaligned().addAll(misaligned);
                        });
                        missing.clear();
                        misaligned.clear();
                        break;

                    case "group":
                        Optional.ofNullable(group).ifPresent(g -> {
                            g.getMissing().addAll(missing);
                            g.getMisaligned().addAll(misaligned);
                        });
                        missing.clear();
                        misaligned.clear();
                        break;

                    case "anyObject":
                        Optional.ofNullable(anyObject).ifPresent(a -> {
                            a.getMissing().addAll(missing);
                            a.getMisaligned().addAll(misaligned);
                        });
                        missing.clear();
                        misaligned.clear();
                        break;

                    case "onSyncope":
                        misaligned.get(misaligned.size() - 1).getOnSyncope().addAll(onSyncope);
                        onSyncope = null;
                        break;

                    case "onResource":
                        misaligned.get(misaligned.size() - 1).getOnResource().addAll(onResource);
                        onResource = null;
                        break;

                    default:
                }

            }

            streamReader.next();
        }

        return report;
    }

    private ReconciliationReportParser() {
        // private constructor for static utility class
    }
}
