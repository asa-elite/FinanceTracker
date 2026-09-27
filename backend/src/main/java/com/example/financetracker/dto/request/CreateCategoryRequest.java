package com.example.financetracker.dto.request;

public record CreateCategoryRequest(
        String name,
        CreateCategoryRequest parent
) {
}
