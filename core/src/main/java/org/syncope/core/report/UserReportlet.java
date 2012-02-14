/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.report;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.SyncopeConstants;
import org.syncope.client.report.UserReportletConf;
import org.syncope.client.report.UserReportletConf.Feature;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.rest.data.UserDataBinder;
import static org.syncope.core.scheduling.ReportXMLConst.*;
import org.syncope.core.util.EntitlementUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class UserReportlet extends AbstractReportlet {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(UserReportlet.class);

    private final static int PAGE_SIZE = 10;

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {

                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat(
                            SyncopeConstants.DEFAULT_DATE_PATTERN);
                }
            };

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSearchDAO searchDAO;

    @Autowired
    private UserDataBinder dataBinder;

    private List<SyncopeUser> getPagedUsers(final UserReportletConf conf,
            final int page) {

        Set<Long> adminRoleIds =
                EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        return conf.getMatchingCond() == null
                ? userDAO.findAll(adminRoleIds, page, PAGE_SIZE)
                : searchDAO.search(adminRoleIds, conf.getMatchingCond(),
                page, PAGE_SIZE);
    }

    private int count(final UserReportletConf conf) {
        Set<Long> adminRoleIds =
                EntitlementUtil.getRoleIds(entitlementDAO.findAll());

        return conf.getMatchingCond() == null
                ? userDAO.count(adminRoleIds)
                : searchDAO.count(adminRoleIds, conf.getMatchingCond());
    }

    private void doExtractResources(final ContentHandler handler,
            final AbstractAttributableTO attributableTO)
            throws SAXException {

        if (attributableTO.getResources().isEmpty()) {
            LOG.debug("No resources found for {}[{}]",
                    attributableTO.getClass().getSimpleName(),
                    attributableTO.getId());
        } else {
            AttributesImpl atts = new AttributesImpl();
            handler.startElement("", "", "resources", null);

            for (String resourceName : attributableTO.getResources()) {
                atts.clear();

                atts.addAttribute("", "", ATTR_NAME, XSD_STRING, resourceName);
                handler.startElement("", "", "resource", atts);
                handler.endElement("", "", "resource");
            }

            handler.endElement("", "", "resources");
        }
    }

    private void doExtractAttributes(final ContentHandler handler,
            final AbstractAttributableTO attributableTO,
            final Collection<String> attrs,
            final Collection<String> derAttrs,
            final Collection<String> virAttrs)
            throws SAXException {

        AttributesImpl atts = new AttributesImpl();
        if (!attrs.isEmpty()) {
            Map<String, AttributeTO> attrMap = attributableTO.getAttributeMap();

            handler.startElement("", "", "attributes", null);
            for (String attrName : attrs) {
                atts.clear();

                atts.addAttribute("", "", ATTR_NAME, XSD_STRING, attrName);
                handler.startElement("", "", "attribute", atts);

                if (attrMap.containsKey(attrName)) {
                    for (String value : attrMap.get(attrName).getValues()) {
                        handler.startElement("", "", "value", null);
                        handler.characters(
                                value.toCharArray(), 0, value.length());
                        handler.endElement("", "", "value");
                    }
                } else {
                    LOG.debug("{} not found for {}[{}]", new Object[]{
                                attrName,
                                attributableTO.getClass().getSimpleName(),
                                attributableTO.getId()});
                }

                handler.endElement("", "", "attribute");
            }
            handler.endElement("", "", "attributes");
        }

        if (!derAttrs.isEmpty()) {
            Map<String, AttributeTO> derAttrMap =
                    attributableTO.getDerivedAttributeMap();

            handler.startElement("", "", "derivedAttributes", null);
            for (String attrName : derAttrs) {
                atts.clear();

                atts.addAttribute("", "", ATTR_NAME, XSD_STRING, attrName);
                handler.startElement("", "", "derivedAttribute", atts);

                if (derAttrMap.containsKey(attrName)) {
                    for (String value : derAttrMap.get(attrName).getValues()) {
                        handler.startElement("", "", "value", null);
                        handler.characters(
                                value.toCharArray(), 0, value.length());
                        handler.endElement("", "", "value");
                    }
                } else {
                    LOG.debug("{} not found for {}[{}]", new Object[]{
                                attrName,
                                attributableTO.getClass().getSimpleName(),
                                attributableTO.getId()});
                }

                handler.endElement("", "", "derivedAttribute");
            }
            handler.endElement("", "", "derivedAttributes");
        }

        if (!virAttrs.isEmpty()) {
            Map<String, AttributeTO> virAttrMap =
                    attributableTO.getVirtualAttributeMap();

            handler.startElement("", "", "virtualAttributes", null);
            for (String attrName : virAttrs) {
                atts.clear();

                atts.addAttribute("", "", ATTR_NAME, XSD_STRING, attrName);
                handler.startElement("", "", "virtualAttribute", atts);

                if (virAttrMap.containsKey(attrName)) {
                    for (String value : virAttrMap.get(attrName).getValues()) {
                        handler.startElement("", "", "value", null);
                        handler.characters(
                                value.toCharArray(), 0, value.length());
                        handler.endElement("", "", "value");
                    }
                } else {
                    LOG.debug("{} not found for {}[{}]", new Object[]{
                                attrName,
                                attributableTO.getClass().getSimpleName(),
                                attributableTO.getId()});
                }

                handler.endElement("", "", "virtualAttribute");
            }
            handler.endElement("", "", "virtualAttributes");
        }
    }

    private void doExtract(final ContentHandler handler,
            final UserReportletConf conf,
            final List<SyncopeUser> users)
            throws SAXException, ReportException {

        AttributesImpl atts = new AttributesImpl();
        for (SyncopeUser user : users) {
            atts.clear();

            for (Feature feature : conf.getFeatures()) {
                String type = null;
                String value = null;
                switch (feature) {
                    case id:
                        type = XSD_LONG;
                        value = String.valueOf(user.getId());
                        break;

                    case username:
                        type = XSD_STRING;
                        value = user.getUsername();
                        break;

                    case workflowId:
                        type = XSD_LONG;
                        value = String.valueOf(user.getWorkflowId());
                        break;

                    case status:
                        type = XSD_STRING;
                        value = user.getStatus();
                        break;

                    case creationDate:
                        type = XSD_DATETIME;
                        value = user.getCreationDate() == null ? ""
                                : DATE_FORMAT.get().format(
                                user.getCreationDate());
                        break;

                    case lastLoginDate:
                        type = XSD_DATETIME;
                        value = user.getLastLoginDate() == null ? ""
                                : DATE_FORMAT.get().
                                format(user.getLastLoginDate());
                        break;

                    case changePwdDate:
                        type = XSD_DATETIME;
                        value = user.getChangePwdDate() == null ? ""
                                : DATE_FORMAT.get().format(
                                user.getChangePwdDate());
                        break;

                    case passwordHistorySize:
                        type = XSD_INT;
                        value = String.valueOf(
                                user.getPasswordHistory().size());
                        break;

                    case failedLoginCount:
                        type = XSD_INT;
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
            UserTO userTO = dataBinder.getUserTO(user);

            doExtractAttributes(handler, userTO, conf.getAttrs(),
                    conf.getDerAttrs(), conf.getVirAttrs());

            if (conf.getFeatures().contains(Feature.memberships)) {
                handler.startElement("", "", "memberships", null);

                for (MembershipTO memb : userTO.getMemberships()) {
                    atts.clear();

                    atts.addAttribute("", "", "id", XSD_LONG,
                            String.valueOf(memb.getId()));
                    atts.addAttribute("", "", "roleId", XSD_LONG,
                            String.valueOf(memb.getRoleId()));
                    atts.addAttribute("", "", "roleName", XSD_STRING,
                            String.valueOf(memb.getRoleName()));
                    handler.startElement("", "", "membership", atts);

                    doExtractAttributes(handler, memb,
                            memb.getAttributeMap().keySet(),
                            memb.getDerivedAttributeMap().keySet(),
                            memb.getVirtualAttributeMap().keySet());

                    if (conf.getFeatures().contains(Feature.resources)) {
                        doExtractResources(handler, memb);
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

    @Override
    @Transactional(readOnly = true)
    public void extract(final ContentHandler handler)
            throws SAXException, ReportException {

        if (getConf() == null || !(getConf() instanceof UserReportletConf)) {
            throw new ReportException(
                    new IllegalArgumentException("Expected "
                    + UserReportletConf.class.getName()
                    + ", got " + getConf()));
        }

        UserReportletConf conf = (UserReportletConf) getConf();

        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", ATTR_NAME, XSD_STRING, getConf().getName());
        handler.startElement("", "", ELEMENT_REPORTLET, atts);

        for (int i = 1; i <= (count(conf) / PAGE_SIZE) + 1; i++) {
            doExtract(handler, conf, getPagedUsers(conf, i));
        }

        handler.endElement("", "", ELEMENT_REPORTLET);
    }
}
