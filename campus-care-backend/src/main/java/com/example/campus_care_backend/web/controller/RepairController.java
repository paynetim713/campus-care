package com.example.campus_care_backend.web.controller;

import com.example.campus_care_backend.domain.UserRole;
import com.example.campus_care_backend.entity.RepairRequest;
import com.example.campus_care_backend.entity.User;
import com.example.campus_care_backend.repo.RepairRequestRepository;
import com.example.campus_care_backend.service.AuthService;
import com.example.campus_care_backend.service.RepairService;
import com.example.campus_care_backend.web.dto.RepairDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
public class RepairController {
    private final AuthService           authService;
    private final RepairService         repairService;
    private final RepairRequestRepository repairRepo;

    @GetMapping
    public List<RepairDtos.RepairResponse> listAll(
            @RequestHeader("X-Auth-Token") String token) {
        User u = authService.requireUserByToken(token);
        if (u.getRole() != UserRole.ADMIN)
            throw new IllegalArgumentException("admin role required");
        return repairRepo.findAll().stream()
                .map(this::toDto)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    @PostMapping
    public RepairDtos.RepairResponse create(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody RepairDtos.CreateRepairRequest req
    ) {
        User u = authService.requireUserByToken(token);
        RepairRequest r = repairService.create(u, req.building(), req.floor(),
                req.room(), req.category(), req.details(), req.photoUrl());
        return toDto(r);
    }

    @GetMapping("/mine")
    public List<RepairDtos.RepairResponse> mine(
            @RequestHeader("X-Auth-Token") String token) {
        User u = authService.requireUserByToken(token);
        return repairService.listForRequester(u).stream().map(this::toDto).toList();
    }

    @GetMapping("/available")
    public List<RepairDtos.RepairResponse> available(
            @RequestHeader("X-Auth-Token") String token) {
        User u = authService.requireUserByToken(token);
        return repairService.listAvailableForTechnician(u).stream()
                .map(this::toDto).toList();
    }

    @GetMapping("/assigned")
    public List<RepairDtos.RepairResponse> assigned(
            @RequestHeader("X-Auth-Token") String token) {
        User u = authService.requireUserByToken(token);
        return repairService.listForTechnician(u).stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public RepairDtos.RepairResponse getById(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id
    ) {
        User u = authService.requireUserByToken(token);
        return toDto(repairService.getByIdForUser(u, id));
    }

    @PatchMapping("/{id}/assign")
    public RepairDtos.RepairResponse assignTechnician(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id,
            @RequestBody Map<String, Long> body
    ) {
        User u = authService.requireUserByToken(token);
        Long technicianId = body.get("technicianId");
        if (technicianId == null)
            throw new IllegalArgumentException("technicianId is required");
        return toDto(repairService.assign(u, id, technicianId));
    }

    @PatchMapping("/{id}/accept")
    public RepairDtos.RepairResponse accept(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id
    ) {
        User u = authService.requireUserByToken(token);
        return toDto(repairService.acceptByTechnician(u, id));
    }

    @PatchMapping("/{id}/task")
    public RepairDtos.RepairResponse updateTask(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id,
            @RequestBody RepairDtos.UpdateTaskRequest req
    ) {
        User u = authService.requireUserByToken(token);
        RepairRequest r = repairService.updateByTechnician(
                u, id, req.status(), req.eta(), req.technicianNote());
        return toDto(r);
    }

    @PatchMapping("/{id}/photo")
    public RepairDtos.RepairResponse updatePhoto(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body
    ) {
        User u = authService.requireUserByToken(token);
        return toDto(repairService.updatePhotoUrl(u, id, body.get("photoUrl")));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id
    ) {
        User u = authService.requireUserByToken(token);
        repairService.deleteByRequester(u, id);
    }

    @PostMapping("/{id}/rate")
    public RepairDtos.RepairResponse rate(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id,
            @Valid @RequestBody RepairDtos.RateRequest req
    ) {
        User u = authService.requireUserByToken(token);
        return toDto(repairService.rate(u, id, req.stars(), req.comment()));
    }

    @PatchMapping("/{id}/admin-reply")
    public RepairDtos.RepairResponse adminReply(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Long id,
            @RequestBody RepairDtos.AdminReplyRequest req
    ) {
        User u = authService.requireUserByToken(token);
        return toDto(repairService.saveAdminReply(u, id, req.reply()));
    }

    private RepairDtos.RepairResponse toDto(RepairRequest r) {
        DateTimeFormatter f = DateTimeFormatter.ISO_INSTANT;
        return new RepairDtos.RepairResponse(
                r.getId(),
                r.getRequesterId(),
                r.getAssignedTechnicianId(),
                r.getBuilding(),
                r.getFloor(),
                r.getRoom(),
                r.getCategory(),
                r.getDetails(),
                r.getPhotoUrl(),
                r.getStatus(),
                r.getEta(),
                r.getTechnicianNote(),
                r.getRating(),
                r.getRatingComment(),
                r.getAdminReply(),
                f.format(r.getCreatedAt()),
                f.format(r.getUpdatedAt())
        );
    }
}
