package com.example.campus_care_backend.repo;

import com.example.campus_care_backend.domain.RepairStatus;
import com.example.campus_care_backend.entity.RepairRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<RepairRequest> findByAssignedTechnicianIdOrderByCreatedAtDesc(Long assignedTechnicianId);
    List<RepairRequest> findByStatusOrderByCreatedAtDesc(RepairStatus status);

    List<RepairRequest> findByAssignedTechnicianIdAndStatusOrderByCreatedAtDesc(
            Long assignedTechnicianId, RepairStatus status);
}