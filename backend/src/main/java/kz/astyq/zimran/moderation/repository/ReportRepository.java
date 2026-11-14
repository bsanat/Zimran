package kz.astyq.zimran.moderation.repository;

import kz.astyq.zimran.moderation.model.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
