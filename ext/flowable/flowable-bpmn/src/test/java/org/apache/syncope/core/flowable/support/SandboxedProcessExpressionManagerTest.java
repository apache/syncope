/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.flowable.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.flowable.impl.FlowableWorkflowUtils;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;

class SandboxedProcessExpressionManagerTest {

    private static final class TestVariableContainer implements VariableContainer {

        private final Map<String, Object> variables = new HashMap<>();

        private TestVariableContainer(final Map<String, Object> variables) {
            this.variables.putAll(variables);
        }

        @Override
        public boolean hasVariable(final String variableName) {
            return variables.containsKey(variableName);
        }

        @Override
        public Object getVariable(final String variableName) {
            return variables.get(variableName);
        }

        @Override
        public void setVariable(final String variableName, final Object variableValue) {
            variables.put(variableName, variableValue);
        }

        @Override
        public void setTransientVariable(final String variableName, final Object variableValue) {
            variables.put(variableName, variableValue);
        }

        @Override
        public String getTenantId() {
            return null;
        }

        @Override
        public Set<String> getVariableNames() {
            return variables.keySet();
        }
    }

    @TempDir
    private Path tempDir;

    private final SandboxedProcessExpressionManager expressionManager =
            new SandboxedProcessExpressionManager(new HashMap<>());

    private Object evaluate(final String expression, final VariableContainer variableContainer) {
        return expressionManager.createExpression(expression).getValue(variableContainer);
    }

    @Test
    void blocksBracketNotationPayload() {
        Path target = tempDir.resolve("bracket-notation");
        String expression = "${''['getClass']()['forName']('java.lang.Runtime')"
                + "['getMethod']('getRuntime')['invoke'](null)['exec']('touch " + target + "') != null}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksDotNotationPayload() {
        Path target = tempDir.resolve("dot-notation");
        String expression = "${''.getClass().forName('java.lang.Runtime')"
                + ".getMethod('getRuntime').invoke(null).exec('touch " + target + "') != null}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksHashNotationPayload() {
        Path target = tempDir.resolve("hash-notation");
        String expression = "#{\"\".getClass().forName(\"java.lang.Runtime\")"
                + ".getMethod(\"getRuntime\").invoke(null).exec(\"touch " + target + "\").toString()}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksPayloadWithClosingBraceInStringLiteral() {
        Path target = tempDir.resolve("closing-brace");
        String expression = "${'}'.concat(\"\".getClass().forName(\"java.lang.Runtime\")"
                + ".getMethod(\"getRuntime\").invoke(null).exec(\"touch " + target + "\").toString())}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksDoubleQuotedPayloadWithMultilineArgument() {
        Path target = tempDir.resolve("multiline");
        String expression = "${\"\".getClass().forName(\"java.lang.Runtime\")"
                + ".getMethod(\"getRuntime\").invoke(null).exec(\"touch\n" + target + "\") != null}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksClassProperty() {
        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate("${''.class}", VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown property used in expression"), ex.getMessage());
    }

    @Test
    void blocksGetClassOnWhitelistedType() {
        UserTO userTO = new UserTO();

        FlowableException ex = assertThrows(FlowableException.class, () -> evaluate(
                "${userTO.getClass()}", new TestVariableContainer(Map.of("userTO", userTO))));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
    }

    @Test
    void blocksClassPropertyOnWhitelistedType() {
        UserTO userTO = new UserTO();

        assertThrows(FlowableException.class,
                () -> evaluate("${userTO.class}", new TestVariableContainer(Map.of("userTO", userTO))));
    }

    @Test
    void blocksNonWhitelistedMethod() {
        assertThrows(FlowableException.class,
                () -> evaluate("${'abc'.toUpperCase()}", VariableContainer.empty()));
    }

    @Test
    void blocksRuntimeBaseType() {
        TestVariableContainer variables = new TestVariableContainer(Map.of("runtime", Runtime.getRuntime()));

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate("${runtime.exec('echo sandbox')}", variables));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
    }

    @Test
    void allowsUserMethods() {
        User user = mock(User.class);
        when(user.checkToken("token")).thenReturn(true);
        when(user.getUsername()).thenReturn("admin");

        TestVariableContainer variables = new TestVariableContainer(Map.of("user", user, "token", "token"));
        assertEquals(true, evaluate("${user.checkToken(token)}", variables));
        assertEquals("admin", evaluate("${user.getUsername()}", variables));
    }

    @Test
    void allowsUserTOProperty() {
        UserTO userTO = new UserTO();
        userTO.setUsername("admin");

        assertEquals("admin", evaluate("${userTO.username}",
                new TestVariableContainer(Map.of("userTO", userTO))));
    }

    @Test
    void allowsUserCRAndUserURApproval() {
        TestVariableContainer variables = new TestVariableContainer(Map.of(
                "userCR", new UserCR(),
                "userUR", new UserUR()));

        assertEquals(false, evaluate("${userCR.requiresApproval()}", variables));
        assertEquals(false, evaluate("${userUR.requiresApproval()}", variables));
    }

    @Test
    void allowsFlowableUtilsMethod() {
        FlowableWorkflowUtils flowableUtils = new FlowableWorkflowUtils(mock(DomainProcessEngine.class));
        UserTO userTO = new UserTO();

        assertEquals(false, evaluate("${flowableUtils.isUserIngroup(userTO, 'group')}",
                new TestVariableContainer(Map.of("flowableUtils", flowableUtils, "userTO", userTO))));
    }

    @Test
    void allowsFlowableUtilsResolvedLazilyFromApplicationContext() {
        FlowableWorkflowUtils flowableUtils = new FlowableWorkflowUtils(mock(DomainProcessEngine.class));
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.containsBean("flowableUtils")).thenReturn(true);
        doReturn(FlowableWorkflowUtils.class).when(ctx).getType("flowableUtils");
        when(ctx.getBean("flowableUtils")).thenReturn(flowableUtils);

        SandboxedProcessExpressionManager manager =
                new SandboxedProcessExpressionManager(new AllowedBeansMap(ctx));
        UserTO userTO = new UserTO();

        assertEquals(false, manager.
                createExpression("${flowableUtils.isUserIngroup(userTO, 'groupForWorkflowOptIn')}").
                getValue(new TestVariableContainer(Map.of("userTO", userTO))));
    }
}
