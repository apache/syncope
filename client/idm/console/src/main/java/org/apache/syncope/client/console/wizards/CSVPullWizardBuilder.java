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
package org.apache.syncope.client.console.wizards;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.CSVConfPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.lang.Bytes;

public class CSVPullWizardBuilder extends BaseAjaxWizardBuilder<CSVPullSpec> {

    private static final long serialVersionUID = -4049787433975145383L;

    public enum LineSeparator {
        LF("\n"),
        CR("\r"),
        CRLF("\r\n");

        private final String repr;

        LineSeparator(final String repr) {
            this.repr = repr;
        }

        public String getRepr() {
            return repr;
        }

        public static LineSeparator byRepr(final String repr) {
            for (LineSeparator value : values()) {
                if (value.getRepr().equals(repr)) {
                    return value;
                }
            }
            return null;
        }
    }

    protected final Model<byte[]> csv = new Model<>();

    protected final ReconciliationRestClient reconciliationRestClient;

    protected final ImplementationRestClient implementationRestClient;

    protected final Bytes maxUploadSize;

    public CSVPullWizardBuilder(
            final CSVPullSpec defaultItem,
            final ReconciliationRestClient reconciliationRestClient,
            final ImplementationRestClient implementationRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.reconciliationRestClient = reconciliationRestClient;
        this.implementationRestClient = implementationRestClient;
        this.maxUploadSize = Bytes.megabytes(SyncopeWebApplication.get().getMaxUploadFileSizeMB());
    }

    @Override
    public CSVPullWizardBuilder setEventSink(final IEventSink eventSink) {
        super.setEventSink(eventSink);
        return this;
    }

    @Override
    protected ArrayList<ProvisioningReport> onApplyInternal(final CSVPullSpec modelObject) {
        return reconciliationRestClient.pull(modelObject, new ByteArrayInputStream(csv.getObject()));
    }

    @Override
    protected WizardModel buildModelSteps(final CSVPullSpec modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        wizardModel.add(new Columns(modelObject));
        wizardModel.add(new PullTask(modelObject));
        return wizardModel;
    }

    public class Details extends WizardStep {

        private static final long serialVersionUID = -4736870165235853919L;

        public Details(final CSVPullSpec spec) {
            FileInputConfig csvFile = new FileInputConfig().
                    showUpload(false).showRemove(false).showPreview(false).
                    browseClass("btn btn-success").browseIcon("<i class=\"fas fa-folder-open\"></i> &nbsp;");
            String language = SyncopeConsoleSession.get().getLocale().getLanguage();
            if (!Locale.ENGLISH.getLanguage().equals(language)) {
                csvFile.withLocale(language);
            }
            BootstrapFileInputField csvUpload =
                    new BootstrapFileInputField("csvUpload", new ListModel<>(new ArrayList<>()), csvFile);
            csvUpload.add(new AjaxFormSubmitBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = 5538299138211283825L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    FileUpload uploadedFile = csvUpload.getFileUpload();
                    if (uploadedFile != null) {
                        if (maxUploadSize != null && uploadedFile.getSize() > maxUploadSize.bytes()) {
                            SyncopeConsoleSession.get().error(getString("tooLargeFile").
                                    replace("${maxUploadSizeB}", String.valueOf(maxUploadSize.bytes())).
                                    replace("${maxUploadSizeMB}", String.valueOf(maxUploadSize.bytes() / 1000000L)));
                            ((BasePage) getPageReference().getPage()).getNotificationPanel().refresh(target);
                        } else {
                            csv.setObject(uploadedFile.getBytes());
                        }
                    }
                }
            });
            add(csvUpload.setRequired(true).setOutputMarkupId(true));

            add(new CSVConfPanel("csvconf", spec));
        }
    }

    public class Columns extends WizardStep implements WizardModel.ICondition {

        private static final long serialVersionUID = -4136764210454067728L;

        private final CSVPullSpec spec;

        private final ListModel<String> columnsModel = new ListModel<>();

        public Columns(final CSVPullSpec spec) {
            this.spec = spec;

            RadioGroup<String> keyColumn = new RadioGroup<>("keyColumn", new PropertyModel<>(spec, "keyColumn"));
            keyColumn.setOutputMarkupId(true);
            keyColumn.setOutputMarkupPlaceholderTag(true);
            keyColumn.setRenderBodyOnly(false);
            keyColumn.setRequired(true);
            add(keyColumn);

            keyColumn.add(new ListView<>("columns", columnsModel) {

                private static final long serialVersionUID = -9112553137618363167L;

                @Override
                protected void populateItem(final ListItem<String> item) {
                    item.add(new Label("column", item.getModelObject()));
                    item.add(new Radio<>("key", new Model<>(item.getModelObject()), keyColumn));
                    item.add(new AjaxCheckBox("ignore", new Model<>()) {

                        private static final long serialVersionUID = -6139318907146065915L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            if (spec.getIgnoreColumns().contains(item.getModelObject())) {
                                spec.getIgnoreColumns().remove(item.getModelObject());
                            } else {
                                spec.getIgnoreColumns().add(item.getModelObject());
                            }
                        }
                    });
                }
            }.setReuseItems(true));
        }

        @Override
        public boolean evaluate() {
            if (csv.getObject() != null) {
                String header = StringUtils.substringBefore(new String(csv.getObject()), spec.getLineSeparator());
                columnsModel.setObject(Stream.of(StringUtils.split(header, spec.getColumnSeparator())).
                        map(value -> {
                            String unquoted = value;
                            if (unquoted.charAt(0) == spec.getQuoteChar()) {
                                unquoted = unquoted.substring(1);
                            }
                            if (unquoted.charAt(unquoted.length() - 1) == spec.getQuoteChar()) {
                                unquoted = unquoted.substring(0, unquoted.length() - 1);
                            }
                            return unquoted;
                        }).collect(Collectors.toList()));
            }

            return true;
        }
    }

    public class PullTask extends WizardStep {

        private static final long serialVersionUID = -8954789648303078732L;

        private final IModel<List<String>> inboundActions = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 4659376149825914247L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.INBOUND_ACTIONS).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };

        private final IModel<List<String>> inboundCorrelationRules = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 4659376149825914247L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.INBOUND_CORRELATION_RULE).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };

        public PullTask(final CSVPullSpec spec) {
            AjaxCheckBoxPanel remediation = new AjaxCheckBoxPanel(
                    "remediation", "remediation", new PropertyModel<>(spec, "remediation"), false);
            add(remediation);

            AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<>(spec, "matchingRule"), false);
            matchingRule.setChoices(List.of(MatchingRule.values()));
            add(matchingRule);

            AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<>(spec, "unmatchingRule"),
                    false);
            unmatchingRule.setChoices(List.of(UnmatchingRule.values()));
            add(unmatchingRule);

            AjaxPalettePanel<String> provisioningActions = new AjaxPalettePanel.Builder<String>().build(
                    "provisioningActions",
                    new PropertyModel<>(spec, "provisioningActions"),
                    new ListModel<>(inboundActions.getObject()));
            add(provisioningActions);

            AjaxDropDownChoicePanel<ConflictResolutionAction> conflictResolutionAction = new AjaxDropDownChoicePanel<>(
                    "conflictResolutionAction", "conflictResolutionAction",
                    new PropertyModel<>(spec, "conflictResolutionAction"), false);
            conflictResolutionAction.setChoices(List.of(ConflictResolutionAction.values()));
            conflictResolutionAction.setRequired(true);
            add(conflictResolutionAction);

            AjaxDropDownChoicePanel<String> inboundCorrelationRule = new AjaxDropDownChoicePanel<>(
                    "inboundCorrelationRule", "inboundCorrelationRule",
                    new PropertyModel<>(spec, "inboundCorrelationRule"),
                    false);
            inboundCorrelationRule.setChoices(inboundCorrelationRules);
            add(inboundCorrelationRule);
        }
    }
}
