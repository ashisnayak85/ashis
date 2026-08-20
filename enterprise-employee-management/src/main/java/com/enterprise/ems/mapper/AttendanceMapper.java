package com.enterprise.ems.mapper;

import com.enterprise.ems.dto.*;
import com.enterprise.ems.entity.*;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceDTO toDTO(Attendance entity) {
        if (entity == null) return null;
        return AttendanceDTO.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .employeeName(entity.getEmployee().getFullName())
                .attendanceDate(entity.getAttendanceDate())
                .checkInTime(entity.getCheckInTime())
                .checkOutTime(entity.getCheckOutTime())
                .status(entity.getStatus())
                .source(entity.getSource())
                .remarks(entity.getRemarks())
                .faceVerified(entity.isFaceVerified())
                .build();
    }

    // Note: deliberately does NOT copy dto.getSource() or dto.getFaceVerified()
    // onto the entity - the service layer sets both explicitly itself
    // (markAttendance -> ADMIN/false, markSelfAttendance -> SELF/result of a
    // real face match, recordBiometricPunch -> BIOMETRIC/false), so a client
    // can never spoof how a punch was recorded or claim a face match that
    // never happened by editing the request body.
    public Attendance toEntity(AttendanceDTO dto, Employee employee) {
        return Attendance.builder()
                .id(dto.getId())
                .employee(employee)
                .attendanceDate(dto.getAttendanceDate())
                .checkInTime(dto.getCheckInTime())
                .checkOutTime(dto.getCheckOutTime())
                .status(dto.getStatus())
                .remarks(dto.getRemarks())
                .build();
    }
}
