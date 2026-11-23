package org.apache.syncope.core.flowable.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.user.User;

/**
 * Simple sandbox for validating JUEL expressions used in BPMN conditionExpressions.
 * The validator allows only a whitelisted set of beans and methods that are known to
 * be safe in current Syncope deployments.
 */
public final class UELSandbox {

    /**
     * Matches any method invocation, i.e. an identifier immediately followed by an opening parenthesis.
     */
    private static final Pattern METHOD_CALL = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*\\(");

    /**
     * Matches the reference on which a method is invoked, capturing the bean name (the identifier
     * immediately preceding the last dot). DOTALL is required because BPMN expressions may span
     * multiple lines.
     */
    private static final Pattern BEAN_REF =
            Pattern.compile(".*\\b([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*\\.\\s*$", Pattern.DOTALL);

    private static final Map<String, Set<String>> WHITELIST = new HashMap<>();

    static {
        Set<String> flowableUtils = Stream.of(FlowableWorkflowUtils.class.getMethods()).
                map(Method::getName).
                collect(Collectors.toSet());
        WHITELIST.put("flowableUtils", flowableUtils);

        Set<String> user = Stream.of(User.class.getMethods()).
                map(Method::getName).
                collect(Collectors.toSet());
        WHITELIST.put("user", user);

        Set<String> userCR = Stream.of(UserCR.class.getMethods()).
                map(Method::getName).
                collect(Collectors.toSet());
        WHITELIST.put("userCR", userCR);

        Set<String> userUR = Stream.of(UserUR.class.getMethods()).
                map(Method::getName).
                collect(Collectors.toSet());
        WHITELIST.put("userUR", userUR);

        Set<String> userTO = Stream.of(UserTO.class.getMethods()).
                map(Method::getName).
                collect(Collectors.toSet());
        WHITELIST.put("userTO", userTO);
    }

    /**
     * Determines whether the supplied expression string is allowed by the sandbox.
     * Only a very small set of bean method calls are permitted; anything else results in a rejection.
     *
     * @param expr UEL expression
     * @return whether the supplied expression string is allowed by the sandbox
     */
    public static boolean isAllowed(final String expr) {
        if (StringUtils.isBlank(expr)) {
            return true;
        }

        // Strip leading ${...}
        String body = expr.startsWith("${") && expr.endsWith("}")
                ? expr.substring(2, expr.length() - 1).trim()
                : expr.trim();

        Matcher m = METHOD_CALL.matcher(body);
        while (m.find()) {
            String method = m.group(1);

            // The method must be invoked on one of the whitelisted beans.
            Matcher beanMatcher = BEAN_REF.matcher(body.substring(0, m.start(1)));
            if (!beanMatcher.matches()) {
                return false;
            }
            String bean = beanMatcher.group(1);

            Set<String> methods = WHITELIST.get(bean);
            if (methods == null || !methods.contains(method)) {
                return false;
            }
        }

        return true;
    }

    private UELSandbox() {
        // private constructor for static utility class
    }
}
