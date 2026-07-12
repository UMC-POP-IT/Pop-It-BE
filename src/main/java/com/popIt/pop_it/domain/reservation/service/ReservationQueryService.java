package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.reservation.converter.ReservationConverter;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;
    private final SpaceImageRepository spaceImageRepository;

    public List<ReservationResDTO.Summary> getMyReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, String> thumbnails = getThumbnailMap(reservations);

        return reservations.stream()
                .map(r -> ReservationConverter.toSummary(r, false, thumbnails.get(r.getSpace().getId())))
                .toList();
    }

    public List<ReservationResDTO.Summary> getHostReservations(Long hostId) {
        List<Reservation> reservations = reservationRepository.findAllByHostId(hostId);
        Map<Long, String> thumbnails = getThumbnailMap(reservations);

        return reservations.stream()
                .map(r -> ReservationConverter.toSummary(r, true, thumbnails.get(r.getSpace().getId())))
                .toList();
    }

    private Map<Long, String> getThumbnailMap(List<Reservation> reservations) {
        List<Long> spaceIds = reservations.stream()
                .map(r -> r.getSpace().getId())
                .distinct()
                .toList();

        if (spaceIds.isEmpty()) return Map.of();

        return spaceImageRepository.findThumbnailsBySpaceIds(spaceIds).stream()
                .collect(Collectors.toMap(
                        si -> si.getSpace().getId(),
                        SpaceImage::getImageUrl
                ));
    }
}
