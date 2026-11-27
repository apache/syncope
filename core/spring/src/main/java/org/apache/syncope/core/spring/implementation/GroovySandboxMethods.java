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
package org.apache.syncope.core.spring.implementation;

import groovy.grape.GrabAnnotationTransformation;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.RejectASTTransformsCustomizer;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxInterceptor;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

/**
 * Makes sure that methods declared by sandboxed Groovy classes stay sandboxed even when invoked from outside the
 * proxied implementation call, for example when Syncope later uses an object returned by a command.
 */
public final class GroovySandboxMethods {

    private static final String TOKEN = "$syncopeSandboxInterceptor";

    public static CompilerConfiguration newCompilerConfiguration() {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(
                new RejectASTTransformsCustomizer(),
                new SandboxTransformer(),
                new MethodInterceptorCustomizer());
        cc.setDisabledGlobalASTTransformations(Set.of(GrabAnnotationTransformation.class.getName()));
        return cc;
    }

    /**
     * Registers an interceptor for the current thread if there is none yet.
     *
     * @param scriptClass class declaring the method being executed
     * @return the interceptor to unregister, or {@code null} if one was already active
     */
    public static Object enter(final Class<?> scriptClass) {
        if (!GroovyInterceptor.getApplicableInterceptors().isEmpty()) {
            return null;
        }

        GroovyInterceptor interceptor = new SandboxInterceptor(new SelfAllowingWhitelist(
                ApplicationContextProvider.getApplicationContext().getBean(Whitelist.class),
                scriptClass.getClassLoader()));
        interceptor.register();
        return interceptor;
    }

    /**
     * Unregisters the interceptor installed by {@link #enter(Class)}.
     *
     * @param token value returned by {@link #enter(Class)}
     */
    public static void exit(final Object token) {
        if (token instanceof GroovyInterceptor interceptor) {
            interceptor.unregister();
        }
    }

    private static final class MethodInterceptorCustomizer extends CompilationCustomizer {

        MethodInterceptorCustomizer() {
            super(CompilePhase.INSTRUCTION_SELECTION);
        }

        @Override
        public void call(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) {
            if (classNode == null) {
                return;
            }

            ClassNode owner = ClassHelper.make(GroovySandboxMethods.class);
            for (MethodNode method : classNode.getMethods()) {
                Statement body = method.getCode();
                if (body == null || method.isAbstract()
                        || (method.getModifiers() & Modifier.NATIVE) != 0
                        || method.getName().startsWith("<")) {

                    continue;
                }

                VariableExpression token = new VariableExpression(TOKEN, ClassHelper.OBJECT_TYPE);
                BlockStatement wrapped = new BlockStatement();
                wrapped.addStatement(new ExpressionStatement(new DeclarationExpression(
                        token,
                        Token.newSymbol(Types.ASSIGN, -1, -1),
                        new StaticMethodCallExpression(owner, "enter", new ClassExpression(classNode)))));

                BlockStatement cleanup = new BlockStatement();
                cleanup.addStatement(new ExpressionStatement(
                        new StaticMethodCallExpression(owner, "exit", token)));
                wrapped.addStatement(new TryCatchStatement(body, cleanup));

                method.setCode(wrapped);
            }
        }
    }

    private GroovySandboxMethods() {
        // private constructor for static utility class
    }
}
