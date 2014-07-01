package org.apache.syncope.installer.validators;

import com.izforge.izpack.api.installer.DataValidator;

public abstract class AbstractValidator implements DataValidator {

    protected boolean isEmpty(final String string) {
        return !(string != null && string.length() != 0);
    }
}
