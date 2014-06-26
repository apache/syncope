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
package org.apache.syncope.core.init;

import java.lang.reflect.Field;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AbstractPolicySpec;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.beans.UserRequest;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.UserRequestDAO;
import org.apache.syncope.core.persistence.dao.impl.AbstractContentDealer;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.apache.syncope.core.util.XMLSerializer;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

/**
 * Upgrade internal storage content according to format expected by this Syncope release.
 */
@Component
public class ContentUpgrader extends AbstractContentDealer {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private UserRequestDAO userRequestDAO;

    private void upgradeActiviti() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<Map<String, Object>> byteArrays =
                jdbcTemplate.queryForList("SELECT ID_, BYTES_ FROM ACT_GE_BYTEARRAY");
        for (Map<String, Object> row : byteArrays) {
            byte[] updated = new String((byte[]) row.get("BYTES_")).replaceAll(
                    "org\\.apache.syncope\\.core\\.workflow\\.activiti\\.",
                    "org.apache.syncope.core.workflow.user.activiti.task.").
                    replaceAll("org\\.apache\\.syncope\\.client\\.to\\.",
                    "org.apache.syncope.common.to").
                    replaceAll("org\\.apache\\.syncope\\.types\\.",
                    "org.apache.syncope.common.types").
                    replaceAll("org/apache/syncope/types/",
                    "org/apache/syncope/common/types/").
                    getBytes();
            jdbcTemplate.update("UPDATE ACT_GE_BYTEARRAY SET BYTES_=? WHERE ID_=?",
                    new Object[] {updated, row.get("ID_")});
        }
    }

    private String upgradeSyncopeConf() {
        confDAO.delete("connid.bundles.directory");

        URI localConnIdLocation = null;
        for (URI location : ConnIdBundleManager.getConnManagers().keySet()) {
            if ("file".equals(location.getScheme())) {
                localConnIdLocation = location;
            }
        }
        if (localConnIdLocation == null) {
            throw new IllegalArgumentException("No local ConnId location was found, aborting");
        }

        SyncopeConf cipher = confDAO.find("password.cipher.algorithm");
        if ("MD5".equals(cipher.getValue())) {
            cipher.setValue(CipherAlgorithm.SMD5.name());
        }

        return localConnIdLocation.toString();
    }

    private void upgradeConnInstance(final String localConnIdLocation) {
        Field xmlConfiguration = ReflectionUtils.findField(ConnInstance.class, "xmlConfiguration");
        xmlConfiguration.setAccessible(true);
        for (ConnInstance connInstance : connInstanceDAO.findAll()) {
            connInstance.setLocation(localConnIdLocation);

            try {
                String oldConf = (String) xmlConfiguration.get(connInstance);
                connInstance.setConfiguration(
                        XMLSerializer.<HashSet<ConnConfProperty>>deserialize(
                        oldConf.replaceAll("org\\.apache\\.syncope\\.types\\.ConnConfProperty",
                        ConnConfProperty.class.getName())));
            } catch (Exception e) {
                LOG.error("While upgrading {}", connInstance, e);
            }
        }
    }

    private void upgradeExternalResource() {
        Field xmlConfiguration = ReflectionUtils.findField(ExternalResource.class, "xmlConfiguration");
        xmlConfiguration.setAccessible(true);
        for (ExternalResource resource : resourceDAO.findAll()) {
            try {
                String oldConf = (String) xmlConfiguration.get(resource);
                if (StringUtils.isNotBlank(oldConf)) {
                    resource.setConnInstanceConfiguration(
                            XMLSerializer.<HashSet<ConnConfProperty>>deserialize(
                            oldConf.replaceAll("org\\.apache\\.syncope\\.types\\.ConnConfProperty",
                            ConnConfProperty.class.getName())));
                }
            } catch (Exception e) {
                LOG.error("While upgrading {}", resource, e);
            }
        }
    }

    private void upgradePolicy() {
        Field specification = ReflectionUtils.findField(Policy.class, "specification");
        specification.setAccessible(true);
        for (Policy policy : policyDAO.findAll()) {
            try {
                String oldConf = (String) specification.get(policy);
                policy.setSpecification(
                        XMLSerializer.<AbstractPolicySpec>deserialize(
                        oldConf.replaceAll("org\\.apache\\.syncope\\.types\\.",
                        "org.apache.syncope.common.types.").
                        replaceAll("alternativeSearchAttrs", "uAltSearchSchemas")));
            } catch (Exception e) {
                LOG.error("While upgrading {}", policy, e);
            }
        }
    }

    private void upgradeNotification() {
        Field xmlAbout = ReflectionUtils.findField(Notification.class, "xmlAbout");
        xmlAbout.setAccessible(true);
        Field xmlRecipients = ReflectionUtils.findField(Notification.class, "xmlRecipients");
        xmlRecipients.setAccessible(true);
        for (Notification notification : notificationDAO.findAll()) {
            try {
                String oldAbout = (String) xmlAbout.get(notification);
                if (oldAbout != null) {
                    notification.setAbout(
                            XMLSerializer.<NodeCond>deserialize(
                            oldAbout.replaceAll("org\\.apache\\.syncope\\.client\\.search\\.",
                            "org.apache.syncope.common.search.")));
                }
                String oldRecipients = (String) xmlRecipients.get(notification);
                if (oldRecipients != null) {
                    notification.setRecipients(
                            XMLSerializer.<NodeCond>deserialize(
                            oldRecipients.replaceAll("org\\.apache\\.syncope\\.client\\.search\\.",
                            "org.apache.syncope.common.search.")));
                }
            } catch (Exception e) {
                LOG.error("While upgrading {}", notification, e);
            }
        }
    }

    private void upgradeSyncTask() {
        Field userTemplate = ReflectionUtils.findField(SyncTask.class, "userTemplate");
        userTemplate.setAccessible(true);
        for (SyncTask task : taskDAO.findAll(SyncTask.class)) {
            try {
                String oldUserTemplate = (String) userTemplate.get(task);
                if (oldUserTemplate != null) {
                    task.setUserTemplate(
                            XMLSerializer.<UserTO>deserialize(
                            oldUserTemplate.replaceAll("org\\.apache\\.syncope\\.client\\.to\\.",
                            "org.apache.syncope.common.to.").
                            replaceAll("propagationTOs",
                            "propagationStatusTOs")));
                }
            } catch (Exception e) {
                LOG.error("While upgrading {}", task, e);
            }
        }
    }

    private void upgradeReportletConf() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<Map<String, Object>> rcInstances =
                jdbcTemplate.queryForList("SELECT id, serializedInstance FROM ReportletConfInstance");
        for (Map<String, Object> row : rcInstances) {
            String updated = ((String) row.get("serializedInstance")).
                    replaceAll("org\\.apache\\.syncope\\.client\\.report\\.",
                    "org.apache.syncope.common.report.");
            jdbcTemplate.update("UPDATE ReportletConfInstance SET serializedInstance=? WHERE id=?",
                    new Object[] {updated, row.get("id")});
        }
    }

    private void upgradeUserRequest() {
        Field payload = ReflectionUtils.findField(UserRequest.class, "payload");
        payload.setAccessible(true);
        for (UserRequest request : userRequestDAO.findAll()) {
            try {
                String oldPayload = (String) payload.get(request);
                if (oldPayload != null) {
                    payload.set(request,
                            oldPayload.replaceAll("org\\.apache\\.syncope\\.client\\.to\\.",
                            "org.apache.syncope.common.to.").
                            replaceAll("org\\.apache\\.syncope\\.client\\.mod\\.",
                            "org.apache.syncope.common.mod."));
                }
            } catch (Exception e) {
                LOG.error("While upgrading {}", request, e);
            }
        }
    }

    @Transactional
    public void upgrade() {
        if (ActivitiDetector.isActivitiEnabledForUsers()) {
            upgradeActiviti();
        }

        final String localConnIdLocation = upgradeSyncopeConf();

        upgradeConnInstance(localConnIdLocation);

        upgradeExternalResource();

        upgradePolicy();

        upgradeNotification();

        upgradeSyncTask();

        upgradeReportletConf();

        upgradeUserRequest();

        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            createIndexes();
            createViews();
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    LOG.error("While releasing connection", e);
                }
            }
        }
    }
}
