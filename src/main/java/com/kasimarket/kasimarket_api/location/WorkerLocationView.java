package com.kasimarket.kasimarket_api.location;

import java.time.LocalDateTime;

// The answer after going online or offline.
public record WorkerLocationView(
        boolean online,
        LocalDateTime updatedAt
) {
}