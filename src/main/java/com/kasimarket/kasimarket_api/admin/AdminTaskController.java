package com.kasimarket.kasimarket_api.admin;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.catalog.TaskTypeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

// Lets staff control the pilot scope: switch tasks on and off, and change their price.
// Changing a price never changes jobs that are already booked, because each job keeps its own price.
// Everything under /api/v1/admin is already limited to the ADMIN role in SecurityConfig.
@RestController
@RequestMapping("/api/v1/admin/task-types")
public class AdminTaskController {

    private final TaskTypeRepository taskTypeRepository;

    public AdminTaskController(TaskTypeRepository taskTypeRepository) {
        this.taskTypeRepository = taskTypeRepository;
    }

    // GET /api/v1/admin/task-types  (every task, including switched-off ones)
    @GetMapping
    public List<AdminTaskView> list() {
        return taskTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(TaskType::getId))
                .map(this::toView)
                .toList();
    }

    // PUT /api/v1/admin/task-types/8  with body {"active":false}
    @PutMapping("/{id}")
    public AdminTaskView update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        TaskType task = taskTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found."));

        if (request.active() == null && request.baseRatePerHour() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to change.");
        }
        if (request.active() != null) {
            task.setActive(request.active());
        }
        if (request.baseRatePerHour() != null) {
            task.setBaseRatePerHour(request.baseRatePerHour());
        }
        taskTypeRepository.save(task);
        return toView(task);
    }

    private AdminTaskView toView(TaskType t) {
        return new AdminTaskView(t.getId(), t.getName(), t.getCategory(), t.getBaseRatePerHour(),
                t.getMinHours(), t.getMinTier(), t.isActive());
    }
}
