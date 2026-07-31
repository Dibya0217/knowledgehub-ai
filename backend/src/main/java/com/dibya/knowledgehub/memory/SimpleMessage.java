package com.dibya.knowledgehub.memory;

import com.dibya.knowledgehub.conversation.entity.MessageRole;

public record SimpleMessage(MessageRole role, String content) {}
