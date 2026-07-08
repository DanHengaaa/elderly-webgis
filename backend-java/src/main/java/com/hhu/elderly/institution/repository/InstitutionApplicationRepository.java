package com.hhu.elderly.institution.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhu.elderly.institution.dto.InstitutionApplicationRequest;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
import com.hhu.elderly.institution.dto.InstitutionConsoleInstitutionOption;
import com.hhu.elderly.institution.dto.InstitutionProfileUpdateRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class InstitutionApplicationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public InstitutionApplicationRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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
                    province,
                    city,
                    district,
                    address,
                    address_name,
                    lon,
                    lat,
                    point_source,
                    institution_category,
                    grade_level,
                    total_beds,
                    available_beds,
                    monthly_fee_base,
                    price_tier,
                    cover_image_url,
                    images,
                    has_panorama,
                    intro,
                    services_json,
                    facilities_json,
                    license_url,
                    record_certificate_url,
                    other_material_url,
                    description,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, 'pending')
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                userId,
                request.institutionName(),
                request.contactPerson(),
                request.contactPhone(),
                request.province(),
                request.city(),
                request.district(),
                request.address(),
                request.addressName(),
                request.lon(),
                request.lat(),
                request.pointSource(),
                request.institutionCategory(),
                request.gradeLevel(),
                request.totalBeds(),
                request.availableBeds(),
                request.monthlyFeeBase(),
                request.priceTier(),
                request.coverImageUrl(),
                normalizeJsonArrayText(request.images()),
                request.hasPanorama() == null ? false : request.hasPanorama(),
                request.intro(),
                toJson(request.services()),
                toJson(request.facilities()),
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
                ORDER BY created_at DESC NULLS LAST, id DESC
                """;

        return jdbcTemplate.query(sql, this::mapRow, userId);
    }

    public List<InstitutionApplicationResponse> findByStatus(String status) {
        String sql = """
                SELECT *
                FROM institution_applications
                WHERE status = ?
                ORDER BY created_at DESC NULLS LAST, id DESC
                """;

        return jdbcTemplate.query(sql, this::mapRow, status);
    }

    public Long findOwnedInstitutionId(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT id
                    FROM institutions
                    WHERE owner_user_id = ?
                    ORDER BY updated_at DESC NULLS LAST, id DESC
                    LIMIT 1
                    """,
                    Long.class,
                    userId
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<InstitutionConsoleInstitutionOption> findOwnedInstitutionOptions(Long userId) {
        String sql = """
                SELECT
                    id,
                    name,
                    district,
                    address,
                    status,
                    updated_at
                FROM institutions
                WHERE owner_user_id = ?
                  AND COALESCE(status, 1) = 1
                ORDER BY updated_at DESC NULLS LAST, id DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new InstitutionConsoleInstitutionOption(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("district"),
                        rs.getString("address"),
                        getIntegerSafely(rs, "status"),
                        getInstitutionStatusText(getIntegerSafely(rs, "status")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))
                ),
                userId
        );
    }

    public boolean isOwnedInstitution(Long userId, Long institutionId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM institutions
                WHERE id = ?
                  AND owner_user_id = ?
                  AND COALESCE(status, 1) = 1
                """,
                Integer.class,
                institutionId,
                userId
        );

        return count != null && count > 0;
    }

    public void rejectApplication(
            Long id,
            String reason,
            Long operatorId,
            String operatorName
    ) {
        String oldStatus = findApplicationStatus(id);

        jdbcTemplate.update(
                """
                UPDATE institution_applications
                SET status = 'rejected',
                    reject_reason = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                reason,
                operatorId,
                id
        );

        saveAuditLog(id, "reject", oldStatus, "rejected", operatorId, operatorName, reason);
    }

    public Long approveAndPublishApplication(
            Long applicationId,
            Long operatorId,
            String operatorName
    ) {
        InstitutionApplicationResponse application = findApplicationById(applicationId);

        if (!"pending".equals(application.status())) {
            return application.institutionId();
        }

        Long institutionId = insertInstitutionFromApplication(application);
        replaceServicesFromApplication(institutionId, application.services());
        replaceFacilitiesFromApplication(institutionId, application.facilities());
        refreshSpatialIndex(institutionId);

        jdbcTemplate.update(
                """
                UPDATE institution_applications
                SET status = 'approved',
                    reject_reason = NULL,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    institution_id = ?
                WHERE id = ?
                """,
                operatorId,
                institutionId,
                applicationId
        );

        jdbcTemplate.update(
                """
                UPDATE sys_users
                SET institution_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                institutionId,
                application.userId()
        );

        saveAuditLog(applicationId, "approve", application.status(), "approved", operatorId, operatorName, "审核通过");

        return institutionId;
    }

    public void updateOwnedInstitution(
            Long userId,
            Long institutionId,
            InstitutionProfileUpdateRequest request
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE institutions
                SET name = ?,
                    address = ?,
                    province = ?,
                    city = ?,
                    district = ?,
                    institution_category = ?,
                    grade_level = ?,
                    total_beds = ?,
                    available_beds = ?,
                    monthly_fee_base = ?,
                    price_tier = ?,
                    contact_person = ?,
                    contact_phone = ?,
                    cover_image_url = ?,
                    images = CAST(? AS jsonb),
                    has_panorama = ?,
                    intro = ?,
                    lon_wgs84 = ?,
                    lat_wgs84 = ?,
                    geom = ST_SetSRID(ST_MakePoint(?, ?), 4326),
                    geom_proj = ST_Transform(ST_SetSRID(ST_MakePoint(?, ?), 4326), 4548),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND owner_user_id = ?
                """,
                request.name(),
                request.address(),
                request.province(),
                request.city(),
                request.district(),
                request.institutionCategory(),
                request.gradeLevel(),
                request.totalBeds(),
                request.availableBeds(),
                request.monthlyFeeBase(),
                request.priceTier(),
                request.contactPerson(),
                request.contactPhone(),
                request.coverImageUrl(),
                normalizeJsonArrayText(request.images()),
                request.hasPanorama() == null ? false : request.hasPanorama(),
                request.intro(),
                request.lon(),
                request.lat(),
                request.lon(),
                request.lat(),
                request.lon(),
                request.lat(),
                institutionId,
                userId
        );

        if (updated <= 0) {
            throw new IllegalArgumentException("只能修改本机构账号绑定的养老机构。请确认机构已通过入驻审核。");
        }

        replaceServicesFromProfile(institutionId, request.services());
        replaceFacilitiesFromProfile(institutionId, request.facilities());
    }

    public void refreshSpatialIndex(Long institutionId) {
        try {
            jdbcTemplate.update(
                    "DELETE FROM institution_spatial_index WHERE institution_id = ?",
                    institutionId
            );

            jdbcTemplate.update(
                    """
                    INSERT INTO institution_spatial_index (
                        institution_id,
                        nearest_key_medical_km,
                        key_medical_count_3km,
                        medical_type_count_5km,
                        nearest_life_km,
                        life_count_600m,
                        life_type_count_600m,
                        nearest_park_km,
                        park_count_3km,
                        park_type_count_3km,
                        updated_at
                    )
                    SELECT
                        i.id,
                        (
                            SELECT ROUND((ST_Distance(p.geom::geography, i.geom::geography) / 1000.0)::numeric, 3)::double precision
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 1
                              AND (
                                    p.poi_small IN ('三级甲等医院', '综合医院')
                                    OR p.type_full ILIKE '%三级甲等医院%'
                                    OR p.type_full ILIKE '%综合医院%'
                              )
                            ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                            LIMIT 1
                        ) AS nearest_key_medical_km,
                        (
                            SELECT COUNT(*)::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 1
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 3000)
                              AND (
                                    p.poi_small IN ('三级甲等医院', '综合医院')
                                    OR p.type_full ILIKE '%三级甲等医院%'
                                    OR p.type_full ILIKE '%综合医院%'
                              )
                        ) AS key_medical_count_3km,
                        (
                            SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.type_full, ''), '其他医疗设施'))::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 1
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 5000)
                        ) AS medical_type_count_5km,
                        (
                            SELECT ROUND((ST_Distance(p.geom::geography, i.geom::geography) / 1000.0)::numeric, 3)::double precision
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 2
                            ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                            LIMIT 1
                        ) AS nearest_life_km,
                        (
                            SELECT COUNT(*)::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 2
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 600)
                        ) AS life_count_600m,
                        (
                            SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.poi_mid, ''), NULLIF(p.type_full, ''), '其他生活服务'))::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 2
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 600)
                        ) AS life_type_count_600m,
                        (
                            SELECT ROUND((ST_Distance(p.geom::geography, i.geom::geography) / 1000.0)::numeric, 3)::double precision
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 3
                            ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                            LIMIT 1
                        ) AS nearest_park_km,
                        (
                            SELECT COUNT(*)::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 3
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 3000)
                        ) AS park_count_3km,
                        (
                            SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.poi_mid, ''), NULLIF(p.type_full, ''), '其他公园景点'))::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 3
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 3000)
                        ) AS park_type_count_3km,
                        CURRENT_TIMESTAMP
                    FROM institutions i
                    WHERE i.id = ?
                      AND i.geom IS NOT NULL
                    """,
                    institutionId
            );
        } catch (Exception e) {
            // 推荐接口已经提供实时空间指标兜底计算；刷新缓存失败不应阻断入驻审核或机构维护。
            System.err.println("刷新机构空间指标缓存失败 institutionId=" + institutionId + ": " + e.getMessage());
        }
    }

    private InstitutionApplicationResponse findApplicationById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM institution_applications WHERE id = ?",
                this::mapRow,
                id
        );
    }

    private String findApplicationStatus(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM institution_applications WHERE id = ?",
                String.class,
                id
        );
    }

    private Long insertInstitutionFromApplication(InstitutionApplicationResponse app) {
        String sql = """
                INSERT INTO institutions (
                    name,
                    address,
                    province,
                    city,
                    district,
                    institution_category,
                    grade_level,
                    total_beds,
                    available_beds,
                    monthly_fee_base,
                    price_tier,
                    contact_person,
                    contact_phone,
                    status,
                    owner_user_id,
                    rating_count,
                    cover_image_url,
                    images,
                    has_panorama,
                    intro,
                    lon_wgs84,
                    lat_wgs84,
                    geom,
                    geom_proj,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 0, ?, CAST(? AS jsonb), ?, ?, ?, ?,
                        ST_SetSRID(ST_MakePoint(?, ?), 4326),
                        ST_Transform(ST_SetSRID(ST_MakePoint(?, ?), 4326), 4548),
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                app.institutionName(),
                app.address(),
                app.province(),
                app.city(),
                app.district(),
                app.institutionCategory(),
                app.gradeLevel(),
                app.totalBeds(),
                app.availableBeds(),
                app.monthlyFeeBase(),
                app.priceTier(),
                app.contactPerson(),
                app.contactPhone(),
                app.userId(),
                app.coverImageUrl(),
                normalizeJsonArrayText(app.images()),
                app.hasPanorama() == null ? false : app.hasPanorama(),
                app.intro(),
                app.lon(),
                app.lat(),
                app.lon(),
                app.lat(),
                app.lon(),
                app.lat()
        );
    }

    private void replaceServicesFromApplication(
            Long institutionId,
            List<InstitutionApplicationRequest.ServiceItem> services
    ) {
        jdbcTemplate.update("DELETE FROM institution_services WHERE institution_id = ?", institutionId);

        if (services == null) {
            return;
        }

        for (InstitutionApplicationRequest.ServiceItem item : services) {
            if (item == null || !hasText(item.serviceType())) {
                continue;
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO institution_services (
                        institution_id,
                        service_type,
                        service_detail,
                        is_available
                    )
                    VALUES (?, ?, ?, ?)
                    """,
                    institutionId,
                    item.serviceType(),
                    item.serviceDetail(),
                    item.isAvailable() == null ? true : item.isAvailable()
            );
        }
    }

    private void replaceFacilitiesFromApplication(
            Long institutionId,
            List<InstitutionApplicationRequest.FacilityItem> facilities
    ) {
        jdbcTemplate.update("DELETE FROM institution_facilities WHERE institution_id = ?", institutionId);

        if (facilities == null) {
            return;
        }

        for (InstitutionApplicationRequest.FacilityItem item : facilities) {
            if (item == null || !hasText(item.facilityName())) {
                continue;
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO institution_facilities (
                        institution_id,
                        facility_name,
                        facility_desc
                    )
                    VALUES (?, ?, ?)
                    """,
                    institutionId,
                    item.facilityName(),
                    item.facilityDesc()
            );
        }
    }

    private void replaceServicesFromProfile(
            Long institutionId,
            List<InstitutionProfileUpdateRequest.ServiceItem> services
    ) {
        jdbcTemplate.update("DELETE FROM institution_services WHERE institution_id = ?", institutionId);

        if (services == null) {
            return;
        }

        for (InstitutionProfileUpdateRequest.ServiceItem item : services) {
            if (item == null || !hasText(item.serviceType())) {
                continue;
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO institution_services (
                        institution_id,
                        service_type,
                        service_detail,
                        is_available
                    )
                    VALUES (?, ?, ?, ?)
                    """,
                    institutionId,
                    item.serviceType(),
                    item.serviceDetail(),
                    item.isAvailable() == null ? true : item.isAvailable()
            );
        }
    }

    private void replaceFacilitiesFromProfile(
            Long institutionId,
            List<InstitutionProfileUpdateRequest.FacilityItem> facilities
    ) {
        jdbcTemplate.update("DELETE FROM institution_facilities WHERE institution_id = ?", institutionId);

        if (facilities == null) {
            return;
        }

        for (InstitutionProfileUpdateRequest.FacilityItem item : facilities) {
            if (item == null || !hasText(item.facilityName())) {
                continue;
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO institution_facilities (
                        institution_id,
                        facility_name,
                        facility_desc
                    )
                    VALUES (?, ?, ?)
                    """,
                    institutionId,
                    item.facilityName(),
                    item.facilityDesc()
            );
        }
    }

    private void saveAuditLog(
            Long id,
            String action,
            String oldStatus,
            String newStatus,
            Long operatorId,
            String operatorName,
            String reason
    ) {
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
                action,
                oldStatus,
                newStatus,
                operatorId,
                operatorName,
                reason
        );
    }

    private InstitutionApplicationResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        String servicesText = getStringSafely(rs, "services_json");
        String facilitiesText = getStringSafely(rs, "facilities_json");

        return new InstitutionApplicationResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                getLongSafely(rs, "institution_id"),

                rs.getString("institution_name"),
                rs.getString("contact_person"),
                rs.getString("contact_phone"),

                getStringSafely(rs, "province"),
                getStringSafely(rs, "city"),
                rs.getString("district"),
                rs.getString("address"),
                getStringSafely(rs, "address_name"),
                getDouble(rs.getObject("lon")),
                getDouble(rs.getObject("lat")),
                getStringSafely(rs, "point_source"),

                getIntegerSafely(rs, "institution_category"),
                institutionCategoryText(getIntegerSafely(rs, "institution_category")),
                getStringSafely(rs, "grade_level"),
                getIntegerSafely(rs, "total_beds"),
                getIntegerSafely(rs, "available_beds"),
                getBigDecimalSafely(rs, "monthly_fee_base"),
                getIntegerSafely(rs, "price_tier"),
                priceTierText(getIntegerSafely(rs, "price_tier")),

                getStringSafely(rs, "cover_image_url"),
                getStringSafely(rs, "images"),
                getBooleanSafely(rs, "has_panorama"),
                getStringSafely(rs, "intro"),
                rs.getString("description"),

                parseServices(servicesText),
                parseFacilities(facilitiesText),

                rs.getString("license_url"),
                rs.getString("record_certificate_url"),
                rs.getString("other_material_url"),

                rs.getString("status"),
                getStatusText(rs.getString("status")),
                rs.getString("reject_reason"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("reviewed_at"))
        );
    }

    private List<InstitutionApplicationRequest.ServiceItem> parseServices(String json) {
        if (!hasText(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<InstitutionApplicationRequest.ServiceItem>>() {}
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<InstitutionApplicationRequest.FacilityItem> parseFacilities(String json) {
        if (!hasText(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<InstitutionApplicationRequest.FacilityItem>>() {}
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String normalizeJsonArrayText(String value) {
        if (!hasText(value)) {
            return "[]";
        }

        String trimmed = value.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        return toJson(List.of(trimmed));
    }

    private String getInstitutionStatusText(Integer status) {
        if (status == null) {
            return "正常展示";
        }

        return switch (status) {
            case 1 -> "正常展示";
            case 0 -> "待上架";
            case 2 -> "已下架";
            default -> "未知状态";
        };
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

    private String institutionCategoryText(Integer category) {
        if (category == null) {
            return "未设置";
        }

        return switch (category) {
            case 1 -> "公办公营";
            case 2 -> "公办民营";
            case 3 -> "民营";
            default -> "未设置";
        };
    }

    private String priceTierText(Integer priceTier) {
        if (priceTier == null) {
            return "未设置";
        }

        return switch (priceTier) {
            case 1 -> "3000 元以下";
            case 2 -> "3000-5000 元";
            case 3 -> "5000-8000 元";
            case 4 -> "8000 元以上";
            default -> "未设置";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Double getDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String getStringSafely(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLongSafely(ResultSet rs, String column) {
        try {
            Object value = rs.getObject(column);
            return value == null ? null : ((Number) value).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getIntegerSafely(ResultSet rs, String column) {
        try {
            Object value = rs.getObject(column);
            return value == null ? null : ((Number) value).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalSafely(ResultSet rs, String column) {
        try {
            return rs.getBigDecimal(column);
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getBooleanSafely(ResultSet rs, String column) {
        try {
            Object value = rs.getObject(column);
            return value == null ? null : (Boolean) value;
        } catch (Exception e) {
            return null;
        }
    }
}
