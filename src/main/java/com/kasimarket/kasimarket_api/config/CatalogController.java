package com.kasimarket.kasimarket_api.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Every address in this class starts with /api/v1/catalog.
// Anyone with a valid token (client, worker or admin) may look at the task list.
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final TaskTypeRepository taskTypeRepository;

    // Spring gives us the repository automatically.
    public CatalogController(TaskTypeRepository taskTypeRepository) {
        this.taskTypeRepository = taskTypeRepository;
    }

    // GET /api/v1/catalog/task-types
    // Returns every task that can currently be booked.
    @GetMapping("/task-types")
    public List<TaskTypeView> listTaskTypes() {
        return taskTypeRepository.findByActiveTrue()
                .stream()
                // Turn each database row into a short view with only the safe fields.
                .map(t -> new TaskTypeView(t.getId(), t.getName(), t.getCategory(),
                        t.getBaseRatePerHour(), t.getMinHours()))
                .toList();
    }
}