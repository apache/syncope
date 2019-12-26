package org.apache.syncope.client.console.panels;

import org.apache.syncope.client.console.wizards.any.UserWrapper;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardStep;

public class MergeLinkedAccountsReviewAccountsPanel extends WizardStep {
    public MergeLinkedAccountsReviewAccountsPanel(final UserWrapper userWrapper, final PageReference pageRef) {
        super();
        setOutputMarkupId(true);
    }
}
