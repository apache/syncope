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
package org.apache.syncope.client.console.policies;

import java.io.Serializable;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TicketExpirationPolicyModalPanel extends AbstractModalPanel<TicketExpirationPolicyTO> {

    private static final long serialVersionUID = 2668291404983623500L;

    @SpringBean
    protected PolicyRestClient policyRestClient;

    protected final IModel<TicketExpirationPolicyTO> model;

    public TicketExpirationPolicyModalPanel(
            final BaseModal<TicketExpirationPolicyTO> modal,
            final IModel<TicketExpirationPolicyTO> model,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.model = model;

        conf("tgtConf", new DefaultTicketExpirationPolicyConf.TGTConf());
        conf("stConf", new DefaultTicketExpirationPolicyConf.STConf());
        conf("proxyTgtConf", new DefaultTicketExpirationPolicyConf.TGTConf());
        conf("proxyStConf", new DefaultTicketExpirationPolicyConf.STConf());
    }

    private <T extends Serializable> void conf(final String field, final T newInstance) {
        PropertyModel<T> beanPanelModel = new PropertyModel<>(model.getObject(), "conf." + field);

        AjaxCheckBoxPanel enable = new AjaxCheckBoxPanel("enable." + field, "enable." + field, new IModel<Boolean>() {

            private static final long serialVersionUID = -7126718045816207110L;

            @Override
            public Boolean getObject() {
                return beanPanelModel.getObject() != null;
            }

            @Override
            public void setObject(final Boolean object) {
                // nothing to do
            }
        });
        enable.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (beanPanelModel.getObject() == null) {
                    beanPanelModel.setObject(newInstance);
                } else {
                    beanPanelModel.setObject(null);
                }
                target.add(TicketExpirationPolicyModalPanel.this);
            }
        });
        add(enable);

        add(new BeanPanel<>("bean." + field, beanPanelModel, pageRef).setRenderBodyOnly(true));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            policyRestClient.update(PolicyType.TICKET_EXPIRATION, model.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating Ticket Expiration Policy {}", model.getObject().getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
