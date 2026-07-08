package com.hhu.elderly.institution.repository;

import com.hhu.elderly.institution.dto.InstitutionApplicationRequest;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class InstitutionApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public InstitutionApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createApplication(
            Long userId,
            InstitutionApplicationRequest request
    ) {
        String sql = """
                INSERT INTO institution_applications (
                    user_id,
                    institution_name,
                    contact_person,
                    contact_phone,
                    district,
                    address,
                    lon,
                    lat,
                    license_url,
                    record_certificate_url,
                    other_material_url,
                    description,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                userId,
                request.institutionName(),
                request.contactPerson(),
                request.contactPhone(),
                request.district(),
                request.address(),
                request.lon(),
                request.lat(),
                request.licenseUrl(),
                request.recordCertificateUrl(),
                request.otherMaterialUrl(),
                request.description()
        );
    }

    public List<InstitutionApplicationResponse> findMine(Long userId) {
        String sql = """
                SELECT *
                FROM institution_applications
                WHERE user_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs),
                userId
        );
    }

    public List<InstitutionApplicationResponse> findByStatus(String status) {
        String sql = """
                SELECT *
                FROM institution_applications
                WHERE status = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs),
                status
        );
    }

    public void updateStatus(
            Long id,
            String newStatus,
            String reason,
            Long operatorId,
            String operatorName
    ) {
        String oldStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM institution_applications WHERE id = ?",
                String.class,
                id
        );

        jdbcTemplate.update(
                """
                UPDATE institution_applications
                SET status = ?,
                    reject_reason = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                newStatus,
                reason,
                operatorId,
                id
        );

        jdbcTemplate.update(
                """
                INSERT INTO audit_logs (
                    target_type,
                    target_id,
                    action,
                    old_status,
                    new_status,
                    operator_id,
                    operator_name,
                    reason
                )
                VALUES ('institution_application', ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "approved".equals(newStatus) ? "approve" : "reject",
                oldStatus,
                newStatus,
                operatorId,
                operatorName,
                reason
        );
    }

    private InstitutionApplicationResponse mapRow(java.sql.ResultSet rs)
            throws java.sql.SQLException {
        return new InstitutionApplicationResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("institution_name"),
                rs.getString("contact_person"),
                rs.getString("contact_phone"),
                rs.getString("district"),
                rs.getString("address"),
                getDouble(rs.getObject("lon")),
                getDouble(rs.getObject("lat")),
                rs.getString("license_url"),
                rs.getString("record_certificate_url"),
                rs.getString("other_material_url"),
                rs.getString("description"),
                rs.getString("status"),
                getStatusText(rs.getString("status")),
                rs.getString("reject_reason"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("reviewed_at"))
        );
    }

    private String getStatusText(String status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case "pending" -> "待审核";
            case "reviewing" -> "审核中";
            case "approved" -> "已通过";
            case "rejected" -> "已驳回";
            default -> "未知";
        };
    }

    private Double getDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}