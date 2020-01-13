package org.apache.syncope.client.console.commons;

import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;

import java.io.Serializable;

public class MergeLinkedAccountsWizardModel implements Serializable {
    private final UserTO baseUser;

    private UserTO mergingUser;

    private ResourceTO resource;

    public MergeLinkedAccountsWizardModel(final UserTO baseUser) {
        this.baseUser = baseUser;
    }

    public ResourceTO getResource() {
        return resource;
    }

    public UserTO getBaseUser() {
        return baseUser;
    }

    public UserTO getMergingUser() {
        return mergingUser;
    }

    public void setMergingUser(final UserTO mergingUser) {
        this.mergingUser = mergingUser;
    }

    public void setResource(final ResourceTO resource) {
        this.resource = resource;
    }
}
