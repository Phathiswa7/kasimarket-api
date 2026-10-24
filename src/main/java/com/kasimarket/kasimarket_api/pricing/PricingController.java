package com.kasimarket.kasimarket_api.pricing;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.catalog.TaskTypeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Every address in this class starts with /api/v1/pricing.
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    private final TaskTypeRepository taskTypeRepository;
    private final PriceCalculator priceCalculator;

    // Spring gives us both tools automatically.
    public PricingController(TaskTypeRepository taskTypeRepository,
                             PriceCalculator priceCalculator) {
        this.taskTypeRepository = taskTypeRepository;
        this.priceCalculator = priceCalculator;
    }

    // POST /api/v1/pricing/quote
    // Works out the price of a task. It saves nothing: it only answers "what would it cost?".
    @PostMapping("/quote")
    public PriceQuote quote(@Valid @RequestBody QuoteRequest request) {

        // Find the task. If the ID does not exist, reply "404 Not Found".
        TaskType task = taskTypeRepository.findById(request.taskTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task type not found."));

        // A switched-off task cannot be booked or priced.
        if (!task.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This task is not available right now.");
        }

        // Let the calculator do the maths.
        return priceCalculator.calculate(task, request.hours(), request.urgent());
    }
}