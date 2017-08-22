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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateLogic extends AbstractTransactionalLogic<ReportTemplateTO> {

    @Autowired
    private ReportTemplateDAO reportTemplateDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private EntityFactory entityFactory;

    private ReportTemplateTO getReportTemplateTO(final String key) {
        ReportTemplateTO reportTemplateTO = new ReportTemplateTO();
        reportTemplateTO.setKey(key);
        return reportTemplateTO;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_TEMPLATE_READ + "')")
    public ReportTemplateTO read(final String key) {
        ReportTemplate reportTemplate = reportTemplateDAO.find(key);
        if (reportTemplate == null) {
            LOG.error("Could not find report template '" + key + "'");

            throw new NotFoundException(key);
        }

        return getReportTemplateTO(key);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_TEMPLATE_LIST + "')")
    public List<ReportTemplateTO> list() {
        return reportTemplateDAO.findAll().stream().
                map(template -> getReportTemplateTO(template.getKey())).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_TEMPLATE_CREATE + "')")
    public ReportTemplateTO create(final String key) {
        if (reportTemplateDAO.find(key) != null) {
            throw new DuplicateException(key);
        }
        ReportTemplate reportTemplate = entityFactory.newEntity(ReportTemplate.class);
        reportTemplate.setKey(key);
        reportTemplateDAO.save(reportTemplate);

        return getReportTemplateTO(key);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_TEMPLATE_READ + "')")
    public String getFormat(final String key, final ReportTemplateFormat format) {
        ReportTemplate reportTemplate = reportTemplateDAO.find(key);
        if (reportTemplate == null) {
            LOG.error("Could not find report template '" + key + "'");

            throw new NotFoundException(key);
        }

        String template = format == ReportTemplateFormat.HTML
                ? reportTemplate.getHTMLTemplate()
                : format == ReportTemplateFormat.CSV
                        ? reportTemplate.getCSVTemplate()
                        : reportTemplate.getFOTemplate();
        if (StringUtils.isBlank(template)) {
            LOG.error("Could not find report template '" + key + "' in " + format + " format");

            throw new NotFoundException(key + " in " + format);
        }

        return template;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_TEMPLATE_UPDATE + "')")
    public void setFormat(final String key, final ReportTemplateFormat format, final String template) {
        ReportTemplate reportTemplate = reportTemplateDAO.find(key);
        if (reportTemplate == null) {
            LOG.error("Could not find report template '" + key + "'");

            throw new NotFoundException(key);
        }

        switch (format) {
            case CSV:
                reportTemplate.setCSVTemplate(template);
                break;

            case FO:
                reportTemplate.setFOTemplate(template);
                break;

            case HTML:
                reportTemplate.setHTMLTemplate(template);
                break;

            default:
        }

        reportTemplateDAO.save(reportTemplate);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REPORT_TEMPLATE_DELETE + "')")
    public ReportTemplateTO delete(final String key) {
        ReportTemplate reportTemplate = reportTemplateDAO.find(key);
        if (reportTemplate == null) {
            LOG.error("Could not find report template '" + key + "'");

            throw new NotFoundException(key);
        }

        List<Report> reports = reportDAO.findByTemplate(reportTemplate);
        if (!reports.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InUseByNotifications);
            sce.getElements().addAll(reports.stream().map(Entity::getKey).collect(Collectors.toList()));
            throw sce;
        }

        ReportTemplateTO deleted = getReportTemplateTO(key);
        reportTemplateDAO.delete(key);
        return deleted;
    }

    @Override
    protected ReportTemplateTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = ((String) args[i]);
                } else if (args[i] instanceof ReportTemplateTO) {
                    key = ((ReportTemplateTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return getReportTemplateTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
