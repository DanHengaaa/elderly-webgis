package com.hhu.elderly.analysis.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhu.elderly.analysis.dto.GeoCodeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/geo")
public class GeoCodeController {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${tianditu.tk:01c5845db0eb91889d42399c5a5b4f16}")
    private String tiandituTk;

    public GeoCodeController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/geocode")
    public GeoCodeResponse geocode(@RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜索关键词不能为空"
            );
        }

        String fixedKeyword = keyword.contains("南京")
                ? keyword.trim()
                : "南京市" + keyword.trim();

        try {
            return searchByTiandituPoiSearch(keyword.trim(), fixedKeyword);
        } catch (Exception searchException) {
            try {
                return searchByTiandituGeoCoder(keyword.trim(), fixedKeyword);
            } catch (Exception geoException) {
                String message = "天地图搜索失败。"
                        + "POI搜索错误：" + searchException.getMessage()
                        + "；地理编码错误：" + geoException.getMessage();

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        message
                );
            }
        }
    }

    @GetMapping("/geocode/ping")
    public String ping() {
        return "geocode controller ok";
    }

    private GeoCodeResponse searchByTiandituPoiSearch(
            String originalKeyword,
            String fixedKeyword
    ) throws Exception {
        /*
         * 南京大致范围：
         * 西南角 118.35,31.20
         * 东北角 119.30,32.70
         *
         * queryType = 1：普通关键词搜索
         */
        String postStr = objectMapper.writeValueAsString(Map.of(
                "keyWord", fixedKeyword,
                "level", 12,
                "mapBound", "118.350000,31.200000,119.300000,32.700000",
                "queryType", 1,
                "start", 0,
                "count", 10
        ));

        String url = "https://api.tianditu.gov.cn/v2/search"
                + "?postStr=" + encode(postStr)
                + "&type=query"
                + "&tk=" + encode(tiandituTk);

        String body = getBody(url);

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("天地图 POI 搜索接口返回为空");
        }

        JsonNode root = objectMapper.readTree(body);

        JsonNode pois = root.path("pois");

        if (!pois.isArray() || pois.isEmpty()) {
            throw new IllegalStateException("天地图 POI 搜索未找到结果：" + body);
        }

        JsonNode firstPoi = pois.get(0);
        String lonlat = firstPoi.path("lonlat").asText("");

        if (lonlat.isBlank() || !lonlat.contains(",")) {
            throw new IllegalStateException("POI 搜索结果缺少 lonlat 字段：" + body);
        }

        String[] parts = lonlat.split(",");

        if (parts.length < 2) {
            throw new IllegalStateException("POI 搜索 lonlat 格式异常：" + lonlat);
        }

        Double lon = Double.parseDouble(parts[0].trim());
        Double lat = Double.parseDouble(parts[1].trim());

        String name = firstPoi.path("name").asText(fixedKeyword);
        String address = firstPoi.path("address").asText("");

        String formattedAddress = address == null || address.isBlank()
                ? name
                : name + "，" + address;

        return new GeoCodeResponse(
                originalKeyword,
                formattedAddress,
                lon,
                lat
        );
    }

    private GeoCodeResponse searchByTiandituGeoCoder(
            String originalKeyword,
            String fixedKeyword
    ) throws Exception {
        String ds = objectMapper.writeValueAsString(Map.of(
                "keyWord", fixedKeyword
        ));

        String url = "https://api.tianditu.gov.cn/geocoder"
                + "?ds=" + encode(ds)
                + "&tk=" + encode(tiandituTk);

        String body = getBody(url);

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("天地图地理编码接口返回为空");
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode result = root.path("result");
        JsonNode location = result.path("location");

        if (location.isMissingNode()
                || location.path("lon").isMissingNode()
                || location.path("lat").isMissingNode()) {
            throw new IllegalStateException("地理编码接口未返回有效经纬度：" + body);
        }

        Double lon = location.path("lon").asDouble();
        Double lat = location.path("lat").asDouble();

        String formattedAddress = result.path("formatted_address").asText(fixedKeyword);

        return new GeoCodeResponse(
                originalKeyword,
                formattedAddress,
                lon,
                lat
        );
    }

    private String getBody(String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 elderly-care-webgis");
        headers.set("Accept", "application/json,text/plain,*/*");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("HTTP 状态码异常：" + response.getStatusCode());
        }

        return response.getBody();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}