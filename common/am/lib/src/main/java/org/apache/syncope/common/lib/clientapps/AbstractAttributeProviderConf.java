package org.apache.syncope.common.lib.clientapps;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.CaseCanonicalizationMode;

public abstract class AbstractAttributeProviderConf implements UsernameAttributeProviderConf {

    private static final long serialVersionUID = 497016622295991904L;

    protected CaseCanonicalizationMode caseCanonicalizationMode = CaseCanonicalizationMode.NONE;

    public CaseCanonicalizationMode getCaseCanonicalizationMode() {
        return caseCanonicalizationMode;
    }

    public void setCaseCanonicalizationMode(final CaseCanonicalizationMode caseCanonicalizationMode) {
        this.caseCanonicalizationMode = caseCanonicalizationMode;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(caseCanonicalizationMode).
                toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        AbstractAttributeProviderConf rhs = (AbstractAttributeProviderConf) obj;
        return new EqualsBuilder().
                append(this.caseCanonicalizationMode, rhs.caseCanonicalizationMode).
                isEquals();
    }
}
