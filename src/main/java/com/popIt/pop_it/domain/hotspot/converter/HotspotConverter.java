package com.popIt.pop_it.domain.hotspot.converter;

import com.popIt.pop_it.domain.hotspot.dto.HotspotResDTO;
import com.popIt.pop_it.domain.hotspot.entity.Hotspot;

import java.util.List;

public class HotspotConverter {

    public static HotspotResDTO.HotspotSummaryRes toSummary(Hotspot hotspot) {
        return HotspotResDTO.HotspotSummaryRes.builder()
                .hotspotId(hotspot.getId())
                .position(List.of(hotspot.getPositionX(), hotspot.getPositionY(), hotspot.getPositionZ()))
                .type(hotspot.getType())
                .label(hotspot.getLabel())
                .description(hotspot.getDescription())
                .targetSceneId(hotspot.getTargetSceneId())
                .build();
    }

    public static List<HotspotResDTO.HotspotSummaryRes> toSummaryList(List<Hotspot> hotspots) {
        return hotspots.stream()
                .map(HotspotConverter::toSummary)
                .toList();
    }

    public static HotspotResDTO.HotspotIdRes toHotspotId(Hotspot hotspot) {
        return HotspotResDTO.HotspotIdRes.builder()
                .hotspotId(hotspot.getId())
                .build();
    }
}
