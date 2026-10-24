
package com.kasimarket.kasimarket_api.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {

    // Finds only the tasks that clients can currently book.
    // Spring writes the SQL itself from the method name.
    List<TaskType> findByActiveTrue();
}