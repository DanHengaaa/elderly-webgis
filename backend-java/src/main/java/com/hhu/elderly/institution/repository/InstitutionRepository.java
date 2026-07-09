package com.hhu.elderly.institution.repository;

import com.hhu.elderly.institution.dto.InstitutionDetailResponse;
import com.hhu.elderly.institution.dto.InstitutionDetailResponse.FacilityItem;
import com.hhu.elderly.institution.dto.InstitutionDetailResponse.ReviewItem;
import com.hhu.elderly.institution.dto.InstitutionDetailResponse.ServiceItem;
import com.hhu.elderly.institution.dto.InstitutionListItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class InstitutionRepository {

    private final JdbcTemplate jdbcTemplate;

    public InstitutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<InstitutionListItem> findList(
            String keyword,
            String district,
            Integer institutionCategory,
            String gradeLevel,
            Integer priceTier,
            String serviceType,
            Boolean hasAvailableBeds,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    i.id,
                    i.name,
                    i.address,
                    i.district,
                    i.institution_category,
                    i.grade_level,
                    i.total_beds,
                    i.available_beds,
                    i.monthly_fee_base,
                    i.price_tier,
                    (
                        SELECT string_agg(DISTINCT s.service_type, ',')
                        FROM institution_services s
                        WHERE s.institution_id = i.id
                          AND s.is_available IS DISTINCT FROM FALSE
                    ) AS service_types,
                    i.rating_avg,
                    i.rating_count,
                    i.cover_image_url,
                    ST_X(i.geom) AS lon,
                    ST_Y(i.geom) AS lat
                FROM institutions i
                WHERE i.status = 1
                  AND i.geom IS NOT NULL
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                    AND (
                        i.name ILIKE ?
                        OR i.address ILIKE ?
                    )
                    """);
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (district != null && !district.isBlank()) {
            sql.append(" AND i.district = ? ");
            params.add(district.trim());
        }

        if (institutionCategory != null) {
            sql.append(" AND i.institution_category = ? ");
            params.add(institutionCategory);
        }

        if (gradeLevel != null && !gradeLevel.isBlank()) {
            sql.append(" AND i.grade_level = ? ");
            params.add(gradeLevel.trim());
        }

        if (priceTier != null) {
            sql.append(" AND i.price_tier = ? ");
            params.add(priceTier);
        }

        if (Boolean.TRUE.equals(hasAvailableBeds)) {
            sql.append(" AND COALESCE(i.available_beds, 0) > 0 ");
        }

        if (serviceType != null && !serviceType.isBlank()) {
            List<String> serviceTypeAliases = serviceTypeAliases(serviceType.trim());
            String placeholders = String.join(",", Collections.nCopies(serviceTypeAliases.size(), "?"));

            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM institution_services s
                        WHERE s.institution_id = i.id
                          AND s.is_available IS DISTINCT FROM FALSE
                          AND s.service_type IN (
                    """)
                    .append(placeholders)
                    .append("""
                          )
                    )
                    """);

            params.addAll(serviceTypeAliases);
        }

        sql.append("""
                ORDER BY
                    i.rating_avg DESC NULLS LAST,
                    i.id ASC
                LIMIT ?
                OFFSET ?
                """);

        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), this::mapListRow, params.toArray());
    }

    public Optional<InstitutionDetailResponse> findDetailById(Long id) {
        List<BasicDetail> detailList = jdbcTemplate.query("""
                SELECT
                    i.id,
                    i.fid,
                    i.object_id,
                    i.name,
                    i.address,
                    i.province,
                    i.city,
                    i.district,
                    i.poi_type_full,
                    i.poi_big,
                    i.poi_mid,
                    i.poi_small,
                    i.lon_gcj02,
                    i.lat_gcj02,
                    i.lon_wgs84,
                    i.lat_wgs84,
                    ST_X(i.geom) AS lon,
                    ST_Y(i.geom) AS lat,
                    ST_AsText(i.geom) AS geom_wkt,
                    CASE
                        WHEN i.geom_proj IS NULL THEN NULL
                        ELSE ST_AsText(i.geom_proj)
                    END AS geom_proj_wkt,
                    i.institution_category,
                    i.grade_level,
                    i.total_beds,
                    i.available_beds,
                    i.monthly_fee_base,
                    i.price_tier,
                    i.contact_person,
                    i.contact_phone,
                    i.status,
                    i.owner_user_id,
                    i.rating_avg,
                    i.rating_count,
                    i.cover_image_url,
                    i.images::text AS images_text,
                    i.has_panorama,
                    i.intro,
                    i.extra_data::text AS extra_data_text,
                    i.created_at,
                    i.updated_at
                FROM institutions i
                WHERE i.id = ?
                """, this::mapBasicDetail, id);

        if (detailList.isEmpty()) {
            return Optional.empty();
        }

        BasicDetail d = detailList.get(0);

        List<ServiceItem> services = findServicesByInstitutionId(id);
        List<FacilityItem> facilities = findFacilitiesByInstitutionId(id);
        List<ReviewItem> reviews = findReviewsByInstitutionId(id);

        return Optional.of(new InstitutionDetailResponse(
                d.id(),
                d.fid(),
                d.objectId(),

                d.name(),
                d.address(),
                d.province(),
                d.city(),
                d.district(),

                d.poiTypeFull(),
                d.poiBig(),
                d.poiMid(),
                d.poiSmall(),

                d.lonGcj02(),
                d.latGcj02(),
                d.lonWgs84(),
                d.latWgs84(),
                d.lon(),
                d.lat(),

                d.geomWkt(),
                d.geomProjWkt(),

                d.institutionCategory(),
                institutionCategoryText(d.institutionCategory()),

                d.gradeLevel(),

                d.totalBeds(),
                d.availableBeds(),

                d.monthlyFeeBase(),
                d.priceTier(),
                priceTierText(d.priceTier()),

                d.contactPerson(),
                d.contactPhone(),

                d.status(),
                statusText(d.status()),

                d.ownerUserId(),

                d.ratingAvg(),
                d.ratingCount(),

                d.coverImageUrl(),
                d.images(),
                d.hasPanorama(),
                d.intro(),

                d.extraData(),

                d.createdAt(),
                d.updatedAt(),

                services,
                facilities,
                reviews
        ));
    }

    private List<ServiceItem> findServicesByInstitutionId(Long institutionId) {
        return jdbcTemplate.query("""
                SELECT
                    id,
                    service_type,
                    service_detail,
                    is_available
                FROM institution_services
                WHERE institution_id = ?
                ORDER BY id ASC
                """, this::mapServiceItem, institutionId);
    }

    private List<FacilityItem> findFacilitiesByInstitutionId(Long institutionId) {
        return jdbcTemplate.query("""
                SELECT
                    id,
                    facility_name,
                    facility_desc
                FROM institution_facilities
                WHERE institution_id = ?
                ORDER BY id ASC
                """, this::mapFacilityItem, institutionId);
    }

    private List<ReviewItem> findReviewsByInstitutionId(Long institutionId) {
        return jdbcTemplate.query("""
                SELECT
                    id,
                    user_id,
                    rating_avg,
                    rating_medical,
                    rating_hardware,
                    rating_food,
                    rating_service,
                    content,
                    images::text AS images_text,
                    is_approved,
                    created_at
                FROM institution_reviews
                WHERE institution_id = ?
                ORDER BY created_at DESC NULLS LAST, id DESC
                LIMIT 30
                """, this::mapReviewItem, institutionId);
    }

    private InstitutionListItem mapListRow(ResultSet rs, int rowNum) throws SQLException {
        Integer institutionCategory = getInteger(rs, "institution_category");
        Integer priceTier = getInteger(rs, "price_tier");

        return new InstitutionListItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),

                institutionCategory,
                institutionCategoryText(institutionCategory),

                rs.getString("grade_level"),
                getInteger(rs, "total_beds"),
                getInteger(rs, "available_beds"),

                rs.getBigDecimal("monthly_fee_base"),
                priceTier,
                priceTierText(priceTier),

                rs.getString("service_types"),
                serviceTypesText(rs.getString("service_types")),

                rs.getBigDecimal("rating_avg"),
                getInteger(rs, "rating_count"),

                rs.getString("cover_image_url"),

                getDouble(rs, "lon"),
                getDouble(rs, "lat")
        );
    }

    private BasicDetail mapBasicDetail(ResultSet rs, int rowNum) throws SQLException {
        return new BasicDetail(
                getLong(rs, "id"),
                rs.getString("fid"),
                getLong(rs, "object_id"),

                rs.getString("name"),
                rs.getString("address"),
                rs.getString("province"),
                rs.getString("city"),
                rs.getString("district"),

                rs.getString("poi_type_full"),
                rs.getString("poi_big"),
                rs.getString("poi_mid"),
                rs.getString("poi_small"),

                getDouble(rs, "lon_gcj02"),
                getDouble(rs, "lat_gcj02"),
                getDouble(rs, "lon_wgs84"),
                getDouble(rs, "lat_wgs84"),
                getDouble(rs, "lon"),
                getDouble(rs, "lat"),

                rs.getString("geom_wkt"),
                rs.getString("geom_proj_wkt"),

                getInteger(rs, "institution_category"),

                rs.getString("grade_level"),

                getInteger(rs, "total_beds"),
                getInteger(rs, "available_beds"),

                rs.getBigDecimal("monthly_fee_base"),
                getInteger(rs, "price_tier"),

                rs.getString("contact_person"),
                rs.getString("contact_phone"),

                getInteger(rs, "status"),
                getLong(rs, "owner_user_id"),

                rs.getBigDecimal("rating_avg"),
                getInteger(rs, "rating_count"),

                rs.getString("cover_image_url"),
                rs.getString("images_text"),
                getBoolean(rs, "has_panorama"),
                rs.getString("intro"),

                rs.getString("extra_data_text"),

                getTimestampText(rs, "created_at"),
                getTimestampText(rs, "updated_at")
        );
    }

    private ServiceItem mapServiceItem(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceItem(
                getLong(rs, "id"),
                rs.getString("service_type"),
                rs.getString("service_detail"),
                getBoolean(rs, "is_available")
        );
    }

    private FacilityItem mapFacilityItem(ResultSet rs, int rowNum) throws SQLException {
        return new FacilityItem(
                getLong(rs, "id"),
                rs.getString("facility_name"),
                rs.getString("facility_desc")
        );
    }

    private ReviewItem mapReviewItem(ResultSet rs, int rowNum) throws SQLException {
        return new ReviewItem(
                getLong(rs, "id"),
                getLong(rs, "user_id"),
                rs.getBigDecimal("rating_avg"),
                rs.getBigDecimal("rating_medical"),
                rs.getBigDecimal("rating_hardware"),
                rs.getBigDecimal("rating_food"),
                rs.getBigDecimal("rating_service"),
                rs.getString("content"),
                rs.getString("images_text"),
                getBoolean(rs, "is_approved"),
                getTimestampText(rs, "created_at")
        );
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    private Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return (Boolean) value;
    }

    private String getTimestampText(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toString().replace("T", " ");
    }

    private String institutionCategoryText(Integer value) {
        if (value == null) {
            return "未知性质";
        }

        return switch (value) {
            case 1 -> "公办公营";
            case 2 -> "公办民营";
            case 3 -> "民营";
            default -> "未知性质";
        };
    }

    private List<String> serviceTypeAliases(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        String normalized = value.trim();

        return switch (normalized) {
            case "selfCare", "self_care", "SELF_CARE", "基本自理", "自理", "自理照护" ->
                    List.of("selfCare", "self_care", "SELF_CARE", "基本自理", "自理", "自理照护");
            case "semiCare", "semi_care", "SEMI_CARE", "半失能", "半失能照护" ->
                    List.of("semiCare", "semi_care", "SEMI_CARE", "半失能", "半失能照护");
            case "nursing", "NURSING", "失能", "失能护理" ->
                    List.of("nursing", "NURSING", "失能", "失能护理");
            case "dementia", "DEMENTIA", "失智", "认知症", "认知症照护" ->
                    List.of("dementia", "DEMENTIA", "失智", "认知症", "认知症照护");
            case "rehab", "REHAB", "康复", "康复护理", "术后康复" ->
                    List.of("rehab", "REHAB", "康复", "康复护理", "术后康复");
            case "medical", "medicalCare", "medical_care", "MEDICAL_CARE", "医养结合", "医养结合照护" ->
                    List.of("medical", "medicalCare", "medical_care", "MEDICAL_CARE", "医养结合", "医养结合照护");
            default -> List.of(normalized);
        };
    }

    private String serviceTypesText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        List<String> names = new ArrayList<>();

        for (String item : value.split(",")) {
            String text = serviceTypeText(item);

            if (text != null && !text.isBlank() && !names.contains(text)) {
                names.add(text);
            }
        }

        return names.isEmpty() ? null : String.join("、", names);
    }

    private String serviceTypeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim()) {
            case "selfCare", "self_care", "SELF_CARE", "基本自理", "自理", "自理照护" -> "基本自理";
            case "semiCare", "semi_care", "SEMI_CARE", "半失能", "半失能照护" -> "半失能照护";
            case "nursing", "NURSING", "失能", "失能护理" -> "失能护理";
            case "dementia", "DEMENTIA", "失智", "认知症", "认知症照护" -> "认知症照护";
            case "rehab", "REHAB", "康复", "康复护理", "术后康复" -> "康复护理";
            case "medical", "medicalCare", "medical_care", "MEDICAL_CARE", "医养结合", "医养结合照护" -> "医养结合";
            default -> value.trim();
        };
    }

    private String priceTierText(Integer value) {
        if (value == null) {
            return "未设置";
        }

        return switch (value) {
            case 1 -> "经济型";
            case 2 -> "标准型";
            case 3 -> "高端型";
            default -> "未设置";
        };
    }

    private String statusText(Integer value) {
        if (value == null) {
            return "未知状态";
        }

        return switch (value) {
            case 0 -> "已下架";
            case 1 -> "已上架";
            default -> "未知状态";
        };
    }

    private record BasicDetail(
            Long id,
            String fid,
            Long objectId,

            String name,
            String address,
            String province,
            String city,
            String district,

            String poiTypeFull,
            String poiBig,
            String poiMid,
            String poiSmall,

            Double lonGcj02,
            Double latGcj02,
            Double lonWgs84,
            Double latWgs84,
            Double lon,
            Double lat,

            String geomWkt,
            String geomProjWkt,

            Integer institutionCategory,

            String gradeLevel,

            Integer totalBeds,
            Integer availableBeds,

            BigDecimal monthlyFeeBase,
            Integer priceTier,

            String contactPerson,
            String contactPhone,

            Integer status,
            Long ownerUserId,

            BigDecimal ratingAvg,
            Integer ratingCount,

            String coverImageUrl,
            String images,
            Boolean hasPanorama,
            String intro,

            String extraData,

            String createdAt,
            String updatedAt
    ) {
    }
}