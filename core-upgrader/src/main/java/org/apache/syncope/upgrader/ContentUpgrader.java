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
package org.apache.syncope.upgrader;

import org.apache.syncope.upgrader.util.XMLDeserializer;
import org.apache.syncope.upgrader.util.SyncopeDefParams;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.report.UserReportletConf;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AbstractPolicySpec;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.conf.CAttr;
import org.apache.syncope.core.persistence.beans.conf.CSchema;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MVirSchema;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RVirSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.AttrTemplateDAO;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.MembershipDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.dao.VirSchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.POJOHelper;
import org.apache.syncope.core.util.ResourceWithFallbackLoader;
import org.apache.syncope.core.workflow.user.activiti.ActivitiImportUtils;
import org.apache.syncope.core.workflow.user.activiti.ActivitiUserWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

@Component
@DependsOn("springContextInitializer")
public class ContentUpgrader implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ContentUpgrader.class);

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private AttrTemplateDAO attrTemplateDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private ActivitiImportUtils importUtils;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    @Qualifier("transactionManager")
    protected JpaTransactionManager txManager;

    @Resource(name = "userWorkflowXML")
    private ResourceWithFallbackLoader userWorkflowXML;

    private boolean continueUpgrade = true;

    private boolean deleteOldWorkflow = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
        final TransactionTemplate txTemplate = new TransactionTemplate(txManager);

        txTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                try {
                    doUpgradePhaseOne();
                } catch (Exception e) {
                    LOG.error("Upgrade STOPPED: an error occurred during upgrade", e);
                }
            }
        });

        if (continueUpgrade) {
            txTemplate.execute(new TransactionCallbackWithoutResult() {

                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    try {
                        doUpgradePhaseTwo();
                    } catch (Exception e) {
                        LOG.error("Upgrade STOPPED: an error occurred during upgrade", e);
                    }
                }
            });
        } else {
            LOG.error("Upgrade STOPPED: can not continue due to previous errors, see exceptions above.");
            return;
        }
        LOG.info("Upgrade completed SUCCESSFULLY.");
    }

    public void doUpgradePhaseOne() throws Exception {
        LOG.info("Beginning upgrade PHASE 1...");
        upgradeSyncopeConf();
        upgradeExternalResource();
        upgradeConnInstance();
        updgradePropagationTask();
        upgradeSyncTask();
        upgradePolicy();
        upgradeReportletConf();
        upgradeWorkflow();
        addTemplatesToRoles();
        addTemplatesToMemberships();
        LOG.info("Upgrade PHASE 1 completed.");
    }

    private void doUpgradePhaseTwo() throws IOException {
        LOG.info("Beginning upgrade PHASE 2...");
        addTemplatesToRAttrs();
        addTemplatesToMAttrs();
        LOG.info("Upgrade PHASE 2 completed.");
    }

    public void setDeleteOldWorkflow(final boolean deleteOldWorkflow) {
        this.deleteOldWorkflow = deleteOldWorkflow;
    }

    private void upgradeExternalResource() throws Exception {
        LOG.info("Upgrading ExternalResource table...");
        final Field jsonConf = ReflectionUtils.findField(ExternalResource.class, "jsonConf");
        jsonConf.setAccessible(true);
        final Field uSyncToken = ReflectionUtils.findField(ExternalResource.class, "userializedSyncToken");
        uSyncToken.setAccessible(true);
        final Field rSyncToken = ReflectionUtils.findField(ExternalResource.class, "rserializedSyncToken");
        rSyncToken.setAccessible(true);
        for (ExternalResource resource : resourceDAO.findAll()) {
            try {
                final String oldConf = (String) jsonConf.get(resource);
                if (StringUtils.isNotBlank(oldConf)) {
                    LOG.info("Upgrading resource {} jsonConf", resource.getName());
                    resource.setConnInstanceConfiguration(
                            XMLDeserializer.<HashSet<ConnConfProperty>>deserialize(oldConf));
                }

                final String oldUSyncToken = (String) uSyncToken.get(resource);
                final String oldRSyncToken = (String) rSyncToken.get(resource);

                if (StringUtils.isNotBlank(oldUSyncToken)) {
                    LOG.info("Upgrading resource {} userializedSyncToken", resource.getName());
                    resource.setUsyncToken(XMLDeserializer.<SyncToken>deserialize(oldUSyncToken));
                }
                if (StringUtils.isNotBlank(oldRSyncToken)) {
                    LOG.info("Upgrading resource {} rserializedSyncToken", resource.getName());
                    resource.setRsyncToken(XMLDeserializer.<SyncToken>deserialize(oldRSyncToken));
                }
            } catch (Exception e) {
                LOG.error("While upgrading resource {}", resource, e);
                continueUpgrade = false;
                throw e;
            }
        }
    }

    private void upgradeConnInstance() throws Exception {
        LOG.info("Upgrading ConnInstance table...");
        final Field jsonConf = ReflectionUtils.findField(ConnInstance.class, "jsonConf");
        jsonConf.setAccessible(true);
        for (ConnInstance connInstance : connInstanceDAO.findAll()) {
            LOG.info("Upgrading connInstance {} jsonConf", connInstance);
            try {
                final String oldConf = (String) jsonConf.get(connInstance);
                connInstance.setConfiguration(XMLDeserializer.<HashSet<ConnConfProperty>>deserialize(oldConf));
            } catch (Exception e) {
                LOG.error("While upgrading connInstance {}", connInstance, e);
                continueUpgrade = false;
                throw e;
            }
        }
    }

    private void upgradePolicy() throws Exception {
        LOG.info("Upgrading Policy table...");
        final Field specification = ReflectionUtils.findField(Policy.class, "specification");
        specification.setAccessible(true);
        for (Policy policy : policyDAO.findAll()) {
            LOG.info("Upgrading policy {} specification", policy.getDescription());
            try {
                final String oldConf = (String) specification.get(policy);
                policy.setSpecification(XMLDeserializer.<AbstractPolicySpec>deserialize(oldConf));
            } catch (Exception e) {
                LOG.error("While upgrading policy {}", policy, e);
                continueUpgrade = false;
                throw e;
            }
        }
    }

    private void upgradeSyncTask() throws Exception {
        LOG.info("Upgrading Task table (sync tasks)...");
        final Field userTemplate = ReflectionUtils.findField(SyncTask.class, "userTemplate");
        userTemplate.setAccessible(true);
        final Field roleTemplate = ReflectionUtils.findField(SyncTask.class, "roleTemplate");
        roleTemplate.setAccessible(true);
        for (SyncTask task : taskDAO.findAll(SyncTask.class)) {
            try {
                LOG.info("Upgrading syncTask {} userTemplate", task.getName());
                final String oldUserTemplate = (String) userTemplate.get(task);
                final String oldRoleTemplate = (String) roleTemplate.get(task);
                if (oldUserTemplate != null) {
                    task.setUserTemplate(XMLDeserializer.<UserTO>deserialize(oldUserTemplate));
                }
                if (oldRoleTemplate != null) {
                    LOG.info("Upgrading syncTask {} roleTemplate", task.getName());
                    task.setRoleTemplate(XMLDeserializer.<RoleTO>deserialize(oldRoleTemplate));
                }
            } catch (Exception e) {
                LOG.error("While upgrading syncTask {}", task, e);
                continueUpgrade = false;
                throw e;
            }
        }
    }

    private void updgradePropagationTask() throws Exception {
        LOG.info("Upgrading Task table (propagation tasks)...");
        final Field xmlAttributes = ReflectionUtils.findField(PropagationTask.class, "xmlAttributes");
        xmlAttributes.setAccessible(true);
        for (PropagationTask task : taskDAO.findAll(PropagationTask.class)) {
            try {
                final String oldXmlAttr = (String) xmlAttributes.get(task);
                if (StringUtils.isNotBlank(oldXmlAttr)) {
                    LOG.info("Upgrading propagationTask {} xmlAttributes", task.getId());
                    task.setAttributes(XMLDeserializer.<HashSet<Attribute>>deserialize(oldXmlAttr));
                }
            } catch (Exception e) {
                LOG.error("While upgrading propagationTask {}", task, e);
                continueUpgrade = false;
                throw e;
            }
        }
    }

    private void upgradeReportletConf() {
        LOG.info("Upgrading ReportletConf table...");
        try {
            final List<Map<String, Object>> reportletConfs = jdbcTemplate.queryForList(
                    "SELECT id, serializedInstance FROM ReportletConfInstance");
            for (Map<String, Object> row : reportletConfs) {
                final String serializedInstance = (String) row.get("serializedInstance");
                if (StringUtils.isNotBlank(serializedInstance)) {
                    LOG.info("Upgrading ReportletConf {} serializedInstance", row.get("id"));
                    final UserReportletConf set = XMLDeserializer.<UserReportletConf>deserialize(serializedInstance);
                    final String newSerializedInst = POJOHelper.serialize(set);
                    jdbcTemplate.update("UPDATE ReportletConfInstance set serializedInstance = ? WHERE id = ?",
                            new Object[] { newSerializedInst, row.get("id") });
                }
            }
        } catch (DataAccessException e) {
            LOG.error("Error accessing ReportletConfInstance table", e);
            continueUpgrade = false;
            throw e;
        }
    }

    private void upgradeSyncopeConf() {
        LOG.info("Upgrading SyncopeConf table...");
        // get old syncope configuration from provisional table created ad hoc
        final Map<String, String> syncopeConf = new HashMap<String, String>();
        try {
            final List<Map<String, Object>> syncopeConfs = jdbcTemplate.queryForList(
                    "SELECT * FROM SyncopeConf_temp");

            for (Map<String, Object> row : syncopeConfs) {
                final String confKey = (String) row.get("confKey");
                if (StringUtils.isNotBlank(confKey) && !SyncopeDefParams.contains(confKey)) {
                    syncopeConf.put(confKey, (String) row.get("confValue"));
                }
            }

            // delete provisional table
            jdbcTemplate.update("DROP TABLE SyncopeConf_temp;");

        } catch (DataAccessException e) {
            LOG.error("Error accessing SyncopeConf table", e);
            continueUpgrade = false;
            throw e;
        }

        // save new, well-formed SyncopeConf and associated CSchema, CAttr and CAttrValue
        for (Map.Entry<String, String> entry : syncopeConf.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            // 1. create CSChema
            CSchema confSchema = new CSchema();
            confSchema.setName(key);
            confSchema.setType(AttributeSchemaType.String);
            confSchema.setMultivalue(value.contains("|"));
            confSchema.setUniqueConstraint(false);
            confSchema.setReadonly(false);
            confSchema = schemaDAO.save(confSchema);
            // 2. create and save CAttr
            final CAttr confAttr = new CAttr();
            confAttr.setSchema(confSchema);
            confAttr.setOwner(confDAO.get());
            if (confSchema.isMultivalue()) {
                for (String singleValue : value.split("|")) {
                    confAttr.addValue(singleValue, AttributableUtil.getInstance(AttributableType.CONFIGURATION));
                }
            } else {
                confAttr.addValue(value, AttributableUtil.getInstance(AttributableType.CONFIGURATION));
            }
            confDAO.save(confAttr);
        }
    }

    private void upgradeWorkflow() {
        try {
            LOG.info("Upgrading workflow version...");
            final byte[] userWorkflowBytes = IOUtils.toString(userWorkflowXML.getResource().getInputStream()).getBytes();

            // delete old workflow version if needed, Activiti 5.15.1 workflow referred to Syncope 1.1.X
            if (deleteOldWorkflow) {
                jdbcTemplate.update("DELETE FROM ACT_GE_BYTEARRAY WHERE NAME_ = ?;",
                        ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE);
            }
            if (userWorkflowBytes != null && userWorkflowBytes.length > 0) {
                // write default workflow value to database
                LOG.info("Importing default workflow for Syncope 1.2.X");

                importUtils.fromXML(userWorkflowBytes);

                // add activiti model to database
                final Model model = repositoryService.newModel();

                final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().
                        processDefinitionKey(ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().singleResult();

                model.setDeploymentId(processDefinition.getDeploymentId());
                model.setName("User Workflow");
                model.setVersion(1);
                model.setMetaInfo("{\"name\":\"User Workflow\",\"revision\":2,\"description\":null}");
                repositoryService.saveModel(model);

                repositoryService.addModelEditorSource(repositoryService.createModelQuery().deploymentId(
                        processDefinition.getDeploymentId()).singleResult().getId(), userWorkflowBytes);

                // update ACT_RU_EXECUTION table and ACT_RU_TASK tables for old database users with new process 
                // id associated to the new workflow
                jdbcTemplate.update("UPDATE ACT_RU_EXECUTION SET PROC_DEF_ID_ = ?", processDefinition.getId());
                jdbcTemplate.update("UPDATE ACT_RU_TASK SET PROC_DEF_ID_ = ?", processDefinition.getId());
            }
        } catch (IOException e) {
            LOG.error("Error reading file {}", ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        } catch (DataAccessException e) {
            LOG.error("Error accessing table ACT_GE_BYTEARRAY", e);
        }
    }

    private void addTemplatesToRoles() {
        try {
            LOG.info("Adding templates to roles");
            for (SyncopeRole role : roleDAO.findAll()) {

                final List<RAttr> newRattrs = new ArrayList<RAttr>();
                final List<RAttrTemplate> newRattrTemplates = new ArrayList<RAttrTemplate>();

                final List<RDerAttr> newRDerattrs = new ArrayList<RDerAttr>();
                final List<RDerAttrTemplate> newRDerattrTemplates = new ArrayList<RDerAttrTemplate>();

                final List<RVirAttr> newRVirattrs = new ArrayList<RVirAttr>();
                final List<RVirAttrTemplate> newRVirattrTemplates = new ArrayList<RVirAttrTemplate>();

                LOG.info("Adding role templates to role {}", role);

                //Create normal attributes templates
                for (AbstractAttr attr : role.getAttrs()) {
                    final RAttrTemplate rAttrTemplate = new RAttrTemplate();
                    rAttrTemplate.setOwner(role);
                    LOG.info("Creating template for role normal attribute {}", attr);
                    final String schemaName = jdbcTemplate.queryForObject(
                            "SELECT schema_name FROM RAttr WHERE id = ?;", String.class, attr.getId());

                    rAttrTemplate.setSchema(schemaDAO.find(schemaName, RSchema.class));
                    newRattrTemplates.add(rAttrTemplate);
                }

                //Create derived attributes templates
                for (AbstractDerAttr derAttr : role.getDerAttrs()) {
                    final RDerAttrTemplate rDerattrTemplate = new RDerAttrTemplate();
                    rDerattrTemplate.setOwner(role);
                    LOG.info("Creating template for role derived attribute {}", derAttr);
                    final String derSchemaName = jdbcTemplate.queryForObject(
                            "SELECT DERIVEDSCHEMA_NAME FROM RDerAttr WHERE id = ?;", String.class, derAttr.getId());

                    rDerattrTemplate.setSchema(derSchemaDAO.find(derSchemaName, RDerSchema.class));
                    newRDerattrTemplates.add(rDerattrTemplate);
                }

                //Create virtual attributes templates
                for (AbstractVirAttr virAttr : role.getVirAttrs()) {
                    final RVirAttrTemplate rVirattrTemplate = new RVirAttrTemplate();
                    rVirattrTemplate.setOwner(role);
                    LOG.info("Creating template for role virtual attribute {}", virAttr);
                    final String virSchemaName = jdbcTemplate.queryForObject(
                            "SELECT VIRTUALSCHEMA_NAME FROM RVirAttr WHERE id = ?;", String.class, virAttr.getId());
                    rVirattrTemplate.setSchema(virSchemaDAO.find(virSchemaName, RVirSchema.class));
                    newRVirattrTemplates.add(rVirattrTemplate);
                }
                role.setAttrs(newRattrs);
                role.setDerAttrs(newRDerattrs);
                role.setVirAttrs(newRVirattrs);
                role.getAttrTemplates(RAttrTemplate.class).addAll(newRattrTemplates);
                role.getAttrTemplates(RDerAttrTemplate.class).addAll(newRDerattrTemplates);
                role.getAttrTemplates(RVirAttrTemplate.class).addAll(newRVirattrTemplates);
            }
        } catch (DataAccessException ex) {
            LOG.error("Error accessing RAttr table", ex);
        }
    }

    private void addTemplatesToRAttrs() {
        try {
            LOG.info("Adding templates to role attributes");
            for (SyncopeRole role : roleDAO.findAll()) {

                final List<RAttr> newRattrs = new ArrayList<RAttr>();
                final List<RDerAttr> newRDerattrs = new ArrayList<RDerAttr>();
                final List<RVirAttr> newRVirattrs = new ArrayList<RVirAttr>();

                // add template to normal attributes
                for (Long attrId : jdbcTemplate.queryForList("SELECT id FROM RAttr WHERE OWNER_ID = ?", Long.class,
                        role.getId())) {
                    final RAttr rAttr = attrDAO.find(attrId, RAttr.class);

                    LOG.info("Adding template to role attribute {}", rAttr);

                    final String schemaName = jdbcTemplate.queryForObject(
                            "SELECT schema_name FROM RAttr WHERE id = ?;", String.class, attrId);

                    rAttr.setTemplate(getTemplate(role, schemaName, RAttrTemplate.class));
                    newRattrs.add(rAttr);
                }

                // add template to derived attributes
                for (Long attrId : jdbcTemplate.queryForList("SELECT id FROM RDerAttr WHERE OWNER_ID = ?", Long.class,
                        role.getId())) {
                    final RDerAttr rDerAttr = derAttrDAO.find(attrId, RDerAttr.class);

                    LOG.info("Adding template to role attribute {}", rDerAttr);

                    final String derSchemaName = jdbcTemplate.queryForObject(
                            "SELECT DERIVEDSCHEMA_NAME FROM RDerAttr WHERE id = ?;", String.class, attrId);

                    rDerAttr.setTemplate(getTemplate(role, derSchemaName, RDerAttrTemplate.class));
                    newRDerattrs.add(rDerAttr);
                }

                // add template to virtual attributes
                for (Long attrId : jdbcTemplate.queryForList("SELECT id FROM RVirAttr WHERE OWNER_ID = ?", Long.class,
                        role.getId())) {
                    final RVirAttr rVirAttr = virAttrDAO.find(attrId, RVirAttr.class);

                    LOG.info("Adding template to role attribute {}", rVirAttr);

                    final String virSchemaName = jdbcTemplate.queryForObject(
                            "SELECT VIRTUALSCHEMA_NAME FROM RVirAttr WHERE id = ?;", String.class, attrId);

                    rVirAttr.setTemplate(getTemplate(role, virSchemaName, RVirAttrTemplate.class));
                    newRVirattrs.add(rVirAttr);
                }
                role.setAttrs(newRattrs);
                role.setDerAttrs(newRDerattrs);
                role.setVirAttrs(newRVirattrs);
            }

            // drop obsolete columns
            jdbcTemplate.update("ALTER TABLE RAttr DROP COLUMN schema_name;");
            jdbcTemplate.update("ALTER TABLE RDerAttr DROP COLUMN DERIVEDSCHEMA_NAME;");
            jdbcTemplate.update("ALTER TABLE RVirAttr DROP COLUMN VIRTUALSCHEMA_NAME;");

        } catch (DataAccessException ex) {
            LOG.error("Error accessing RAttr or RAttrTemplate table", ex);
        }
    }

    private void addTemplatesToMemberships() {
        try {
            LOG.info("Adding templates to memberships");
            for (Membership membership : membershipDAO.findAll()) {

                final List<MAttr> newMattrs = new ArrayList<MAttr>();
                final List<MAttrTemplate> newMattrTemplates = new ArrayList<MAttrTemplate>();

                final List<MDerAttr> newMDerattrs = new ArrayList<MDerAttr>();
                final List<MDerAttrTemplate> newMDerattrTemplates = new ArrayList<MDerAttrTemplate>();

                final List<MVirAttr> newMVirattrs = new ArrayList<MVirAttr>();
                final List<MVirAttrTemplate> newMVirattrTemplates = new ArrayList<MVirAttrTemplate>();

                LOG.info("Adding template to membership {}", membership);

                //Create normal attributes templates
                for (AbstractAttr attr : membership.getAttrs()) {
                    final MAttrTemplate mAttrTemplate = new MAttrTemplate();
                    mAttrTemplate.setOwner(membership.getSyncopeRole());
                    final String schemaName = jdbcTemplate.queryForObject(
                            "SELECT schema_name FROM MAttr WHERE id = ?;", String.class, attr.getId());

                    mAttrTemplate.setSchema(schemaDAO.find(schemaName, MSchema.class));
                    newMattrTemplates.add(mAttrTemplate);
                }

                //Create derived attributes templates
                for (AbstractDerAttr mDerAttr : membership.getDerAttrs()) {
                    final MDerAttrTemplate mDerattrTemplate = new MDerAttrTemplate();
                    mDerattrTemplate.setOwner(membership.getSyncopeRole());
                    final String mDerSchemaName = jdbcTemplate.queryForObject(
                            "SELECT DERIVEDSCHEMA_NAME FROM MDerAttr WHERE id = ?;", String.class, mDerAttr.getId());

                    mDerattrTemplate.setSchema(derSchemaDAO.find(mDerSchemaName, MDerSchema.class));
                    newMDerattrTemplates.add(mDerattrTemplate);
                }

                //Create virtual attributes templates
                for (AbstractVirAttr mVirAttr : membership.getVirAttrs()) {
                    final MVirAttrTemplate mVirattrTemplate = new MVirAttrTemplate();
                    mVirattrTemplate.setOwner(membership.getSyncopeRole());
                    final String virSchemaName = jdbcTemplate.queryForObject(
                            "SELECT VIRTUALSCHEMA_NAME FROM MVirAttr WHERE id = ?;", String.class, mVirAttr.getId());

                    mVirattrTemplate.setSchema(virSchemaDAO.find(virSchemaName, MVirSchema.class));
                    newMVirattrTemplates.add(mVirattrTemplate);
                }
                membership.setAttrs(newMattrs);
                membership.setDerAttrs(newMDerattrs);
                membership.setVirAttrs(newMVirattrs);
                membership.getSyncopeRole().getAttrTemplates(MAttrTemplate.class).addAll(newMattrTemplates);
                membership.getSyncopeRole().getAttrTemplates(MDerAttrTemplate.class).addAll(newMDerattrTemplates);
                membership.getSyncopeRole().getAttrTemplates(MVirAttrTemplate.class).addAll(newMVirattrTemplates);
            }
        } catch (DataAccessException ex) {
            LOG.error("Error accessing MAttr, MDerAttr or MVirAttr table", ex);
        }
    }

    private void addTemplatesToMAttrs() {
        try {
            LOG.info("Adding templates to membership attributes");
            for (Membership membership : membershipDAO.findAll()) {

                final List<MAttr> newMattrs = new ArrayList<MAttr>();
                final List<MDerAttr> newMDerattrs = new ArrayList<MDerAttr>();
                final List<MVirAttr> newMVirattrs = new ArrayList<MVirAttr>();

                // add template to normal attributes
                for (Long attrId : jdbcTemplate.queryForList("SELECT id FROM MAttr WHERE OWNER_ID = ?", Long.class,
                        membership.getId())) {
                    final MAttr mAttr = attrDAO.find(attrId, MAttr.class);

                    LOG.info("Adding template to membership normal attribute {}", mAttr);

                    final String schemaName = jdbcTemplate.queryForObject(
                            "SELECT schema_name FROM MAttr WHERE id = ?;", String.class, attrId);

                    mAttr.setTemplate(getTemplate(membership, schemaName, MAttrTemplate.class));
                    newMattrs.add(mAttr);
                }

                // add template to derived attributes
                for (Long attrId : jdbcTemplate.queryForList("SELECT id FROM MDerAttr WHERE OWNER_ID = ?", Long.class,
                        membership.getId())) {
                    final MDerAttr mDerAttr = derAttrDAO.find(attrId, MDerAttr.class);

                    LOG.info("Adding template to membership derived attribute {}", mDerAttr);

                    final String derSchemaName = jdbcTemplate.queryForObject(
                            "SELECT DERIVEDSCHEMA_NAME FROM MDerAttr WHERE id = ?;", String.class, attrId);

                    mDerAttr.setTemplate(getTemplate(membership, derSchemaName, MDerAttrTemplate.class));
                    newMDerattrs.add(mDerAttr);
                }

                // add template to virtual attributes
                for (Long attrId : jdbcTemplate.queryForList("SELECT id FROM MVirAttr WHERE OWNER_ID = ?", Long.class,
                        membership.getId())) {
                    final MVirAttr mVirAttr = virAttrDAO.find(attrId, MVirAttr.class);

                    LOG.info("Adding template to membership virtual attribute {}", mVirAttr);

                    final String virSchemaName = jdbcTemplate.queryForObject(
                            "SELECT VIRTUALSCHEMA_NAME FROM MVirAttr WHERE id = ?;", String.class, attrId);

                    mVirAttr.setTemplate(getTemplate(membership, virSchemaName, MVirAttrTemplate.class));
                    newMVirattrs.add(mVirAttr);
                }
                membership.setAttrs(newMattrs);
                membership.setDerAttrs(newMDerattrs);
                membership.setVirAttrs(newMVirattrs);
            }

            // delete obsolete columns
            jdbcTemplate.update("ALTER TABLE MAttr DROP COLUMN schema_name;");
            jdbcTemplate.update("ALTER TABLE MDerAttr DROP COLUMN DERIVEDSCHEMA_NAME;");
            jdbcTemplate.update("ALTER TABLE MVirAttr DROP COLUMN VIRTUALSCHEMA_NAME;");
        } catch (DataAccessException ex) {
            LOG.error("Error accessing MAttr or MAttrTemplate table", ex);
        }
    }

    private <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> T getTemplate(final Object obj,
            final String schemaName, final Class<T> reference) {
        T attrTemplate = null;
        for (Number number : attrTemplateDAO.findBySchemaName(schemaName, reference)) {
            final T attrTemplateTemp = attrTemplateDAO.find((Long) number, reference);

            if (attrTemplateTemp.getOwner().equals(obj instanceof SyncopeRole ? (SyncopeRole) obj : ((Membership) obj).
                    getSyncopeRole())) {
                attrTemplate = attrTemplateTemp;
            }
        }
        return attrTemplate;
    }
}
