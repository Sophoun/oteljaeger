package com.sophoun.oteljaeger.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that evaluates to {@code true} when the OTel Java agent is NOT active.
 * Used to skip manual instrumentation beans when the agent handles everything.
 */
public class AgentNotActiveCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return !"true".equals(System.getProperty(RuntimeAttachRunListener.AGENT_ACTIVE_PROPERTY));
	}
}
