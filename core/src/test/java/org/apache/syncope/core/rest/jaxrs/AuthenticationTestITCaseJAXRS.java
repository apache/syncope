package org.apache.syncope.core.rest.jaxrs;

import org.apache.syncope.core.rest.AuthenticationTestITCase;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AuthenticationTestITCaseJAXRS extends AuthenticationTestITCase {
    public AuthenticationTestITCaseJAXRS() {
        setEnabledCXF(true);
    }
}
