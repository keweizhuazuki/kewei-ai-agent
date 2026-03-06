package com.kiwi.keweiaiagent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.Assert;

import java.util.Map;

public class ReReadingAdvisor implements BaseAdvisor {

	private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
            {re2_input_query}
            Read the question again: {re2_input_query}
			""";

	private final String re2AdviseTemplate;

	private int order = 0;

	public ReReadingAdvisor() {
		this(DEFAULT_RE2_ADVISE_TEMPLATE);
	}

	public ReReadingAdvisor(String re2AdviseTemplate) {
		Assert.hasText(re2AdviseTemplate, "re2AdviseTemplate must not be blank");
		this.re2AdviseTemplate = re2AdviseTemplate;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		String userText = chatClientRequest.prompt().getUserMessage() == null
			? ""
			: chatClientRequest.prompt().getUserMessage().getText();

		String augmentedUserText = PromptTemplate.builder()
			.template(this.re2AdviseTemplate)
			.variables(Map.of("re2_input_query", userText))
			.build()
			.render();

        ChatClientRequest ret = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .build();

        // 存放原始用户输入，供后续advisor使用,在 Advisor 链中共享状态
        ret.context().put("userText", userText);
        return ret;

    }

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		return chatClientResponse;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public ReReadingAdvisor withOrder(int order) {
		this.order = order;
		return this;
	}
}
