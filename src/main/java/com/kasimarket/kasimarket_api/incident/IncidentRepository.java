package com.kasimarket.kasimarket_api.incident;

import com.kasimarket.kasimarket_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // All incidents with a given status (for example OPEN).
    List<Incident> findByStatus(IncidentStatus status);

    // The incidents one person reported, newest first.
    List<Incident> findByReporterOrderByCreatedAtDesc(User reporter);

    // How many incidents have a given status (for the staff dashboard).
    long countByStatus(IncidentStatus status);

    // How many incidents came from the SOS button and have a given status.
    // "SosTrue" means: the "sos" field is true. Spring writes the SQL from the method name.
    long countBySosTrueAndStatus(IncidentStatus status);
}
