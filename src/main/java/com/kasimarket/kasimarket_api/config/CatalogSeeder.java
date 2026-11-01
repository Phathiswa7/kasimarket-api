package com.kasimarket.kasimarket_api.config;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import com.kasimarket.kasimarket_api.catalog.TaskTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Puts the starting list of tasks into an EMPTY database.
// Unlike the fake test people, the task list is real data that every installation needs,
// so this runs in development AND production. It does nothing if tasks already exist.
@Component
public class CatalogSeeder implements CommandLineRunner {

    private final TaskTypeRepository taskTypeRepository;

    public CatalogSeeder(TaskTypeRepository taskTypeRepository) {
        this.taskTypeRepository = taskTypeRepository;
    }

    @Override
    public void run(String... args) {
        if (taskTypeRepository.count() > 0) {
            return; // the catalogue is already there
        }
        // name, category, rand per hour, minimum hours, minimum tier
        // These prices are only starting guesses. We test them in the pilot.
        addTask("Dog walking", "PET_CARE", 80, 1, 1);
        addTask("Errands", "ERRANDS", 90, 1, 1);
        addTask("Laundry", "LAUNDRY", 50, 3, 1);
        addTask("Standard house clean", "CLEANING", 80, 4, 1);
        addTask("Deep clean", "CLEANING", 87, 6, 2);
        addTask("Moving help", "LABOUR", 90, 2, 2);
        addTask("Babysitting", "CHILD_CARE", 60, 3, 3);
        addTask("House watching", "HOME_CARE", 120, 1, 3);
        System.out.println("Task catalogue created: " + taskTypeRepository.count() + " tasks");
    }

    private void addTask(String name, String category, int rate, int minHours, int minTier) {
        TaskType task = new TaskType();
        task.setName(name);
        task.setCategory(category);
        task.setBaseRatePerHour(rate);
        task.setMinHours(minHours);
        task.setMinTier(minTier);
        taskTypeRepository.save(task);
    }
}