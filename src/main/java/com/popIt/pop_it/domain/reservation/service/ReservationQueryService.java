package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.reservation.converter.ReservationConverter;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.CheckoutImage;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.CheckoutImageRepository;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.reservation.repository.ReservationStatusCount;
import com.popIt.pop_it.domain.reservation.util.ReservationCursor;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.space.entity.SpaceImage;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final CheckoutImageRepository checkoutImageRepository;
    private final SpaceRepository spaceRepository;

    //게스트 예약 목록 조회 (커서 기반 + 상태 필터)
    public ReservationResDTO.PagedSummary getMyReservations(
            Long userId, ReservationStatus status, String cursor, int size
    ) {
        ReservationCursor.Decoded decoded = ReservationCursor.decode(cursor);
        Slice<Reservation> slice = reservationRepository.findMyReservationsByCursor(
                userId, status, decoded.createdAt(), decoded.id(), PageRequest.of(0, size)
        );
        return toPagedSummary(slice, false);
    }

    //호스트 예약 목록 조회 (커서 기반 + 상태 필터)
    public ReservationResDTO.PagedSummary getHostReservations(
            Long hostId, ReservationStatus status, String cursor, int size
    ) {
        ReservationCursor.Decoded decoded = ReservationCursor.decode(cursor);
        Slice<Reservation> slice = reservationRepository.findHostReservationsByCursor(
                hostId, status, decoded.createdAt(), decoded.id(), PageRequest.of(0, size)
        );
        return toPagedSummary(slice, true);
    }

    //게스트 상태별 탭 카운트
    public ReservationResDTO.StatusCounts getMyStatusCounts(Long userId) {
        return toStatusCounts(reservationRepository.countMyReservationsByStatus(userId));
    }

    //호스트 상태별 탭 카운트
    public ReservationResDTO.StatusCounts getHostStatusCounts(Long hostId) {
        return toStatusCounts(reservationRepository.countHostReservationsByStatus(hostId));
    }

    //공간별 예약 불가(선점된) 날짜 목록 - 프론트 캘린더 비활성화 표시용
    public ReservationResDTO.UnavailableDates getUnavailableDates(Long spaceId) {
        if (!spaceRepository.existsById(spaceId)) {
            throw new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND);
        }

        return ReservationConverter.toUnavailableDates(
                reservationRepository.findUnavailableDateRangesBySpaceId(
                        spaceId, List.of(ReservationStatus.CANCELLED), LocalDate.now()
                )
        );
    }

    //퇴실 증빙 사진 목록 조회(호스트가 승인/거절 전 확인용)
    public ReservationResDTO.CheckoutImages getCheckoutImages(Long reservationId, Long hostId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getSpace().getHostId().equals(hostId)) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        List<CheckoutImage> images = checkoutImageRepository.findAllByReservationIdOrderBySortOrder(reservationId);
        return ReservationConverter.toCheckoutImages(images);
    }

    //페이징
    private ReservationResDTO.PagedSummary toPagedSummary(Slice<Reservation> slice, boolean includeGuest) {
        List<Reservation> reservations = slice.getContent();
        Map<Long, String> thumbnails = getThumbnailMap(reservations);
        Set<Long> verifiedIds = getVerifiedReservationIds(reservations);

        List<ReservationResDTO.Summary> summaries = reservations.stream()
                .map(r -> ReservationConverter.toSummary(
                        r, includeGuest, thumbnails.get(r.getSpace().getId()), verifiedIds.contains(r.getId())
                ))
                .toList();

        String nextCursor = (slice.hasNext() && !reservations.isEmpty())
                ? ReservationCursor.encode(
                        reservations.get(reservations.size() - 1).getCreatedAt(),
                        reservations.get(reservations.size() - 1).getId()
                  )
                : null;

        return ReservationResDTO.PagedSummary.builder()
                .reservations(summaries)
                .hasNext(slice.hasNext())
                .nextCursor(nextCursor)
                .build();
    }

    //상태별 예약 갯수 카운트 DTO변환
    private ReservationResDTO.StatusCounts toStatusCounts(List<ReservationStatusCount> counts) {
        Map<ReservationStatus, Long> countMap = counts.stream()
                .collect(Collectors.toMap(ReservationStatusCount::getStatus, ReservationStatusCount::getCount));
        return ReservationResDTO.StatusCounts.builder()
                .countsByStatus(countMap)
                .build();
    }

    //체크아웃 사진 인증한 예약만 set으로 반환
    private Set<Long> getVerifiedReservationIds(List<Reservation> reservations) {
        List<Long> reservationIds = reservations.stream()
                .map(Reservation::getId)
                .toList();

        if (reservationIds.isEmpty()) return Set.of();

        return new HashSet<>(checkoutImageRepository.findVerifiedReservationIds(reservationIds));
    }

    //각 예약 대표 사진 불러오기
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
