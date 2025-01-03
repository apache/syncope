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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.EncryptedFieldPanel;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class DomainWizardBuilder extends BaseAjaxWizardBuilder<Domain> {

    private static final long serialVersionUID = -6731030158762705250L;

    private static final List<String> JDBC_DRIVERS = List.of(
            "org.postgresql.Driver",
            "com.mysql.cj.jdbc.Driver",
            "org.mariadb.jdbc.Driver",
            "oracle.jdbc.OracleDriver",
            "org.h2.Driver");

    private static final List<String> DATABASE_PLATFORMS = List.of(
            "org.apache.openjpa.jdbc.sql.PostgresDictionary",
            "org.apache.openjpa.jdbc.sql.MySQLDictionary"
            + "(blobTypeName=LONGBLOB,dateFractionDigits=3,useSetStringForClobs=true)",
            "org.apache.openjpa.jdbc.sql.MariaDBDictionary"
            + "(blobTypeName=LONGBLOB,dateFractionDigits=3)",
            "org.apache.openjpa.jdbc.sql.OracleDictionary",
            "org.apache.openjpa.jdbc.sql.H2Dictionary");

    private final DomainOps domainOps;

    public DomainWizardBuilder(final DomainOps domainOps, final Domain domain, final PageReference pageRef) {
        super(domain, pageRef);
        this.domainOps = domainOps;
    }

    @Override
    protected Serializable onApplyInternal(final Domain domain) {
        domainOps.create(domain);
        return domain;
    }

    @Override
    protected WizardModel buildModelSteps(final Domain domain, final WizardModel wizardModel) {
        wizardModel.add(domain instanceof Neo4jDomain ? new Neo4jStorage(domain) : new JPAStorage(domain));
        wizardModel.add(new AdminCredentials(domain));
        wizardModel.add(new Content(domain));
        wizardModel.add(new KeymasterConfParams(domain, pageRef));
        return wizardModel;
    }

    public static class JPAStorage extends WizardStep {

        private static final long serialVersionUID = 3671044119870133102L;

        public JPAStorage(final Domain domain) {
            add(new AjaxTextFieldPanel(
                    "key",
                    "key",
                    new PropertyModel<>(domain, "key")).addRequiredLabel());

            AjaxDropDownChoicePanel<String> jdbcDriver = new AjaxDropDownChoicePanel<>(
                    "jdbcDriver", "jdbcDriver", new PropertyModel<>(domain, "jdbcDriver"), false);
            jdbcDriver.setChoices(JDBC_DRIVERS);
            jdbcDriver.addRequiredLabel();
            jdbcDriver.setNullValid(false);
            add(jdbcDriver);

            add(new AjaxTextFieldPanel(
                    "jdbcURL",
                    "jdbcURL",
                    new PropertyModel<>(domain, "jdbcURL")).addRequiredLabel());

            add(new AjaxTextFieldPanel(
                    "dbSchema",
                    "dbSchema",
                    new PropertyModel<>(domain, "dbSchema")).setRequired(false));

            add(new AjaxTextFieldPanel(
                    "dbUsername",
                    "dbUsername",
                    new PropertyModel<>(domain, "dbUsername")).addRequiredLabel());
            add(new EncryptedFieldPanel(
                    "dbPassword",
                    "dbPassword",
                    new PropertyModel<>(domain, "dbPassword"), false));

            AjaxDropDownChoicePanel<JPADomain.TransactionIsolation> transactionIsolation =
                    new AjaxDropDownChoicePanel<>(
                            "transactionIsolation", "transactionIsolation",
                            new PropertyModel<>(domain, "transactionIsolation"), false);
            transactionIsolation.setChoices(List.of(JPADomain.TransactionIsolation.values()));
            transactionIsolation.addRequiredLabel();
            transactionIsolation.setNullValid(false);
            add(transactionIsolation);

            add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).build(
                    "poolMaxActive",
                    "poolMaxActive",
                    Integer.class,
                    new PropertyModel<>(domain, "poolMaxActive")).addRequiredLabel());
            add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).build(
                    "poolMinIdle",
                    "poolMinIdle",
                    Integer.class,
                    new PropertyModel<>(domain, "poolMinIdle")).addRequiredLabel());

            add(new AjaxTextFieldPanel(
                    "orm",
                    "orm",
                    new PropertyModel<>(domain, "orm")).addRequiredLabel());

            AjaxDropDownChoicePanel<String> databasePlatform = new AjaxDropDownChoicePanel<>(
                    "databasePlatform", "databasePlatform", new PropertyModel<>(domain, "databasePlatform"), false);
            databasePlatform.setChoices(DATABASE_PLATFORMS);
            databasePlatform.addRequiredLabel();
            databasePlatform.setNullValid(false);
            add(databasePlatform);
        }
    }

    public static class Neo4jStorage extends WizardStep {

        private static final long serialVersionUID = 3671044119870133102L;

        public Neo4jStorage(final Domain domain) {
            add(new AjaxTextFieldPanel(
                    "key",
                    "key",
                    new PropertyModel<>(domain, "key")).addRequiredLabel());

            PropertyModel<URI> uriModel = new PropertyModel<>(domain, "uri");
            add(new AjaxTextFieldPanel(
                    "uri",
                    "uri",
                    new IModel<>() {

                private static final long serialVersionUID = 807008909842554829L;

                @Override
                public String getObject() {
                    return Optional.ofNullable(uriModel.getObject()).map(URI::toASCIIString).orElse(null);
                }

                @Override
                public void setObject(final String object) {
                    uriModel.setObject(URI.create(object));
                }
            }).addRequiredLabel());

            add(new AjaxTextFieldPanel(
                    "username",
                    "username",
                    new PropertyModel<>(domain, "username")).addRequiredLabel());
            add(new EncryptedFieldPanel(
                    "password",
                    "password",
                    new PropertyModel<>(domain, "password"), false));
        }
    }

    public static class AdminCredentials extends WizardStep {

        private static final long serialVersionUID = -7472243942630790243L;

        public AdminCredentials(final Domain domain) {
            AjaxDropDownChoicePanel<CipherAlgorithm> adminCipherAlgorithm = new AjaxDropDownChoicePanel<>(
                    "adminCipherAlgorithm", "adminCipherAlgorithm",
                    new PropertyModel<>(domain, "adminCipherAlgorithm"), false);
            adminCipherAlgorithm.setChoices(List.of(CipherAlgorithm.values()));
            adminCipherAlgorithm.addRequiredLabel();
            adminCipherAlgorithm.setNullValid(false);
            add(adminCipherAlgorithm);

            EncryptedFieldPanel adminPassword = new EncryptedFieldPanel(
                    "adminPassword", "adminPassword", new PropertyModel<>(domain, "adminPassword"), false);
            adminPassword.addRequiredLabel();
            add(adminPassword);
        }
    }

    public class Content extends WizardStep {

        private static final long serialVersionUID = -4214163958296844853L;

        public Content(final Domain domain) {
            XMLEditorPanel content = new XMLEditorPanel(
                    null, new PropertyModel<>(domain, "content"), false, pageRef);
            content.setOutputMarkupPlaceholderTag(true);
            content.setVisible(false);
            add(content);

            AjaxCheckBoxPanel defaultContent = new AjaxCheckBoxPanel(
                    "defaultContent", "defaultContent", Model.of(true), true);
            defaultContent.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    content.setVisible(!BooleanUtils.toBoolean(defaultContent.getField().getConvertedInput()));
                    target.add(content);
                }
            });
            add(defaultContent);
        }
    }

    public static class KeymasterConfParams extends WizardStep {

        private static final long serialVersionUID = -8448363577805933925L;

        public KeymasterConfParams(final Domain domain, final PageReference pageRef) {
            JsonEditorPanel keymasterConfParams = new JsonEditorPanel(
                    null, new PropertyModel<>(domain, "keymasterConfParams"), false, pageRef);
            keymasterConfParams.setOutputMarkupPlaceholderTag(true);
            keymasterConfParams.setVisible(false);
            add(keymasterConfParams);

            AjaxCheckBoxPanel defaultKeymasterConfParams = new AjaxCheckBoxPanel(
                    "defaultKeymasterConfParams", "defaultKeymasterConfParams", Model.of(true), true);
            defaultKeymasterConfParams.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -6139318907146065915L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    keymasterConfParams.setVisible(
                            !BooleanUtils.toBoolean(defaultKeymasterConfParams.getField().getConvertedInput()));
                    target.add(keymasterConfParams);
                }
            });
            add(defaultKeymasterConfParams);
        }
    }
}
