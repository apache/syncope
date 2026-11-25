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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * JUEL treats backslashes inside string literals as escape characters, so Windows paths must be
     * normalised before being embedded in an expression.
     */
    private static String juelPath(final Path path) {
        return path.toString().replace('\\', '/');
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
                + "['getMethod']('getRuntime')['invoke'](null)['exec']('touch " + juelPath(target) + "') != null}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksDotNotationPayload() {
        Path target = tempDir.resolve("dot-notation");
        String expression = "${''.getClass().forName('java.lang.Runtime')"
                + ".getMethod('getRuntime').invoke(null).exec('touch " + juelPath(target) + "') != null}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksHashNotationPayload() {
        Path target = tempDir.resolve("hash-notation");
        String expression = "#{\"\".getClass().forName(\"java.lang.Runtime\")"
                + ".getMethod(\"getRuntime\").invoke(null).exec(\"touch " + juelPath(target) + "\").toString()}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksPayloadWithClosingBraceInStringLiteral() {
        Path target = tempDir.resolve("closing-brace");
        String expression = "${'}'.concat(\"\".getClass().forName(\"java.lang.Runtime\")"
                + ".getMethod(\"getRuntime\").invoke(null).exec(\"touch " + juelPath(target) + "\").toString())}";

        FlowableException ex = assertThrows(FlowableException.class,
                () -> evaluate(expression, VariableContainer.empty()));
        assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void blocksDoubleQuotedPayloadWithMultilineArgument() {
        Path target = tempDir.resolve("multiline");
        String expression = "${\"\".getClass().forName(\"java.lang.Runtime\")"
                + ".getMethod(\"getRuntime\").invoke(null).exec(\"touch\n" + juelPath(target) + "\") != null}";

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
        TestVariableContainer variables = new TestVariableContainer(Map.of("obj", new Object()));

        assertThrows(FlowableException.class, () -> evaluate("${obj.toString()}", variables));
    }

    @Test
    void allowsStringAndCollectionMethods() {
        UserTO userTO = new UserTO();
        userTO.setUsername("admin");

        TestVariableContainer variables = new TestVariableContainer(Map.of(
                "userTO", userTO,
                "list", new ArrayList<>(List.of("a", "b")),
                "map", new HashMap<>(Map.of("k", "v"))));

        assertEquals(5, ((Number) evaluate("${userTO.username.length()}", variables)).intValue());
        assertEquals("ADMIN", evaluate("${userTO.username.toUpperCase()}", variables));
        assertEquals(0, ((Number) evaluate("${userTO.plainAttrs.size()}", variables)).intValue());
        assertEquals(2, ((Number) evaluate("${list.size()}", variables)).intValue());
        assertEquals("a", evaluate("${list.get(0)}", variables));
        assertEquals(1, ((Number) evaluate("${map.size()}", variables)).intValue());
        assertEquals("v", evaluate("${map.get('k')}", variables));
        assertEquals(Boolean.TRUE, evaluate("${map.containsKey('k')}", variables));
    }

    @Test
    void blocksReflectionOnStringAndCollectionTypes() {
        TestVariableContainer variables = new TestVariableContainer(Map.of(
                "list", new ArrayList<>(List.of("a")),
                "map", new HashMap<>(Map.of("k", "v"))));

        for (String expression : List.of(
                "${'abc'.getClass()}",
                "${list.getClass()}",
                "${map.getClass()}",
                "${list.getClass().forName('java.lang.Runtime')}")) {

            FlowableException ex = assertThrows(FlowableException.class,
                    () -> evaluate(expression, variables), expression);
            assertTrue(ex.getMessage().contains("Unknown method used in expression"), ex.getMessage());
        }
    }

    @Test
    void allowsPropertyAndMapKeyAccess() {
        UserTO userTO = new UserTO();
        userTO.setUsername("admin");

        TestVariableContainer variables = new TestVariableContainer(Map.of(
                "userTO", userTO,
                "map", new HashMap<>(Map.of("k", "v"))));

        assertEquals("admin", evaluate("${userTO.username}", variables));
        assertTrue(evaluate("${userTO.plainAttrs}", variables) instanceof Set);
        assertEquals("v", evaluate("${map['k']}", variables));
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
