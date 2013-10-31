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

import org.apache.syncope.common.report.UserReportletConf;
import org.apache.syncope.common.report.UserReportletConf.Feature;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.DataFormat;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@ReportletConfClass(UserReportletConf.class)
public class UserReportlet extends AbstractReportlet<UserReportletConf> {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AttributableSearchDAO searchDAO;

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private RoleDataBinder roleDataBinder;

    private List<SyncopeUser> getPagedUsers(final int page) {
        final Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        final List<SyncopeUser> result;
        if (conf.getMatchingCond() == null) {
            result = userDAO.findAll(adminRoleIds, page, PAGE_SIZE);
        } else {
            result = searchDAO.search(adminRoleIds, conf.getMatchingCond(), page, PAGE_SIZE,
                    AttributableUtil.getInstance(AttributableType.USER));
        }

        return result;
    }

    private int count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        return conf.getMatchingCond() == null
                ? userDAO.count(adminRoleIds)
                : searchDAO.count(adminRoleIds, conf.getMatchingCond(),
                AttributableUtil.getInstance(AttributableType.USER));
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

    private void doExtract(final ContentHandler handler, final List<SyncopeUser> users)
            throws SAXException, ReportException {

        AttributesImpl atts = new AttributesImpl();
        for (SyncopeUser user : users) {
            atts.clear();

            for (Feature feature : conf.getFeatures()) {
                String type = null;
                String value = null;
                switch (feature) {
                    case id:
                        type = ReportXMLConst.XSD_LONG;
                        value = String.valueOf(user.getId());
                        break;

                    case username:
                        type = ReportXMLConst.XSD_STRING;
                        value = user.getUsername();
                        break;

                    case workflowId:
                        type = ReportXMLConst.XSD_LONG;
                        value = String.valueOf(user.getWorkflowId());
                        break;

                    case status:
                        type = ReportXMLConst.XSD_STRING;
                        value = user.getStatus();
                        break;

                    case creationDate:
                        type = ReportXMLConst.XSD_DATETIME;
                        value = user.getCreationDate() == null
                                ? ""
                                : DataFormat.format(user.getCreationDate());
                        break;

                    case lastLoginDate:
                        type = ReportXMLConst.XSD_DATETIME;
                        value = user.getLastLoginDate() == null
                                ? ""
                                : DataFormat.format(user.getLastLoginDate());
                        break;

                    case changePwdDate:
                        type = ReportXMLConst.XSD_DATETIME;
                        value = user.getChangePwdDate() == null
                                ? ""
                                : DataFormat.format(user.getChangePwdDate());
                        break;

                    case passwordHistorySize:
                        type = ReportXMLConst.XSD_INT;
                        value = String.valueOf(user.getPasswordHistory().size());
                        break;

                    case failedLoginCount:
                        type = ReportXMLConst.XSD_INT;
                        value = String.valueOf(user.getFailedLogins());
                        break;

                    default:
                }

                if (type != null && value != null) {
                    atts.addAttribute("", "", feature.name(), type, value);
                }
            }

            handler.startElement("", "", "user", atts);

            // Using UserTO for attribute values, since the conversion logic of
            // values to String is already encapsulated there
            UserTO userTO = userDataBinder.getUserTO(user);

            doExtractAttributes(handler, userTO, conf.getAttrs(), conf.getDerAttrs(), conf.getVirAttrs());

            if (conf.getFeatures().contains(Feature.memberships)) {
                handler.startElement("", "", "memberships", null);

                for (MembershipTO memb : userTO.getMemberships()) {
                    atts.clear();

                    atts.addAttribute("", "", "id", ReportXMLConst.XSD_LONG, String.valueOf(memb.getId()));
                    atts.addAttribute("", "", "roleId", ReportXMLConst.XSD_LONG, String.valueOf(memb.getRoleId()));
                    atts.addAttribute("", "", "roleName", ReportXMLConst.XSD_STRING, String.valueOf(memb.getRoleName()));
                    handler.startElement("", "", "membership", atts);

                    doExtractAttributes(handler, memb, memb.getAttrMap().keySet(), memb.getDerAttrMap()
                            .keySet(), memb.getVirAttrMap().keySet());

                    if (conf.getFeatures().contains(Feature.resources)) {
                        Membership actualMemb = user.getMembership(memb.getRoleId());
                        if (actualMemb == null) {
                            LOG.warn("Unexpected: cannot find membership for role {} for user {}", memb.getRoleId(),
                                    user);
                        } else {
                            doExtractResources(handler, roleDataBinder.getRoleTO(actualMemb.getSyncopeRole()));
                        }
                    }

                    handler.endElement("", "", "membership");
                }

                handler.endElement("", "", "memberships");
            }

            if (conf.getFeatures().contains(Feature.resources)) {
                doExtractResources(handler, userTO);
            }

            handler.endElement("", "", "user");
        }
    }

    private void doExtractConf(final ContentHandler handler) throws SAXException {

        AttributesImpl atts = new AttributesImpl();
        handler.startElement("", "", "configurations", null);
        handler.startElement("", "", "userAttributes", atts);

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

        handler.endElement("", "", "userAttributes");
        handler.endElement("", "", "configurations");
    }

    @Override
    protected void doExtract(final ContentHandler handler) throws SAXException, ReportException {
        doExtractConf(handler);
        for (int i = 1; i <= (count() / PAGE_SIZE) + 1; i++) {
            doExtract(handler, getPagedUsers(i));
        }
    }
}
