package com.sophoun.oteljaeger.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that evaluates to true when the OTel agent IS active.
 * Used to enable body-only capture filters when the agent already creates spans.
 */
class WhenAgentActiveCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String agentActive = context.getEnvironment().getProperty("oteljaeger.agent.active", "false");
        return "true".equals(agentActive);
    }
}
