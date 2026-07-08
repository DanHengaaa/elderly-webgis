package com.hhu.elderly.analysis.service;

import com.hhu.elderly.analysis.dto.VisitAccessibilityResponse;
import com.hhu.elderly.analysis.dto.VisitAccessibilityResponse.IsochroneRing;
import com.hhu.elderly.analysis.dto.VisitAccessibilityResponse.ReachableInstitution;
import com.hhu.elderly.analysis.repository.VisitAccessibilityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VisitAccessibilityService {

    private final VisitAccessibilityRepository repository;

    public VisitAccessibilityService(VisitAccessibilityRepository repository) {
        this.repository = repository;
    }

    public VisitAccessibilityResponse analyze(
            Double lon,
            Double lat,
            String startName,
            String mode
    ) {
        if (lon == null || lat == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "起点经纬度不能为空"
            );
        }

        Long startNode = repository.findNearestRoadNode(lon, lat);

        if (startNode == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "未找到距离起点最近的路网节点"
            );
        }

        List<IsochroneRing> rings = List.of(
                repository.buildIsochroneRing(startNode, 15 * 60, 15),
                repository.buildIsochroneRing(startNode, 30 * 60, 30),
                repository.buildIsochroneRing(startNode, 60 * 60, 60)
        );

        List<ReachableInstitution> reachableInstitutions =
                repository.findReachableInstitutions(startNode, 60 * 60);

        return new VisitAccessibilityResponse(
                lon,
                lat,
                startName == null || startName.isBlank() ? "自定义起点" : startName,
                mode == null || mode.isBlank() ? "driving" : mode,
                rings,
                reachableInstitutions
        );
    }
}