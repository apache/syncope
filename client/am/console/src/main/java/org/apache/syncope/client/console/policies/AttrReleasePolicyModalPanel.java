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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.AttrRepoRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxGridFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf.PrincipalAttrRepoMergingStrategy;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttrReleasePolicyModalPanel extends AbstractModalPanel<AttrReleasePolicyTO> {

    private static final long serialVersionUID = 2668291404983623500L;

    @SpringBean
    protected PolicyRestClient policyRestClient;

    @SpringBean
    protected AttrRepoRestClient attrRepoRestClient;

    protected final IModel<List<String>> allAttrRepos = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected List<String> load() {
            return attrRepoRestClient.list().stream().map(AttrRepoTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final IModel<AttrReleasePolicyTO> model;

    public AttrReleasePolicyModalPanel(
            final BaseModal<AttrReleasePolicyTO> modal,
            final IModel<AttrReleasePolicyTO> model,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.model = model;

        add(new AjaxGridFieldPanel<>(
                "releaseAttrs", "releaseAttrs",
                new PropertyModel<>(model.getObject().getConf(), "releaseAttrs")));

        AjaxTextFieldPanel allowedAttr = new AjaxTextFieldPanel("panel", "allowedAttrs", new Model<>());
        add(new MultiFieldPanel.Builder<String>(
                new PropertyModel<>(model.getObject().getConf(), "allowedAttrs")).build(
                "allowedAttrs",
                "allowedAttrs",
                allowedAttr));

        AjaxTextFieldPanel excludedAttr = new AjaxTextFieldPanel("panel", "excludedAttrs", new Model<>());
        add(new MultiFieldPanel.Builder<String>(
                new PropertyModel<>(model.getObject().getConf(), "excludedAttrs")).build(
                "excludedAttrs",
                "excludedAttrs",
                excludedAttr));

        AjaxTextFieldPanel includeOnlyAttr = new AjaxTextFieldPanel("panel", "includeOnlyAttrs", new Model<>());
        add(new MultiFieldPanel.Builder<String>(
                new PropertyModel<>(model.getObject().getConf(), "includeOnlyAttrs")).build(
                "includeOnlyAttrs",
                "includeOnlyAttrs",
                includeOnlyAttr));

        add(new AjaxTextFieldPanel(
                "principalIdAttr", "principalIdAttr",
                new PropertyModel<>(model.getObject().getConf(), "principalIdAttr")));

        AjaxDropDownChoicePanel<PrincipalAttrRepoMergingStrategy> mergingStrategy = new AjaxDropDownChoicePanel<>(
                "mergingStrategy", "mergingStrategy",
                new PropertyModel<>(model.getObject().getConf(), "principalAttrRepoConf.mergingStrategy"));
        mergingStrategy.setChoices(List.of(PrincipalAttrRepoMergingStrategy.values()));
        mergingStrategy.addRequiredLabel();
        mergingStrategy.setNullValid(false);
        add(mergingStrategy);

        add(new AjaxCheckBoxPanel(
                "ignoreResolvedAttributes",
                "ignoreResolvedAttributes",
                new PropertyModel<>(model.getObject().getConf(), "principalAttrRepoConf.ignoreResolvedAttributes"),
                false));

        add(new AjaxNumberFieldPanel.Builder<Long>().build(
                "expiration",
                "expiration",
                Long.class,
                new PropertyModel<>(model.getObject().getConf(), "principalAttrRepoConf.expiration")));

        AjaxDropDownChoicePanel<TimeUnit> timeUnit = new AjaxDropDownChoicePanel<>(
                "timeUnit", "timeUnit",
                new PropertyModel<>(model.getObject().getConf(), "principalAttrRepoConf.timeUnit"));
        timeUnit.setChoices(List.of(TimeUnit.values()));
        timeUnit.addRequiredLabel();
        timeUnit.setNullValid(false);
        add(timeUnit);

        add(new AjaxPalettePanel.Builder<String>().setName("attrRepos").build(
                "attrRepos",
                new PropertyModel<>(model.getObject().getConf(), "principalAttrRepoConf.attrRepos"),
                allAttrRepos));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            policyRestClient.update(PolicyType.ATTR_RELEASE, model.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating Attribute Release Policy {}", model.getObject().getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
