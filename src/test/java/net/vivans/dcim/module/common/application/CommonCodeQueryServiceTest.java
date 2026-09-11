package net.vivans.dcim.module.common.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.common.api.dto.CommonCodeRequest;
import net.vivans.dcim.module.common.api.dto.CommonCodeResponse;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class CommonCodeQueryServiceTest {

    @Mock
    private CommonCodeRepository commonCodeRepository;

    @Mock
    private CodeGroupRepository codeGroupRepository;

    @InjectMocks
    private CommonCodeQueryService commonCodeQueryService;

    @Test
    void createCommonCode_returnCommonCodeResponse(){
        CodeGroup codeGroup = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        ReflectionTestUtils.setField(codeGroup, "id", 1);

        given(codeGroupRepository.findById(1)).willReturn(Optional.of(codeGroup));
        given(commonCodeRepository.existsByCodeGroupIdAndCode(1, "pdu")).willReturn(false);

        CommonCodeRequest request = new CommonCodeRequest(1, "pdu", "pdu", 1);
        CommonCodeResponse codeResponse = commonCodeQueryService.createCommonCode(request);


        assertThat(codeResponse.groupId()).isEqualTo(1);
        assertThat(codeResponse.code()).isEqualTo("pdu");
        assertThat(codeResponse.name()).isEqualTo("pdu");
        assertThat(codeResponse.sortOrder()).isEqualTo(1);

        verify(commonCodeRepository).save(any(CommonCode.class));
    }


    @Test
    void updateCommonCode_returnCommonCodeResponse(){
        CodeGroup codeGroup = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        ReflectionTestUtils.setField(codeGroup, "id", 1);
        CommonCode code = CommonCode.createCommonCode(codeGroup, "pdu", "pdu", 1);
        ReflectionTestUtils.setField(code, "id", 1);

        given(commonCodeRepository.findById(1)).willReturn(Optional.of(code));
        given(codeGroupRepository.findById(1)).willReturn(Optional.of(codeGroup));

        given(commonCodeRepository.existsByCodeGroupIdAndCodeAndIdNot(1, "ups", 1)).willReturn(false);
        given(commonCodeRepository.save(any(CommonCode.class))).willAnswer(invocation -> invocation.getArgument(0));

         CommonCodeRequest updateRequest = new CommonCodeRequest(1, "ups", "ups", 1);
         CommonCodeResponse updateCode = commonCodeQueryService.updateCommonCode(1, updateRequest);

        assertThat(updateCode.code()).isEqualTo("ups");
        assertThat(updateCode.name()).isEqualTo("ups");
        assertThat(updateCode.groupId()).isEqualTo(1);
        assertThat(updateCode.sortOrder()).isEqualTo(1);

        verify(commonCodeRepository).save(any(CommonCode.class));
    }

    @Test
    void getCommonCodeList_withoutCodeGroupId_returnAll() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        ReflectionTestUtils.setField(codeGroup, "id", 1);
        CommonCode pdu = CommonCode.createCommonCode(codeGroup, "pdu", "pdu", 1);
        CommonCode ups = CommonCode.createCommonCode(codeGroup, "ups", "ups", 2);

        given(commonCodeRepository.findAll()).willReturn(List.of(pdu, ups));

        List<CommonCodeResponse> result = commonCodeQueryService.getCommonCodeList(null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CommonCodeResponse::code).containsExactly("pdu", "ups");
        verify(commonCodeRepository).findAll();
        verifyNoInteractions(codeGroupRepository);
    }

    @Test
    void getCommonCodeList_withCodeGroupId_returnFilteredList() {
        CodeGroup codeGroup = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        ReflectionTestUtils.setField(codeGroup, "id", 1);
        CommonCode pdu = CommonCode.createCommonCode(codeGroup, "pdu", "pdu", 1);

        given(codeGroupRepository.findById(1)).willReturn(Optional.of(codeGroup));
        given(commonCodeRepository.findByCodeGroupId(1)).willReturn(List.of(pdu));

        List<CommonCodeResponse> result = commonCodeQueryService.getCommonCodeList(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("pdu");
        assertThat(result.get(0).groupId()).isEqualTo(1);
        verify(codeGroupRepository).findById(1);
        verify(commonCodeRepository).findByCodeGroupId(1);
    }

    @Test
    void getCommonCodeList_withUnknownCodeGroupId_throwEntityNotFoundException() {
        given(codeGroupRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commonCodeQueryService.getCommonCodeList(999))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("CodeGroup not found: 999");

        verifyNoInteractions(commonCodeRepository);
    }
}
