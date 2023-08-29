package org.apache.syncope.common.lib.clientapps;

import org.apache.syncope.common.lib.attr.CaseCanonicalizationMode;

public class BaseAttributeProviderConf {
    protected CaseCanonicalizationMode caseCanonicalizationMode;

    public CaseCanonicalizationMode getCaseCanonicalizationMode() {
        return caseCanonicalizationMode;
    }

    public void setCaseCanonicalizationMode(
            final CaseCanonicalizationMode caseCanonicalizationMode) {
        this.caseCanonicalizationMode = caseCanonicalizationMode;
    }
}
