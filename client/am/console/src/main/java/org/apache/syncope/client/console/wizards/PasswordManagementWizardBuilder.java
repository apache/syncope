package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.PasswordManagementRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.util.ClassUtils;

public class PasswordManagementWizardBuilder extends BaseAjaxWizardBuilder<PasswordManagementTO> {

    private static final long serialVersionUID = -6355254150868269721L;

    protected final LoadableDetachableModel<List<String>> passwordManagementConfs = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return SyncopeWebApplication.get().getLookup().getClasses(PasswordManagementConf.class).stream().
                    map(Class::getName).sorted().collect(Collectors.toList());
        }
    };

    protected final PasswordManagementRestClient passwordManagementRestClient;

    protected final Model<Class<? extends PasswordManagementConf>> passwordManagementConfClass = Model.of();

    public PasswordManagementWizardBuilder(
            final PasswordManagementTO defaultItem,
            final PasswordManagementRestClient passwordManagementRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.passwordManagementRestClient = passwordManagementRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final PasswordManagementTO modelObject) {
        if (mode == AjaxWizard.Mode.CREATE) {
            passwordManagementRestClient.create(modelObject);
        } else {
            passwordManagementRestClient.update(modelObject);
        }

        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final PasswordManagementTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject, passwordManagementConfs, passwordManagementConfClass));
        wizardModel.add(new Configuration(modelObject));
        return wizardModel;
    }

    protected static class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Profile(
                final PasswordManagementTO passwordManagement,
                final LoadableDetachableModel<List<String>> passwordManagementConfs,
                final Model<Class<? extends PasswordManagementConf>> passwordManagementConfClass) {

            boolean isNew = passwordManagement.getConf() == null;
            if (!isNew) {
                passwordManagementConfClass.setObject(passwordManagement.getConf().getClass());
            }

            AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(passwordManagement, Constants.KEY_FIELD_NAME));
            key.addRequiredLabel();
            key.setEnabled(isNew);
            add(key);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, getString(Constants.DESCRIPTION_FIELD_NAME),
                    new PropertyModel<>(passwordManagement, Constants.DESCRIPTION_FIELD_NAME));
            add(description);

            AjaxCheckBoxPanel isEnabled = new AjaxCheckBoxPanel(
                    "enabled", "enabled", new PropertyModel<>(passwordManagement, "enabled"));
            add(isEnabled);

            AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>("conf", getString("type"), isNew
                    ? Model.of()
                    : Model.of(passwordManagement.getConf().getClass().getName()));
            conf.setChoices(passwordManagementConfs.getObject());
            conf.addRequiredLabel();
            conf.setNullValid(false);
            conf.setEnabled(isNew);
            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @SuppressWarnings("unchecked")
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    try {
                        Class<? extends PasswordManagementConf> clazz =
                                (Class<? extends PasswordManagementConf>) ClassUtils.resolveClassName(
                                        conf.getModelObject(), ClassUtils.getDefaultClassLoader());

                        passwordManagement.setConf(clazz.getConstructor().newInstance());
                        passwordManagementConfClass.setObject(clazz);
                    } catch (Exception e) {
                        LOG.error("Cannot instantiate {}", conf.getModelObject(), e);
                    }
                }
            });
            add(conf);
        }
    }

    protected class Configuration extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        Configuration(final PasswordManagementTO passwordManagement) {
            add(new BeanPanel<>("bean", new PropertyModel<>(passwordManagement, "conf"), pageRef,
                    "ldap", "keystore", "serviceProviderMetadata").setRenderBodyOnly(true));
        }
    }
}
