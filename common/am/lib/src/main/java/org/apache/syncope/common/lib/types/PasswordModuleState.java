package org.apache.syncope.common.lib.types;

public enum PasswordModuleState {
    /**
     * Active password management configuration,
     * This password management is used for CAS reset password flow.
     */
    ACTIVE,
    /**
     * Disabled password management configuration,
     * and is invoked by default automatically.
     */
    DISABLED
}
