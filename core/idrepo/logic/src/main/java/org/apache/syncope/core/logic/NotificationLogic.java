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
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class NotificationLogic extends AbstractJobLogic<NotificationTO> {

    protected final NotificationDAO notificationDAO;

    protected final NotificationDataBinder binder;

    public NotificationLogic(
            final JobManager jobManager,
            final SchedulerFactoryBean scheduler,
            final NotificationDAO notificationDAO,
            final NotificationDataBinder binder) {

        super(jobManager, scheduler);

        this.notificationDAO = notificationDAO;
        this.binder = binder;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_READ + "')")
    @Transactional(readOnly = true)
    public NotificationTO read(final String key) {
        Notification notification = notificationDAO.find(key);
        if (notification == null) {
            LOG.error("Could not find notification '" + key + '\'');

            throw new NotFoundException(String.valueOf(key));
        }

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_LIST + "')")
    @Transactional(readOnly = true)
    public List<NotificationTO> list() {
        return notificationDAO.findAll().stream().map(binder::getNotificationTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_CREATE + "')")
    public NotificationTO create(final NotificationTO notificationTO) {
        return binder.getNotificationTO(notificationDAO.save(binder.create(notificationTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_UPDATE + "')")
    public NotificationTO update(final NotificationTO notificationTO) {
        Notification notification = notificationDAO.find(notificationTO.getKey());
        if (notification == null) {
            LOG.error("Could not find notification '" + notificationTO.getKey() + '\'');
            throw new NotFoundException(String.valueOf(notificationTO.getKey()));
        }

        binder.update(notification, notificationTO);
        notification = notificationDAO.save(notification);

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_DELETE + "')")
    public NotificationTO delete(final String key) {
        Notification notification = notificationDAO.find(key);
        if (notification == null) {
            LOG.error("Could not find notification '" + key + '\'');

            throw new NotFoundException(String.valueOf(key));
        }

        NotificationTO deleted = binder.getNotificationTO(notification);
        notificationDAO.delete(key);
        return deleted;
    }

    @Override
    protected Triple<JobType, String, String> getReference(final JobKey jobKey) {
        return JobManager.NOTIFICATION_JOB.equals(jobKey)
                ? Triple.of(JobType.NOTIFICATION, (String) null, NotificationJob.class.getSimpleName())
                : null;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_LIST + "')")
    public JobTO getJob() {
        List<JobTO> jobs = super.doListJobs(false);
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.NOTIFICATION_EXECUTE + "')")
    public void actionJob(final JobAction action) {
        super.doActionJob(JobManager.NOTIFICATION_JOB, action);
    }

    @Override
    protected NotificationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof NotificationTO) {
                    key = ((NotificationTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getNotificationTO(notificationDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
