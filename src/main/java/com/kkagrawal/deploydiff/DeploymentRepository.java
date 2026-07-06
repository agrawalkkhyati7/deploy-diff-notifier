package com.kkagrawal.deploydiff;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    // Find the most recent deployment for a given service + environment.
    // Spring Data generates the query from this method name.
    Optional<Deployment> findFirstByServiceNameAndEnvironmentOrderByDeployedAtDesc(
            String serviceName, String environment);
    
    @Query("""
            SELECT d FROM Deployment d
            WHERE (:service IS NULL OR LOWER(d.serviceName) LIKE LOWER(CONCAT('%', :service, '%')))
              AND (:env IS NULL OR LOWER(d.environment) = LOWER(:env))
            ORDER BY d.deployedAt DESC
            """)
    List<Deployment> search(
            @Param("service") String service,
            @Param("env") String env);

}
