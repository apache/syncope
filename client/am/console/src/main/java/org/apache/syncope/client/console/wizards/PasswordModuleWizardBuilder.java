package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.PasswordModuleRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.util.ClassUtils;

public class PasswordModuleWizardBuilder extends BaseAjaxWizardBuilder<PasswordModuleTO> {

    private static final long serialVersionUID = -6355254150868269721L;

    protected final LoadableDetachableModel<List<String>> passwordModuleConfs = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return SyncopeWebApplication.get().getLookup().getClasses(PasswordModuleConf.class).stream().
                    map(Class::getName).sorted().collect(Collectors.toList());
        }
    };

    protected final PasswordModuleRestClient passwordModuleRestClient;

    protected final Model<Class<? extends PasswordModuleConf>> passwordModuleConfClass = Model.of();

    public PasswordModuleWizardBuilder(
            final PasswordModuleTO defaultItem,
            final PasswordModuleRestClient passwordModuleRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.passwordModuleRestClient = passwordModuleRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final PasswordModuleTO modelObject) {
        if (mode == AjaxWizard.Mode.CREATE) {
            passwordModuleRestClient.create(modelObject);
        } else {
            passwordModuleRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final PasswordModuleTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject, passwordModuleConfs, passwordModuleConfClass));
        wizardModel.add(new Configuration(modelObject));
        //wizardModel.add(new AuthModuleWizardBuilder.Mapping(modelObject));
        return wizardModel;
    }

    protected static class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Profile(
                final PasswordModuleTO passwordModule,
                final LoadableDetachableModel<List<String>> passwordModuleConfs,
                final Model<Class<? extends PasswordModuleConf>> passwordModuleConfClass) {

            boolean isNew = passwordModule.getConf() == null;
            if (!isNew) {
                passwordModuleConfClass.setObject(passwordModule.getConf().getClass());
            }

            AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(passwordModule, Constants.KEY_FIELD_NAME));
            key.addRequiredLabel();
            key.setEnabled(isNew);
            add(key);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, getString(Constants.DESCRIPTION_FIELD_NAME),
                    new PropertyModel<>(passwordModule, Constants.DESCRIPTION_FIELD_NAME));
            add(description);

            add(new AjaxNumberFieldPanel.Builder<Integer>().build(
                    "order",
                    "order",
                    Integer.class,
                    new PropertyModel<>(passwordModule, "order")).addRequiredLabel());

            AjaxDropDownChoicePanel<String> conf = new AjaxDropDownChoicePanel<>("conf", getString("type"), isNew
                    ? Model.of()
                    : Model.of(passwordModule.getConf().getClass().getName()));
            conf.setChoices(passwordModuleConfs.getObject());
            conf.addRequiredLabel();
            conf.setNullValid(false);
            conf.setEnabled(isNew);
            conf.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -7133385027739964990L;

                @SuppressWarnings("unchecked")
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    try {
                        Class<? extends PasswordModuleConf> clazz =
                                (Class<? extends PasswordModuleConf>) ClassUtils.resolveClassName(
                                        conf.getModelObject(), ClassUtils.getDefaultClassLoader());

                        passwordModule.setConf(clazz.getConstructor().newInstance());
                        passwordModuleConfClass.setObject(clazz);
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

        Configuration(final PasswordModuleTO passwordModule) {
            add(new BeanPanel<>("bean", new PropertyModel<>(passwordModule, "conf"), pageRef,
                    "ldap", "keystore", "serviceProviderMetadata").setRenderBodyOnly(true));
        }
    }
}
