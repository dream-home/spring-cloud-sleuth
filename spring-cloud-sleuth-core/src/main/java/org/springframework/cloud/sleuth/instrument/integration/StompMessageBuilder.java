/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.integration;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.trace.SpanContextHolder;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.StringUtils;

/**
 * Builder class to create STOMP message
 *
 * @author Gaurav Rai Mazra
 *
 */
public class StompMessageBuilder {

	public static StompMessageBuilder fromMessage(Message<?> message) {
		return new StompMessageBuilder(message);
	}

	private Map<String, Object> headers = new TreeMap<String, Object>();
	private Message<?> message;

	public StompMessageBuilder(final Message<?> message) {
		this.message = message;
		this.headers.putAll(message.getHeaders());
	}

	public StompMessageBuilder setHeader(String key, Object value) {
		this.headers.put(key, value);
		return this;
	}

	public StompMessageBuilder setHeaderIfAbsent(String key, Object value) {
		if (this.headers.get(key) == null)
			this.headers.put(key, value);

		return this;
	}

	public StompMessageBuilder setHeadersFromSpan(final Span span) {
		if (span != null) {
			setHeaderIfAbsent(Span.SPAN_ID_NAME, Span.toHex(span.getSpanId()));
			setHeaderIfAbsent(Span.TRACE_ID_NAME, Span.toHex(span.getTraceId()));
			setHeaderIfAbsent(Span.SPAN_NAME_NAME, span.getName());
			Long parentId = getParentId(SpanContextHolder.getCurrentSpan());
			if (parentId != null)
				setHeaderIfAbsent(Span.PARENT_ID_NAME, Span.toHex(parentId));

			String processId = span.getProcessId();
			if (StringUtils.hasText(processId))
				setHeaderIfAbsent(Span.PROCESS_ID_NAME, processId);
		}
		return this;
	}

	public Message<?> build() {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		for (Map.Entry<String, Object> entry : this.headers.entrySet()) {
			String key = entry.getKey();
			if (key != null) {
				Object value = entry.getValue();
				pushHeaders(headerAccessor, key, value);
			}
		}
		return org.springframework.messaging.support.MessageBuilder.createMessage(this.message.getPayload(),
				headerAccessor.getMessageHeaders());
	}

	private void pushHeaders(final SimpMessageHeaderAccessor accessor, final String key, final Object value) {
		switch (key) {
		case SimpMessageHeaderAccessor.DESTINATION_HEADER:
		case SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER:
		case SimpMessageHeaderAccessor.SESSION_ID_HEADER:
		case SimpMessageHeaderAccessor.SESSION_ATTRIBUTES:
		case SimpMessageHeaderAccessor.SUBSCRIPTION_ID_HEADER:
		case SimpMessageHeaderAccessor.USER_HEADER:
		case SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER:
		case SimpMessageHeaderAccessor.HEART_BEAT_HEADER:
		case SimpMessageHeaderAccessor.ORIGINAL_DESTINATION:
		case SimpMessageHeaderAccessor.IGNORE_ERROR:
		case Span.NOT_SAMPLED_NAME:
		case Span.PARENT_ID_NAME:
		case Span.PROCESS_ID_NAME:
		case Span.SPAN_ID_NAME:
		case Span.SPAN_NAME_NAME:
		case Span.TRACE_ID_NAME:
			accessor.setHeader(key, value);
			break;
		default:
			accessor.setNativeHeader(key, value == null ? null : value.toString());
		}
	}

	private Long getParentId(final Span currentSpan) {
		List<Long> parents = currentSpan.getParents();
		return parents.isEmpty() ? null : parents.get(0);
	}
}
