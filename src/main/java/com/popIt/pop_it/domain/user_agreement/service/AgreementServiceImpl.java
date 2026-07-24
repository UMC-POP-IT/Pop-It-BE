package com.popIt.pop_it.domain.user_agreement.service;

import com.popIt.pop_it.domain.terms.entity.Terms;
import com.popIt.pop_it.domain.terms.repository.TermsRepository;
import com.popIt.pop_it.domain.user_agreement.converter.AgreementConverter;
import com.popIt.pop_it.domain.user_agreement.dto.AgreementReqDTO;
import com.popIt.pop_it.domain.user_agreement.dto.AgreementResDTO;
import com.popIt.pop_it.domain.user_agreement.entity.UserAgreement;
import com.popIt.pop_it.domain.user_agreement.exception.code.AgreementErrorCode;
import com.popIt.pop_it.domain.user_agreement.repository.UserAgreementRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    private final UserAgreementRepository userAgreementRepository;
    private final TermsRepository termsRepository;

    @Override
    @Transactional
    public AgreementResDTO.SaveResult save(Long userId, AgreementReqDTO.Save request) {
        // 요청에 같은 약관이 중복으로 들어와도 마지막 값이 반영되도록 termId 기준으로 정규화 (insert 시 유니크 제약 충돌 방지)
        Map<Long, Boolean> desired = new LinkedHashMap<>();
        for (AgreementReqDTO.Item item : request.agreements()) {
            desired.put(item.termId(), item.isAgreed());
        }
        List<Long> termIds = new ArrayList<>(desired.keySet());

        // 요청한 약관이 모두 실제 존재하는지 검증 (외래키 무결성 보강)
        Map<Long, Terms> termMap = termsRepository.findAllById(termIds).stream()
                .collect(Collectors.toMap(Terms::getId, Function.identity()));
        if (termMap.size() != termIds.size()) {
            throw new ProjectException(AgreementErrorCode.TERM_NOT_FOUND);
        }

        // 필수 약관은 미동의(false)로 저장할 수 없음
        desired.forEach((termId, isAgreed) -> {
            if (termMap.get(termId).isRequired() && !isAgreed) {
                throw new ProjectException(AgreementErrorCode.REQUIRED_TERM_NOT_AGREED);
            }
        });

        // 기존 동의 이력 조회 → 있으면 update, 없으면 insert (upsert)
        Map<Long, UserAgreement> existing = userAgreementRepository
                .findAllByUserIdAndTermIdIn(userId, termIds).stream()
                .collect(Collectors.toMap(UserAgreement::getTermId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        List<UserAgreement> toSave = new ArrayList<>();
        desired.forEach((termId, isAgreed) -> {
            UserAgreement agreement = existing.get(termId);
            if (agreement != null) {
                agreement.updateAgreement(isAgreed, now);
                toSave.add(agreement);
            } else {
                toSave.add(AgreementConverter.toEntity(userId, termId, isAgreed, now));
            }
        });

        List<UserAgreement> saved = userAgreementRepository.saveAll(toSave);
        return AgreementConverter.toSaveResult(saved);
    }
}
