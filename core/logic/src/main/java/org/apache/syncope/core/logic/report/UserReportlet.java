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
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.report.UserReportletConf.Feature;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.misc.DataFormat;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@ReportletConfClass(UserReportletConf.class)
public class UserReportlet extends AbstractReportlet<UserReportletConf> {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SubjectSearchDAO searchDAO;

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private GroupDataBinder groupDataBinder;

    private List<User> getPagedUsers(final int page) {
        List<User> result;

        if (StringUtils.isBlank(conf.getMatchingCond())) {
            result = userDAO.findAll(SyncopeConstants.FULL_ADMIN_REALMS, page, PAGE_SIZE);
        } else {
            result = searchDAO.search(SyncopeConstants.FULL_ADMIN_REALMS,
                    SearchCondConverter.convert(conf.getMatchingCond()),
                    page, PAGE_SIZE, Collections.<OrderByClause>emptyList(), SubjectType.USER);
        }

        return result;
    }

    private int count() {
        return StringUtils.isBlank(conf.getMatchingCond())
                ? userDAO.count(SyncopeConstants.FULL_ADMIN_REALMS)
                : searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS,
                        SearchCondConverter.convert(conf.getMatchingCond()), SubjectType.USER);
    }

    private void doExtractResources(final ContentHandler handler, final AbstractSubjectTO subjectTO)
            throws SAXException {

        if (subjectTO.getResources().isEmpty()) {
            LOG.debug("No resources found for {}[{}]", subjectTO.getClass().getSimpleName(), subjectTO.getKey());
        } else {
            AttributesImpl atts = new AttributesImpl();
            handler.startElement("", "", "resources", null);

            for (String resourceName : subjectTO.getResources()) {
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
            Map<String, AttrTO> attrMap = attributableTO.getPlainAttrMap();

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
                            attributableTO.getClass().getSimpleName(), attributableTO.getKey());
                }

                handler.endElement("", "", "attribute");
            }
            handler.endElement("", "", "attributes");
        }

        if (!derAttrs.isEmpty()) {
            Map<String, AttrTO> derAttrMap = attributableTO.getDerAttrMap();

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
                            attributableTO.getClass().getSimpleName(), attributableTO.getKey());
                }

                handler.endElement("", "", "derivedAttribute");
            }
            handler.endElement("", "", "derivedAttributes");
        }

        if (!virAttrs.isEmpty()) {
            Map<String, AttrTO> virAttrMap = attributableTO.getVirAttrMap();

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
                            attributableTO.getClass().getSimpleName(), attributableTO.getKey());
                }

                handler.endElement("", "", "virtualAttribute");
            }
            handler.endElement("", "", "virtualAttributes");
        }
    }

    private void doExtract(final ContentHandler handler, final List<User> users) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        for (User user : users) {
            atts.clear();

            for (Feature feature : conf.getFeatures()) {
                String type = null;
                String value = null;
                switch (feature) {
                    case key:
                        type = ReportXMLConst.XSD_LONG;
                        value = String.valueOf(user.getKey());
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

            doExtractAttributes(handler, userTO, conf.getPlainAttrs(), conf.getDerAttrs(), conf.getVirAttrs());

            if (conf.getFeatures().contains(Feature.memberships)) {
                handler.startElement("", "", "memberships", null);

                for (MembershipTO memb : userTO.getMemberships()) {
                    atts.clear();

                    atts.addAttribute("", "", "id", ReportXMLConst.XSD_LONG, String.valueOf(memb.getKey()));
                    atts.addAttribute("", "", "groupId", ReportXMLConst.XSD_LONG, String.valueOf(memb.getGroupKey()));
                    atts.addAttribute("", "", "groupName", ReportXMLConst.XSD_STRING, String.
                            valueOf(memb.getGroupName()));
                    handler.startElement("", "", "membership", atts);

                    doExtractAttributes(handler, memb, memb.getPlainAttrMap().keySet(), memb.getDerAttrMap()
                            .keySet(), memb.getVirAttrMap().keySet());

                    if (conf.getFeatures().contains(Feature.resources)) {
                        Membership actualMemb = user.getMembership(memb.getGroupKey());
                        if (actualMemb == null) {
                            LOG.warn("Unexpected: cannot find membership for group {} for user {}", memb.getGroupKey(),
                                    user);
                        } else {
                            doExtractResources(handler, groupDataBinder.getGroupTO(actualMemb.getGroup()));
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

        handler.endElement("", "", "userAttributes");
        handler.endElement("", "", "configurations");
    }

    @Override
    protected void doExtract(final ContentHandler handler) throws SAXException {
        doExtractConf(handler);
        for (int i = 1; i <= (count() / PAGE_SIZE) + 1; i++) {
            doExtract(handler, getPagedUsers(i));
        }
    }
}
