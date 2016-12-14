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
package org.apache.syncope.core.logic.report;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.report.GroupReportletConf;
import org.apache.syncope.common.lib.report.GroupReportletConf.Feature;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.ReportletConfClass;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@ReportletConfClass(GroupReportletConf.class)
public class GroupReportlet extends AbstractReportlet {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private GroupDataBinder groupDataBinder;

    private GroupReportletConf conf;

    private void doExtractResources(final ContentHandler handler, final AnyTO anyTO)
            throws SAXException {

        if (anyTO.getResources().isEmpty()) {
            LOG.debug("No resources found for {}[{}]", anyTO.getClass().getSimpleName(), anyTO.getKey());
        } else {
            AttributesImpl atts = new AttributesImpl();
            handler.startElement("", "", "resources", null);

            for (String resourceName : anyTO.getResources()) {
                atts.clear();

                atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, resourceName);
                handler.startElement("", "", "resource", atts);
                handler.endElement("", "", "resource");
            }

            handler.endElement("", "", "resources");
        }
    }

    private void doExtractAttributes(final ContentHandler handler, final AnyTO anyTO,
            final Collection<String> attrs, final Collection<String> derAttrs, final Collection<String> virAttrs)
            throws SAXException {

        AttributesImpl atts = new AttributesImpl();
        if (!attrs.isEmpty()) {
            Map<String, AttrTO> attrMap = anyTO.getPlainAttrMap();

            handler.startElement("", "", "attributes", null);
            for (String attrName : attrs) {
                atts.clear();

                atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, attrName);
                handler.startElement("", "", "attribute", atts);

                if (attrMap.containsKey(attrName)) {
                    for (String value : attrMap.get(attrName).getValues()) {
                        handler.startElement("", "", "value", null);
                        handler.characters(value.toCharArray(), 0, value.length());
                        handler.endElement("", "", "value");
                    }
                } else {
                    LOG.debug("{} not found for {}[{}]", attrName,
                            anyTO.getClass().getSimpleName(), anyTO.getKey());
                }

                handler.endElement("", "", "attribute");
            }
            handler.endElement("", "", "attributes");
        }

        if (!derAttrs.isEmpty()) {
            Map<String, AttrTO> derAttrMap = anyTO.getDerAttrMap();

            handler.startElement("", "", "derivedAttributes", null);
            for (String attrName : derAttrs) {
                atts.clear();

                atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, attrName);
                handler.startElement("", "", "derivedAttribute", atts);

                if (derAttrMap.containsKey(attrName)) {
                    for (String value : derAttrMap.get(attrName).getValues()) {
                        handler.startElement("", "", "value", null);
                        handler.characters(value.toCharArray(), 0, value.length());
                        handler.endElement("", "", "value");
                    }
                } else {
                    LOG.debug("{} not found for {}[{}]", attrName,
                            anyTO.getClass().getSimpleName(), anyTO.getKey());
                }

                handler.endElement("", "", "derivedAttribute");
            }
            handler.endElement("", "", "derivedAttributes");
        }

        if (!virAttrs.isEmpty()) {
            Map<String, AttrTO> virAttrMap = anyTO.getVirAttrMap();

            handler.startElement("", "", "virtualAttributes", null);
            for (String attrName : virAttrs) {
                atts.clear();

                atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, attrName);
                handler.startElement("", "", "virtualAttribute", atts);

                if (virAttrMap.containsKey(attrName)) {
                    for (String value : virAttrMap.get(attrName).getValues()) {
                        handler.startElement("", "", "value", null);
                        handler.characters(value.toCharArray(), 0, value.length());
                        handler.endElement("", "", "value");
                    }
                } else {
                    LOG.debug("{} not found for {}[{}]", attrName,
                            anyTO.getClass().getSimpleName(), anyTO.getKey());
                }

                handler.endElement("", "", "virtualAttribute");
            }
            handler.endElement("", "", "virtualAttributes");
        }
    }

    private void doExtract(final ContentHandler handler, final List<Group> groups) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        for (Group group : groups) {
            atts.clear();

            for (Feature feature : conf.getFeatures()) {
                String type = null;
                String value = null;
                switch (feature) {
                    case key:
                        type = ReportXMLConst.XSD_STRING;
                        value = group.getKey();
                        break;

                    case name:
                        type = ReportXMLConst.XSD_STRING;
                        value = String.valueOf(group.getName());
                        break;

                    case groupOwner:
                        type = ReportXMLConst.XSD_STRING;
                        value = group.getGroupOwner().getKey();
                        break;

                    case userOwner:
                        type = ReportXMLConst.XSD_STRING;
                        value = group.getUserOwner().getKey();
                        break;

                    default:
                }

                if (type != null && value != null) {
                    atts.addAttribute("", "", feature.name(), type, value);
                }
            }

            handler.startElement("", "", "group", atts);

            // Using GroupTO for attribute values, since the conversion logic of
            // values to String is already encapsulated there
            GroupTO groupTO = groupDataBinder.getGroupTO(group, true);

            doExtractAttributes(handler, groupTO, conf.getPlainAttrs(), conf.getDerAttrs(), conf.getVirAttrs());

            // to get resources associated to a group
            if (conf.getFeatures().contains(Feature.resources)) {
                doExtractResources(handler, groupTO);
            }
            //to get users asscoiated to a group is preferred GroupDAO to GroupTO
            if (conf.getFeatures().contains(Feature.users)) {
                handler.startElement("", "", "users", null);

                for (UMembership memb : groupDAO.findUMemberships(group)) {
                    atts.clear();

                    atts.addAttribute("", "", "key", ReportXMLConst.XSD_STRING,
                            memb.getLeftEnd().getKey());
                    atts.addAttribute("", "", "username", ReportXMLConst.XSD_STRING,
                            String.valueOf(memb.getLeftEnd().getUsername()));

                    handler.startElement("", "", "user", atts);
                    handler.endElement("", "", "user");
                }

                handler.endElement("", "", "users");
            }

            handler.endElement("", "", "group");
        }
    }

    private void doExtractConf(final ContentHandler handler) throws SAXException {
        if (conf == null) {
            LOG.debug("Report configuration is not present");
        }

        AttributesImpl atts = new AttributesImpl();
        handler.startElement("", "", "configurations", null);
        handler.startElement("", "", "groupAttributes", atts);

        if (conf != null) {
            for (Feature feature : conf.getFeatures()) {
                atts.clear();
                handler.startElement("", "", "feature", atts);
                handler.characters(feature.name().toCharArray(), 0, feature.name().length());
                handler.endElement("", "", "feature");
            }

            for (String attr : conf.getPlainAttrs()) {
                atts.clear();
                handler.startElement("", "", "attribute", atts);
                handler.characters(attr.toCharArray(), 0, attr.length());
                handler.endElement("", "", "attribute");
            }

            for (String derAttr : conf.getDerAttrs()) {
                atts.clear();
                handler.startElement("", "", "derAttribute", atts);
                handler.characters(derAttr.toCharArray(), 0, derAttr.length());
                handler.endElement("", "", "derAttribute");
            }

            for (String virAttr : conf.getVirAttrs()) {
                atts.clear();
                handler.startElement("", "", "virAttribute", atts);
                handler.characters(virAttr.toCharArray(), 0, virAttr.length());
                handler.endElement("", "", "virAttribute");
            }
        }

        handler.endElement("", "", "groupAttributes");
        handler.endElement("", "", "configurations");
    }

    private int count() {
        return StringUtils.isBlank(conf.getMatchingCond())
                ? groupDAO.count()
                : searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS,
                        SearchCondConverter.convert(conf.getMatchingCond()), AnyTypeKind.GROUP);
    }

    @Override
    protected void doExtract(final ReportletConf conf, final ContentHandler handler) throws SAXException {
        if (conf instanceof GroupReportletConf) {
            this.conf = GroupReportletConf.class.cast(conf);
        } else {
            throw new ReportException(new IllegalArgumentException("Invalid configuration provided"));
        }

        doExtractConf(handler);

        if (StringUtils.isBlank(this.conf.getMatchingCond())) {
            doExtract(handler, groupDAO.findAll());
        } else {
            for (int page = 1; page <= (count() / PAGE_SIZE) + 1; page++) {
                List<Group> groups = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                        SearchCondConverter.convert(this.conf.getMatchingCond()),
                        page, PAGE_SIZE, Collections.<OrderByClause>emptyList(), AnyTypeKind.GROUP);
                doExtract(handler, groups);
            }
        }
    }
}
