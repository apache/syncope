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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public final class ReconciliationReportParser {

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    public static ReconciliationReport parse(final Date run, final InputStream in) throws XMLStreamException {
        XMLStreamReader streamReader = INPUT_FACTORY.createXMLStreamReader(in);
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
                        user.setKey(streamReader.getAttributeValue("", "key"));
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
                        group.setKey(streamReader.getAttributeValue("", "key"));
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
                        anyObject.setKey(streamReader.getAttributeValue("", "key"));
                        final String anyType = lastAnyType;
                        IterableUtils.find(report.getAnyObjects(), new Predicate<Anys>() {

                            @Override
                            public boolean evaluate(final Anys anys) {
                                return anyType.equals(anys.getAnyType());
                            }
                        }).getAnys().add(anyObject);
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
                        Set<String> set = onSyncope == null ? onResource : onSyncope;
                        set.add(streamReader.getElementText());
                        break;

                    default:
                }
            } else if (streamReader.isEndElement()) {
                switch (streamReader.getLocalName()) {
                    case "user":
                        user.getMissing().addAll(missing);
                        user.getMisaligned().addAll(misaligned);
                        missing.clear();
                        misaligned.clear();
                        break;

                    case "group":
                        group.getMissing().addAll(missing);
                        group.getMisaligned().addAll(misaligned);
                        missing.clear();
                        misaligned.clear();
                        break;

                    case "anyObject":
                        anyObject.getMissing().addAll(missing);
                        anyObject.getMisaligned().addAll(misaligned);
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
