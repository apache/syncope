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
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
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
 * Makes sure that executable code declared by sandboxed Groovy classes stays sandboxed even when invoked from outside
 * the proxied implementation call, for example when Syncope later uses an object returned by a command.
 */
public final class GroovySandboxMethods {

    private static final String TOKEN = "$syncopeSandboxInterceptor";

    private GroovySandboxMethods() {
    }

    public static CompilerConfiguration newCompilerConfiguration() {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(
                new RejectASTTransformsCustomizer(),
                new SandboxTransformer(),
                new ClosureInterceptorCustomizer(),
                new MethodInterceptorCustomizer());
        cc.setDisabledGlobalASTTransformations(Set.of(GrabAnnotationTransformation.class.getName()));
        return cc;
    }

    /**
     * Registers an interceptor for the current thread if there is none yet.
     *
     * @param scriptClass class declaring the executable code being run
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

    private static Statement wrap(final ClassNode classNode, final Statement body) {
        ClassNode owner = ClassHelper.make(GroovySandboxMethods.class);

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

        return wrapped;
    }

    private static boolean isExplicitConstructorCall(final Statement statement) {
        return statement instanceof ExpressionStatement expression
                && expression.getExpression() instanceof ConstructorCallExpression;
    }

    private static void wrapConstructors(final ClassNode classNode) {
        for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
            Statement body = constructor.getCode();
            if (body == null) {
                continue;
            }

            List<Statement> statements = body instanceof BlockStatement block
                    ? block.getStatements()
                    : List.of(body);
            if (statements.isEmpty()) {
                continue;
            }

            int first = isExplicitConstructorCall(statements.get(0)) ? 1 : 0;
            BlockStatement wrapped = new BlockStatement();
            for (int i = 0; i < first; i++) {
                wrapped.addStatement(statements.get(i));
            }

            BlockStatement rest = new BlockStatement();
            for (int i = first; i < statements.size(); i++) {
                rest.addStatement(statements.get(i));
            }
            if (!rest.getStatements().isEmpty()) {
                wrapped.addStatement(wrap(classNode, rest));
            }

            constructor.setCode(wrapped);
        }
    }

    /**
     * Closure bodies are compiled into separate classes after the other customizers have run.
     */
    private static final class ClosureInterceptorCustomizer extends CompilationCustomizer {

        ClosureInterceptorCustomizer() {
            super(CompilePhase.CANONICALIZATION);
        }

        @Override
        public void call(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) {
            if (classNode == null) {
                return;
            }

            ClassCodeExpressionTransformer visitor = new ClassCodeExpressionTransformer() {

                @Override
                protected SourceUnit getSourceUnit() {
                    return source;
                }

                @Override
                public Expression transform(final Expression expression) {
                    Expression transformed = super.transform(expression);
                    if (transformed instanceof ClosureExpression closure) {
                        Statement body = closure.getCode();
                        if (body != null) {
                            body.visit(this);
                            closure.setCode(wrap(classNode, body));
                        }
                    }
                    return transformed;
                }
            };

            for (MethodNode method : classNode.getMethods()) {
                visitor.visitMethod(method);
            }
            for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
                visitor.visitMethod(constructor);
            }
            for (Statement statement : classNode.getObjectInitializerStatements()) {
                statement.visit(visitor);
            }
            for (FieldNode field : classNode.getFields()) {
                visitor.visitField(field);
            }
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

            for (MethodNode method : classNode.getMethods()) {
                Statement body = method.getCode();
                if (body == null || method.isAbstract()
                        || (method.getModifiers() & Modifier.NATIVE) != 0
                        || (method.getName().startsWith("<") && !"<clinit>".equals(method.getName()))) {

                    continue;
                }

                method.setCode(wrap(classNode, body));
            }

            wrapConstructors(classNode);
        }
    }
}
