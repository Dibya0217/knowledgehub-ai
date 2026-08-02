package com.dibya.knowledgehub.admin.dto;

public record AdminStatsResponse(
        long totalUsers,
        long totalDocuments,
        long totalConversations,
        long totalMessages
) {}
