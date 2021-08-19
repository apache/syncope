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
package org.apache.syncope.core.provisioning.java.job.report;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.report.ReconciliationReportletConf;
import org.apache.syncope.common.lib.report.ReconciliationReportletConf.Feature;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Reportlet for extracting information for the current reconciliation status, e.g. the coherence between Syncope
 * information and mapped entities on external resources.
 */
@ReportletConfClass(ReconciliationReportletConf.class)
public class ReconciliationReportlet extends AbstractReportlet {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private ConnectorManager connectorManager;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private SearchCondVisitor searchCondVisitor;

    private ReconciliationReportletConf conf;

    private static String getAnyElementName(final AnyTypeKind anyTypeKind) {
        String elementName;

        switch (anyTypeKind) {
            case USER:
                elementName = "user";
                break;

            case GROUP:
                elementName = "group";
                break;

            case ANY_OBJECT:
            default:
                elementName = "anyObject";
        }

        return elementName;
    }

    private void doExtract(
            final ContentHandler handler,
            final Any<?> any,
            final Set<Missing> missing,
            final Set<Misaligned> misaligned)
            throws SAXException {

        AttributesImpl atts = new AttributesImpl();

        for (Feature feature : conf.getFeatures()) {
            String type = null;
            String value = null;
            switch (feature) {
                case key:
                    type = ReportXMLConst.XSD_STRING;
                    value = any.getKey();
                    break;

                case username:
                    if (any instanceof User) {
                        type = ReportXMLConst.XSD_STRING;
                        value = User.class.cast(any).getUsername();
                    }
                    break;

                case groupName:
                    if (any instanceof Group) {
                        type = ReportXMLConst.XSD_STRING;
                        value = Group.class.cast(any).getName();
                    }
                    break;

                case status:
                    type = ReportXMLConst.XSD_STRING;
                    value = any.getStatus();
                    break;

                case creationDate:
                    type = ReportXMLConst.XSD_DATETIME;
                    value = any.getCreationDate() == null
                            ? StringUtils.EMPTY
                            : FormatUtils.format(any.getCreationDate());
                    break;

                case lastLoginDate:
                    if (any instanceof User) {
                        type = ReportXMLConst.XSD_DATETIME;
                        value = User.class.cast(any).getLastLoginDate() == null
                                ? StringUtils.EMPTY
                                : FormatUtils.format(User.class.cast(any).getLastLoginDate());
                    }
                    break;

                case changePwdDate:
                    if (any instanceof User) {
                        type = ReportXMLConst.XSD_DATETIME;
                        value = User.class.cast(any).getChangePwdDate() == null
                                ? StringUtils.EMPTY
                                : FormatUtils.format(User.class.cast(any).getChangePwdDate());
                    }
                    break;

                case passwordHistorySize:
                    if (any instanceof User) {
                        type = ReportXMLConst.XSD_INT;
                        value = String.valueOf(User.class.cast(any).getPasswordHistory().size());
                    }
                    break;

                case failedLoginCount:
                    if (any instanceof User) {
                        type = ReportXMLConst.XSD_INT;
                        value = String.valueOf(User.class.cast(any).getFailedLogins());
                    }
                    break;

                default:
            }

            if (type != null && value != null) {
                atts.addAttribute("", "", feature.name(), type, value);
            }
        }

        handler.startElement("", "", getAnyElementName(any.getType().getKind()), atts);

        for (Missing item : missing) {
            atts.clear();
            atts.addAttribute("", "", "resource", ReportXMLConst.XSD_STRING, item.getResource());
            atts.addAttribute("", "", "connObjectKeyValue", ReportXMLConst.XSD_STRING, item.getConnObjectKeyValue());

            handler.startElement("", "", "missing", atts);
            handler.endElement("", "", "missing");
        }
        for (Misaligned item : misaligned) {
            atts.clear();
            atts.addAttribute("", "", "resource", ReportXMLConst.XSD_STRING, item.getResource());
            atts.addAttribute("", "", "connObjectKeyValue", ReportXMLConst.XSD_STRING, item.getConnObjectKeyValue());
            atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, item.getName());

            handler.startElement("", "", "misaligned", atts);

            handler.startElement("", "", "onSyncope", null);
            if (item.getOnSyncope() != null) {
                for (Object value : item.getOnSyncope()) {
                    char[] asChars = value.toString().toCharArray();

                    handler.startElement("", "", "value", null);
                    handler.characters(asChars, 0, asChars.length);
                    handler.endElement("", "", "value");
                }
            }
            handler.endElement("", "", "onSyncope");

            handler.startElement("", "", "onResource", null);
            if (item.getOnResource() != null) {
                for (Object value : item.getOnResource()) {
                    char[] asChars = value.toString().toCharArray();

                    handler.startElement("", "", "value", null);
                    handler.characters(asChars, 0, asChars.length);
                    handler.endElement("", "", "value");
                }
            }
            handler.endElement("", "", "onResource");

            handler.endElement("", "", "misaligned");
        }

        handler.endElement("", "", getAnyElementName(any.getType().getKind()));
    }

    private static Set<Object> getValues(final Attribute attr) {
        Set<Object> values;
        if (attr.getValue() == null || attr.getValue().isEmpty()) {
            values = Set.of();
        } else if (attr.getValue().get(0) instanceof byte[]) {
            values = new HashSet<>(attr.getValue().size());
            attr.getValue().forEach(single -> values.add(Base64.getEncoder().encode((byte[]) single)));
        } else {
            values = new HashSet<>(attr.getValue());
        }

        return values;
    }

    private void doExtract(final ContentHandler handler, final List<? extends Any<?>> anys)
            throws SAXException, ReportException {

        final Set<Missing> missing = new HashSet<>();
        final Set<Misaligned> misaligned = new HashSet<>();

        for (Any<?> any : anys) {
            missing.clear();
            misaligned.clear();

            AnyUtils anyUtils = anyUtilsFactory.getInstance(any);
            anyUtils.getAllResources(any).forEach(resource -> {
                Provision provision = resource.getProvision(any.getType()).orElse(null);
                Optional<? extends MappingItem> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
                final String connObjectKeyValue = connObjectKeyItem.isPresent()
                        ? mappingManager.getConnObjectKeyValue(any, provision).get()
                        : StringUtils.EMPTY;
                if (provision != null && connObjectKeyItem.isPresent() && StringUtils.isNotBlank(connObjectKeyValue)) {
                    // 1. read from the underlying connector
                    Connector connector = connectorManager.getConnector(resource);
                    ConnectorObject connectorObject = connector.getObject(
                            provision.getObjectClass(),
                            AttributeBuilder.build(connObjectKeyItem.get().getExtAttrName(), connObjectKeyValue),
                            provision.isIgnoreCaseMatch(),
                            MappingUtils.buildOperationOptions(provision.getMapping().getItems().stream()));

                    if (connectorObject == null) {
                        // 2. not found on resource?
                        LOG.error("Object {} with class {} not found on resource {}",
                                connObjectKeyValue, provision.getObjectClass(), resource);

                        missing.add(new Missing(resource.getKey(), connObjectKeyValue));
                    } else {
                        // 3. found but misaligned?
                        Pair<String, Set<Attribute>> preparedAttrs =
                                mappingManager.prepareAttrsFromAny(any, null, false, null, provision);
                        preparedAttrs.getRight().add(AttributeBuilder.build(
                                Uid.NAME, preparedAttrs.getLeft()));
                        preparedAttrs.getRight().add(AttributeBuilder.build(
                                connObjectKeyItem.get().getExtAttrName(), preparedAttrs.getLeft()));

                        final Map<String, Set<Object>> syncopeAttrs = new HashMap<>();
                        preparedAttrs.getRight().forEach(attr -> syncopeAttrs.put(attr.getName(), getValues(attr)));

                        final Map<String, Set<Object>> resourceAttrs = new HashMap<>();
                        connectorObject.getAttributes().stream().
                                filter(attr -> (!OperationalAttributes.PASSWORD_NAME.equals(attr.getName())
                                && !OperationalAttributes.ENABLE_NAME.equals(attr.getName()))).
                                forEachOrdered(attr -> resourceAttrs.put(attr.getName(), getValues(attr)));

                        syncopeAttrs.keySet().stream().
                                filter(syncopeAttr -> !resourceAttrs.containsKey(syncopeAttr)).
                                forEach(name -> misaligned.add(new Misaligned(
                                resource.getKey(),
                                connObjectKeyValue,
                                name,
                                syncopeAttrs.get(name),
                                Set.of())));

                        resourceAttrs.forEach((key, values) -> {
                            if (syncopeAttrs.containsKey(key)) {
                                if (!Objects.equals(syncopeAttrs.get(key), values)) {
                                    misaligned.add(new Misaligned(
                                            resource.getKey(),
                                            connObjectKeyValue,
                                            key,
                                            syncopeAttrs.get(key),
                                            values));
                                }
                            } else {
                                misaligned.add(new Misaligned(
                                        resource.getKey(),
                                        connObjectKeyValue,
                                        key,
                                        Set.of(),
                                        values));
                            }
                        });
                    }
                }
            });

            if (!missing.isEmpty() || !misaligned.isEmpty()) {
                doExtract(handler, any, missing, misaligned);
            }
        }
    }

    @Override
    protected void doExtract(
            final ReportletConf conf,
            final ContentHandler handler,
            final AtomicReference<String> status)
            throws SAXException {

        if (conf instanceof ReconciliationReportletConf) {
            this.conf = ReconciliationReportletConf.class.cast(conf);
        } else {
            throw new ReportException(new IllegalArgumentException("Invalid configuration provided"));
        }

        AttributesImpl atts = new AttributesImpl();

        if (StringUtils.isBlank(this.conf.getUserMatchingCond())) {
            int total = userDAO.count();
            int pages = (total / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            status.set("Processing " + total + " users in " + pages + " pages");

            atts.addAttribute("", "", "total", ReportXMLConst.XSD_INT, String.valueOf(total));
            handler.startElement("", "", getAnyElementName(AnyTypeKind.USER) + 's', atts);

            for (int page = 1; page <= pages; page++) {
                status.set("Processing " + total + " users: page " + page + " of " + pages);

                doExtract(handler, userDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE));
            }
        } else {
            SearchCond cond = SearchCondConverter.convert(searchCondVisitor, this.conf.getUserMatchingCond());

            int total = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, cond, AnyTypeKind.USER);
            int pages = (total / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            status.set("Processing " + total + " users in " + pages + " pages");

            atts.addAttribute("", "", "total", ReportXMLConst.XSD_INT, String.valueOf(total));
            handler.startElement("", "", getAnyElementName(AnyTypeKind.USER) + 's', atts);

            for (int page = 1; page <= pages; page++) {
                status.set("Processing " + total + " users: page " + page + " of " + pages);

                doExtract(handler, searchDAO.search(
                        SyncopeConstants.FULL_ADMIN_REALMS,
                        cond,
                        page,
                        PAGE_SIZE,
                        List.of(),
                        AnyTypeKind.USER));
            }
        }
        handler.endElement("", "", getAnyElementName(AnyTypeKind.USER) + 's');

        atts.clear();
        if (StringUtils.isBlank(this.conf.getGroupMatchingCond())) {
            int total = groupDAO.count();
            int pages = (total / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            status.set("Processing " + total + " groups in " + pages + " pages");

            atts.addAttribute("", "", "total", ReportXMLConst.XSD_INT, String.valueOf(total));
            handler.startElement("", "", getAnyElementName(AnyTypeKind.GROUP) + 's', atts);

            for (int page = 1; page <= pages; page++) {
                status.set("Processing " + total + " groups: page " + page + " of " + pages);

                doExtract(handler, groupDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE));
            }
        } else {
            SearchCond cond = SearchCondConverter.convert(searchCondVisitor, this.conf.getUserMatchingCond());

            int total = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, cond, AnyTypeKind.GROUP);
            int pages = (total / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

            status.set("Processing " + total + " groups in " + pages + " pages");

            atts.addAttribute("", "", "total", ReportXMLConst.XSD_INT, String.valueOf(total));
            handler.startElement("", "", getAnyElementName(AnyTypeKind.GROUP) + 's', atts);

            for (int page = 1; page <= pages; page++) {
                status.set("Processing " + total + " groups: page " + page + " of " + pages);

                doExtract(handler, searchDAO.search(
                        SyncopeConstants.FULL_ADMIN_REALMS,
                        cond,
                        page,
                        PAGE_SIZE,
                        List.of(),
                        AnyTypeKind.GROUP));
            }
        }
        handler.endElement("", "", getAnyElementName(AnyTypeKind.GROUP) + 's');

        for (AnyType anyType : anyTypeDAO.findAll()) {
            if (!anyType.equals(anyTypeDAO.findUser()) && !anyType.equals(anyTypeDAO.findGroup())) {
                AnyTypeCond anyTypeCond = new AnyTypeCond();
                anyTypeCond.setAnyTypeKey(anyType.getKey());
                SearchCond cond = StringUtils.isBlank(this.conf.getAnyObjectMatchingCond())
                        ? SearchCond.getLeaf(anyTypeCond)
                        : SearchCond.getAnd(
                                SearchCond.getLeaf(anyTypeCond),
                                SearchCondConverter.convert(searchCondVisitor, this.conf.getAnyObjectMatchingCond()));

                int total = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, cond, AnyTypeKind.ANY_OBJECT);
                int pages = (total / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

                status.set("Processing " + total + " any objects " + anyType.getKey() + " in " + pages + " pages");

                atts.clear();
                atts.addAttribute("", "", "type", ReportXMLConst.XSD_STRING, anyType.getKey());
                atts.addAttribute("", "", "total", ReportXMLConst.XSD_INT, String.valueOf(total));
                handler.startElement("", "", getAnyElementName(AnyTypeKind.ANY_OBJECT) + 's', atts);

                for (int page = 1; page <= pages; page++) {
                    status.set("Processing " + total + " any objects " + anyType.getKey()
                            + ": page " + page + " of " + pages);

                    doExtract(handler, searchDAO.search(
                            SyncopeConstants.FULL_ADMIN_REALMS,
                            cond,
                            page,
                            PAGE_SIZE,
                            List.of(),
                            AnyTypeKind.ANY_OBJECT));
                }

                handler.endElement("", "", getAnyElementName(AnyTypeKind.ANY_OBJECT) + 's');
            }
        }
    }

    private static class Missing {

        private final String resource;

        private final String connObjectKeyValue;

        Missing(final String resource, final String connObjectKeyValue) {
            this.resource = resource;
            this.connObjectKeyValue = connObjectKeyValue;
        }

        public String getResource() {
            return resource;
        }

        public String getConnObjectKeyValue() {
            return connObjectKeyValue;
        }

    }

    private static class Misaligned extends Missing {

        private final String name;

        private final Set<Object> onSyncope;

        private final Set<Object> onResource;

        Misaligned(
                final String resource,
                final String connObjectKeyValue,
                final String name,
                final Set<Object> onSyncope,
                final Set<Object> onResource) {

            super(resource, connObjectKeyValue);

            this.name = name;
            this.onSyncope = onSyncope;
            this.onResource = onResource;
        }

        public String getName() {
            return name;
        }

        public Set<Object> getOnSyncope() {
            return onSyncope;
        }

        public Set<Object> getOnResource() {
            return onResource;
        }

    }
}
