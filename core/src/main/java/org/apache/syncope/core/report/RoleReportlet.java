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
package org.apache.syncope.core.report;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.report.RoleReportletConf;
import org.apache.syncope.common.report.RoleReportletConf.Feature;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@ReportletConfClass(RoleReportletConf.class)
public class RoleReportlet extends AbstractReportlet<RoleReportletConf> {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private AttributableSearchDAO searchDAO;

    @Autowired
    private RoleDataBinder roleDataBinder;

    private List<SyncopeRole> getPagedRoles(final int page) {
        final Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(entitlementDAO.findAll());
        final List<SyncopeRole> result;
        if (conf.getMatchingCond() == null) {
            result = roleDAO.findAll();
        } else {
            result = searchDAO.search(adminRoleIds, conf.getMatchingCond(), page, PAGE_SIZE,
                    AttributableUtil.getInstance(AttributableType.ROLE));
        }

        return result;
    }

    private int count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        return conf.getMatchingCond() == null
                ? roleDAO.findAll().size()
                : searchDAO.count(adminRoleIds, conf.getMatchingCond(),
                AttributableUtil.getInstance(AttributableType.ROLE));
    }

    private void doExtractResources(final ContentHandler handler, final AbstractAttributableTO attributableTO)
            throws SAXException {

        if (attributableTO.getResources().isEmpty()) {
            LOG.debug("No resources found for {}[{}]", attributableTO.getClass().getSimpleName(), attributableTO
                    .getId());
        } else {
            AttributesImpl atts = new AttributesImpl();
            handler.startElement("", "", "resources", null);

            for (String resourceName : attributableTO.getResources()) {
                atts.clear();

                atts.addAttribute("", "", ReportXMLConst.ATTR_NAME, ReportXMLConst.XSD_STRING, resourceName);
                handler.startElement("", "", "resource", atts);
                handler.endElement("", "", "resource");
            }

            handler.endElement("", "", "resources");
        }
    }

    private void doExtractAttributes(final ContentHandler handler, final AbstractAttributableTO attributableTO,
            final Collection<String> attrs, final Collection<String> derAttrs, final Collection<String> virAttrs)
            throws SAXException {

        AttributesImpl atts = new AttributesImpl();
        if (!attrs.isEmpty()) {
            Map<String, AttributeTO> attrMap = attributableTO.getAttrMap();

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
                            attributableTO.getClass().getSimpleName(), attributableTO.getId());
                }

                handler.endElement("", "", "attribute");
            }
            handler.endElement("", "", "attributes");
        }

        if (!derAttrs.isEmpty()) {
            Map<String, AttributeTO> derAttrMap = attributableTO.getDerAttrMap();

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
                            attributableTO.getClass().getSimpleName(), attributableTO.getId());
                }

                handler.endElement("", "", "derivedAttribute");
            }
            handler.endElement("", "", "derivedAttributes");
        }

        if (!virAttrs.isEmpty()) {
            Map<String, AttributeTO> virAttrMap = attributableTO.getVirAttrMap();

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
                            attributableTO.getClass().getSimpleName(), attributableTO.getId());
                }

                handler.endElement("", "", "virtualAttribute");
            }
            handler.endElement("", "", "virtualAttributes");
        }
    }

    private void doExtract(final ContentHandler handler, final List<SyncopeRole> roles)
            throws SAXException, ReportException {

        AttributesImpl atts = new AttributesImpl();
        for (SyncopeRole role : roles) {
            atts.clear();

            for (Feature feature : conf.getFeatures()) {
                String type = null;
                String value = null;
                switch (feature) {
                    case id:
                        type = ReportXMLConst.XSD_LONG;
                        value = String.valueOf(role.getId());
                        break;

                    case name:
                        type = ReportXMLConst.XSD_STRING;
                        value = String.valueOf(role.getName());
                        break;

                    case roleOwner:
                        type = ReportXMLConst.XSD_LONG;
                        value = String.valueOf(role.getRoleOwner());
                        break;

                    case userOwner:
                        type = ReportXMLConst.XSD_LONG;
                        value = String.valueOf(role.getUserOwner());
                        break;

                    default:
                }

                if (type != null && value != null) {
                    atts.addAttribute("", "", feature.name(), type, value);
                }
            }

            handler.startElement("", "", "role", atts);

            // Using RoleTO for attribute values, since the conversion logic of
            // values to String is already encapsulated there
            RoleTO roleTO = roleDataBinder.getRoleTO(role);

            doExtractAttributes(handler, roleTO, conf.getAttrs(), conf.getDerAttrs(), conf.getVirAttrs());

            if (conf.getFeatures().contains(Feature.entitlements)) {
                handler.startElement("", "", "entitlements", null);

                for (String ent : roleTO.getEntitlements()) {
                    atts.clear();

                    atts.addAttribute("", "", "id", ReportXMLConst.XSD_STRING, String.valueOf(ent));

                    handler.startElement("", "", "entitlement", atts);
                    handler.endElement("", "", "entitlement");
                }

                handler.endElement("", "", "entitlements");
            }
            // to get resources associated to a role
            if (conf.getFeatures().contains(Feature.resources)) {
                doExtractResources(handler, roleTO);
            }
            //to get users asscoiated to a role is preferred RoleDAO to RoleTO
            if (conf.getFeatures().contains(Feature.users)) {
                handler.startElement("", "", "users", null);

                for (Membership memb : roleDAO.findMemberships(role)) {
                    atts.clear();

                    atts.addAttribute("", "", "userId", ReportXMLConst.XSD_LONG,
                            String.valueOf(memb.getSyncopeUser().getId()));
                    atts.addAttribute("", "", "userUsername", ReportXMLConst.XSD_STRING,
                            String.valueOf(memb.getSyncopeUser().getUsername()));

                    handler.startElement("", "", "user", atts);
                    handler.endElement("", "", "user");
                }

                handler.endElement("", "", "users");
            }

            handler.endElement("", "", "role");
        }
    }

    private void doExtractConf(final ContentHandler handler) throws SAXException {

        if (conf == null) {
            LOG.debug("Report configuration is not present");
        }

        AttributesImpl atts = new AttributesImpl();
        handler.startElement("", "", "configurations", null);
        handler.startElement("", "", "roleAttributes", atts);

        for (Feature feature : conf.getFeatures()) {
            atts.clear();
            handler.startElement("", "", "feature", atts);
            handler.characters(feature.name().toCharArray(), 0, feature.name().length());
            handler.endElement("", "", "feature");
        }

        for (String attr : conf.getAttrs()) {
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

        handler.endElement("", "", "roleAttributes");
        handler.endElement("", "", "configurations");
    }

    @Override
    protected void doExtract(final ContentHandler handler) throws SAXException, ReportException {
        doExtractConf(handler);
        for (int i = 1; i <= (count() / PAGE_SIZE) + 1; i++) {
            doExtract(handler, getPagedRoles(i));
        }
    }
}
