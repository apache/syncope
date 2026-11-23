package org.apache.syncope.core.flowable.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.Test;

class UELSandboxTest {

    @Test
    void allowedExpressions() {
        String[] allowed = new String[] {
            "${user.checkToken(token)}",
            "${wfExecutor == user.getUsername() and task == 'update' and userUR.requiresApproval()}",
            "${flowableUtils.isUserIngroup(userTO, 'groupForWorkflowOptIn')}"
        };
        for (String expr : allowed) {
            assertTrue(UELSandbox.isAllowed(expr), () -> "Expression should be allowed: " + expr);
        }
    }

    @Test
    void blockedExpressions() {
        String[] blocked = new String[] {
            "${''.getClass().forName('java.io.File').getConstructor(''.getClass())."
            + "newInstance('/tmp/syncope_conditionexpression_check.txt')}",
            "${java.lang.Runtime.getRuntime().exec('whoami')}"
        };
        for (String expr : blocked) {
            assertFalse(UELSandbox.isAllowed(expr), () -> "Expression should be blocked: " + expr);
        }
    }

    @Test
    void allWhitelistedMethods() throws Exception {
        Field whitelistField = UELSandbox.class.getDeclaredField("WHITELIST");
        whitelistField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> whitelist = (Map<String, Set<String>>) whitelistField.get(null);
        for (Map.Entry<String, Set<String>> entry : whitelist.entrySet()) {
            String bean = entry.getKey();
            Class<?> cls = switch (bean) {
                case "user" ->
                    User.class;
                case "userCR" ->
                    UserCR.class;
                case "userUR" ->
                    UserUR.class;
                case "userTO" ->
                    UserTO.class;
                case "flowableUtils" ->
                    FlowableWorkflowUtils.class;
                default ->
                    Object.class;
            };

            for (String method : entry.getValue()) {
                // Skip methods that require parameters
                try {
                    if (cls.getMethod(method).getParameterCount() == 0) {
                        String expr = "${" + bean + "." + method + "()}";
                        assertTrue(UELSandbox.isAllowed(expr), () -> "Expression should be allowed: " + expr);
                    }
                } catch (NoSuchMethodException ignored) {
                    // method not found or non‑public; ignore
                }
            }
        }
    }
}
