package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.exception.ContractException;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractResDTO.GetGuestContractInfoRes getContractInfo(Long reservationId) {
        throw new ProjectException(GeneralErrorCode.FUNCTION_ERROR);
    }

    public ContractResDTO.SignatureRes signature(Long reservationId, ContractReqDTO.SignatureReq dto) {
        throw new ProjectException(GeneralErrorCode.FUNCTION_ERROR);
    }
}
