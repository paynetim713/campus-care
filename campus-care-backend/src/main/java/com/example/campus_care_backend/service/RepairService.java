package com.example.campus_care_backend.service;

import com.example.campus_care_backend.domain.RepairStatus;
import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.RepairRequest;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.exception.ForbiddenOperationException;
import com.example.campus_care_backend.repo.RepairRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepairService {
    private final RepairRequestRepository repo;
    private final NotificationService notificationService;

    public RepairRequest create(User requester, String building, String floor,
                                String room, String category, String details,
                                String photoUrl) {
        if (requester.getRole() != UserRole.REQUESTER)
            throw new IllegalArgumentException("role not allowed");

        RepairRequest r = new RepairRequest();
        r.setRequesterId(requester.getId());
        r.setBuilding(building);
        r.setFloor(floor);
        r.setRoom(room);
        r.setCategory(category);
        r.setDetails(details);
        r.setPhotoUrl(photoUrl);
        r.setStatus(RepairStatus.NEW);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());

        RepairRequest saved = repo.save(r);
        notificationService.push(requester.getId(),
                "Request Received",
                "Your request #" + saved.getId() + " has been received and is awaiting assignment.");
        return saved;
    }

    public List<RepairRequest> listForRequester(User requester) {
        if (requester.getRole() != UserRole.REQUESTER)
            throw new IllegalArgumentException("role not allowed");
        return repo.findByRequesterIdOrderByCreatedAtDesc(requester.getId());
    }

    public List<RepairRequest> listAvailableForTechnician(User tech) {
        if (tech.getRole() != UserRole.TECHNICIAN)
            throw new IllegalArgumentException("role not allowed");
        return repo.findByAssignedTechnicianIdAndStatusOrderByCreatedAtDesc(
                tech.getId(), RepairStatus.NEW);
    }

    public List<RepairRequest> listForTechnician(User tech) {
        if (tech.getRole() != UserRole.TECHNICIAN)
            throw new IllegalArgumentException("role not allowed");
        return repo.findByAssignedTechnicianIdOrderByCreatedAtDesc(tech.getId());
    }

    public RepairRequest getById(Long repairId) {
        return repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));
    }

    public RepairRequest getByIdForUser(User viewer, Long repairId) {
        RepairRequest repair = getById(repairId);
        ensureCanAccess(viewer, repair);
        return repair;
    }

    public void ensureCanAccess(User viewer, RepairRequest repair) {
        if (viewer == null) {
            throw new ForbiddenOperationException("login required");
        }
        if (viewer.getRole() == UserRole.ADMIN) {
            return;
        }
        if (viewer.getRole() == UserRole.REQUESTER && repair.getRequesterId().equals(viewer.getId())) {
            return;
        }
        if (viewer.getRole() == UserRole.TECHNICIAN
                && repair.getAssignedTechnicianId() != null
                && repair.getAssignedTechnicianId().equals(viewer.getId())) {
            return;
        }
        throw new ForbiddenOperationException("you do not have access to this repair request");
    }

    public RepairRequest assign(User admin, Long repairId, Long technicianId) {
        if (admin.getRole() != UserRole.ADMIN)
            throw new IllegalArgumentException("role not allowed");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));

        if (r.getStatus() != RepairStatus.NEW)
            throw new IllegalStateException("can only assign jobs that are still pending (NEW)");

        r.setAssignedTechnicianId(technicianId);

        r.setUpdatedAt(Instant.now());
        RepairRequest saved = repo.save(r);

        notificationService.push(r.getRequesterId(),
                "Technician Assigned",
                "A technician has been assigned to your request #" + r.getId()
                        + ". They will accept and begin work shortly.");

        notificationService.push(technicianId,
                "New Job Assigned",
                "Admin has assigned repair request #" + r.getId()
                        + " to you. Open the Job Board to review and accept.");
        return saved;
    }

    public RepairRequest acceptByTechnician(User tech, Long repairId) {
        if (tech.getRole() != UserRole.TECHNICIAN)
            throw new IllegalArgumentException("role not allowed");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));

        if (r.getStatus() != RepairStatus.NEW)
            throw new IllegalStateException("repair is not in NEW status");

        if (r.getAssignedTechnicianId() != null
                && !r.getAssignedTechnicianId().equals(tech.getId())) {
            throw new IllegalStateException("repair is assigned to a different technician");
        }

        r.setAssignedTechnicianId(tech.getId());
        r.setStatus(RepairStatus.IN_PROGRESS);
        r.setUpdatedAt(Instant.now());
        RepairRequest saved = repo.save(r);

        notificationService.push(r.getRequesterId(),
                "Technician On the Way",
                "A technician has accepted your request #" + r.getId()
                        + " and will begin work shortly.");
        return saved;
    }

    public RepairRequest updateByTechnician(User tech, Long repairId,
                                            RepairStatus status,
                                            String eta, String note) {
        if (tech.getRole() != UserRole.TECHNICIAN)
            throw new IllegalArgumentException("role not allowed");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));
        if (r.getAssignedTechnicianId() == null
                || !r.getAssignedTechnicianId().equals(tech.getId())) {
            throw new IllegalArgumentException("not assigned to you");
        }

        if (status != null) r.setStatus(status);
        if (eta  != null)   r.setEta(eta);
        if (note != null)   r.setTechnicianNote(note);
        r.setUpdatedAt(Instant.now());
        RepairRequest saved = repo.save(r);

        if (status == RepairStatus.IN_PROGRESS) {
            notificationService.push(r.getRequesterId(),
                    "Technician on the Way",
                    "Your technician is heading to your location for request #" + r.getId() + ".");
        } else if (status == RepairStatus.DONE) {
            notificationService.push(r.getRequesterId(),
                    "Job Completed",
                    "Your repair request #" + r.getId()
                            + " has been completed. Please rate your experience.");
        }
        return saved;
    }

    public RepairRequest updatePhotoUrl(User requester, Long repairId, String photoUrl) {
        if (requester.getRole() != UserRole.REQUESTER)
            throw new IllegalArgumentException("role not allowed");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));
        if (!r.getRequesterId().equals(requester.getId()))
            throw new IllegalArgumentException("not your request");
        if (photoUrl == null || photoUrl.isBlank())
            throw new IllegalArgumentException("photoUrl is required");

        r.setPhotoUrl(photoUrl);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    public void deleteByRequester(User requester, Long repairId) {
        if (requester.getRole() != UserRole.REQUESTER)
            throw new IllegalArgumentException("role not allowed");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));
        if (!r.getRequesterId().equals(requester.getId()))
            throw new IllegalArgumentException("not your request");
        if (r.getStatus() != RepairStatus.NEW)
            throw new IllegalStateException("cannot delete a request that has already been accepted");

        repo.delete(r);
    }

    public RepairRequest rate(User requester, Long repairId, int stars, String comment) {
        if (requester.getRole() != UserRole.REQUESTER)
            throw new IllegalArgumentException("role not allowed");
        if (stars < 1 || stars > 5)
            throw new IllegalArgumentException("rating must be 1..5");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));
        if (!r.getRequesterId().equals(requester.getId()))
            throw new IllegalArgumentException("not your request");
        if (r.getStatus() != RepairStatus.DONE)
            throw new IllegalArgumentException("can only rate completed requests");

        r.setRating(stars);
        r.setRatingComment(comment);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    public RepairRequest saveAdminReply(User admin, Long repairId, String reply) {
        if (admin.getRole() != UserRole.ADMIN)
            throw new IllegalArgumentException("admin role required");

        RepairRequest r = repo.findById(repairId)
                .orElseThrow(() -> new IllegalArgumentException("repair not found"));
        if (r.getRating() == null || r.getRating() < 1)
            throw new IllegalArgumentException("cannot reply before review is submitted");

        String normalized = reply == null ? "" : reply.trim();
        r.setAdminReply(normalized.isEmpty() ? null : normalized);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }
}
