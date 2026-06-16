package com.sophoun.oteljaeger.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that evaluates to true when the OTel agent is NOT active.
 * Used to enable manual instrumentation only when the agent is not available.
 */
class WhenAgentInactiveCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String agentActive = context.getEnvironment().getProperty("oteljaeger.agent.active", "false");
        return !"true".equals(agentActive);
    }
}
