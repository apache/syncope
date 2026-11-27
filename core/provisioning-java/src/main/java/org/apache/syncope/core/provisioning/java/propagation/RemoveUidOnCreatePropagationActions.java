package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.implementation.InstanceScope;
import org.apache.syncope.core.spring.implementation.SyncopeImplementation;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class RemoveUidOnCreatePropagationActions implements PropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(RemoveUidOnCreatePropagationActions.class);

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected ApplicationEventPublisher publisher;

    @Override
    public void after(
            final PropagationTaskInfo taskInfo,
            final TaskExec<PropagationTask> execution,
            final ConnectorObject afterObj) {
        if (ResourceOperation.DELETE == taskInfo.getOperation()) {
            taskInfo.getResource()
                    .getProvisionByAnyType(taskInfo.getAnyType())
                    .filter(provision -> provision.getUidOnCreate() != null)
                    .ifPresent(provision -> {
                        LOG.debug("Removing uidOnCreate [{}] attribute from [{}] on delete", provision.getUidOnCreate(),
                                taskInfo.getEntityKey());
                        AnyUtils anyUtils = anyUtilsFactory.getInstance(taskInfo.getAnyTypeKind());
                        // perform a preliminary search to check user existence
                        anyUtils.dao().findById(taskInfo.getEntityKey()).ifPresentOrElse(any -> {
                            anyUtils.removeAttr(taskInfo.getEntityKey(),
                                    plainSchemaDAO.findById(provision.getUidOnCreate())
                                            .orElseThrow(() -> new NotFoundException(
                                                    "PlainSchema " + (provision.getUidOnCreate()))));
                            publisher.publishEvent(new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, any,
                                    AuthContextUtils.getDomain()));
                        }, () -> LOG.warn(
                                "Any not found, it is not necessary to remove uidOnCreate [{}] attribute from "
                                        + "[{}] on delete", provision.getUidOnCreate(), taskInfo.getEntityKey()));
                    });
        }
    }
}
