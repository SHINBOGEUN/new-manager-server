package net.vivans.dcim.module.common.application;

import net.vivans.dcim.module.common.api.dto.CodeGroupRequest;
import net.vivans.dcim.module.common.api.dto.CodeGroupResponse;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CodeGroupQueryServiceTest {

    @Mock
    private CodeGroupRepository codeGroupRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    @InjectMocks
    private CodeGroupQueryService codeGroupQueryService;

    @Test
    void createCodeGroup_returnsResponse() {
        // given
        CodeGroupRequest request = new CodeGroupRequest("DEVICE_TYPE", "장비 유형");

        // when
        CodeGroupResponse response = codeGroupQueryService.createCodeGroup(request);

        // then
        assertThat(response.groupKey()).isEqualTo(request.groupKey());
        assertThat(response.groupName()).isEqualTo(request.groupName());
        verify(codeGroupRepository).save(any(CodeGroup.class));
    }

    @Test
    void updateCodeGroup_returnsResponse() {
        // given
        CodeGroup existing = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        //ReflectionTestUtils를 사용해서 비공개 id 값을 할당
        ReflectionTestUtils.setField(existing, "id", 1);

        //willReturn을 사용해 정의한 메서드가 리턴한 값을 지정
        given(codeGroupRepository.findById(1)).willReturn(Optional.of(existing));
        given(codeGroupRepository.existsByGroupKeyAndIdNot("MODEL_TYPE", 1)).willReturn(false);
        given(codeGroupRepository.existsByGroupNameAndIdNot("모델 유형", 1)).willReturn(false);
        /***
         * willAnswer(...)	고정값이 아니라 직접 계산해서 반환
         * getArgument(0)	save()에 넣은 첫 번째 인자
         */
        given(codeGroupRepository.save(any(CodeGroup.class))).willAnswer(invocation -> invocation.getArgument(0));

        CodeGroupRequest updateRequest = new CodeGroupRequest("MODEL_TYPE", "모델 유형");

        // when
        CodeGroupResponse response = codeGroupQueryService.updateCodeGroup(1, updateRequest);

        // then
        assertThat(response.groupKey()).isEqualTo("MODEL_TYPE");
        assertThat(response.groupName()).isEqualTo("모델 유형");
        verify(codeGroupRepository).save(any(CodeGroup.class));
    }

    @Test
    void findAll_returnsEmptyList() {
        // given

        // when
        List<CodeGroupResponse> responses = codeGroupQueryService.findAll();

        // then
        assertThat(responses).isEmpty();
    }

    @Test
    void findAll_returnsMappedResponses() {
        // given
        CodeGroup deviceType = CodeGroup.createCodeGroup("DEVICE_TYPE", "장비 유형");
        CodeGroup sensorType = CodeGroup.createCodeGroup("SENSOR_TYPE", "센서 유형");
        ReflectionTestUtils.setField(deviceType, "id", 1);
        ReflectionTestUtils.setField(sensorType, "id", 2);

        given(codeGroupRepository.findAll()).willReturn(List.of(deviceType, sensorType));

        // when
        List<CodeGroupResponse> responses = codeGroupQueryService.findAll();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(1);
        assertThat(responses.get(0).groupKey()).isEqualTo("DEVICE_TYPE");
        assertThat(responses.get(0).groupName()).isEqualTo("장비 유형");
        assertThat(responses.get(1).id()).isEqualTo(2);
        assertThat(responses.get(1).groupKey()).isEqualTo("SENSOR_TYPE");
        assertThat(responses.get(1).groupName()).isEqualTo("센서 유형");
    }
}
