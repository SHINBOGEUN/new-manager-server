package net.vivans.dcim.module.common.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CodeGroupRepository;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DATA_POINT_TYPE 부트스트랩이 이미 존재하는 코드(예: 기존에 등록된 HUMIDITY/UNCLASSIFIED)를
 * 중복 INSERT하지 않는지 확인한다. LoRa 작업에서 HUMIDITY/UNCLASSIFIED를 codes 배열에 추가하기 전에도
 * 이미 존재 여부를 findByCodeGroupGroupKeyAndCode로 확인한 뒤에만 save하는 구조였으므로,
 * 이 테스트는 그 idempotent 동작을 명시적으로 고정한다.
 */
class DataPointTypeBootstrapTest {

    private final CodeGroupRepository codeGroupRepository = mock(CodeGroupRepository.class);
    private final CommonCodeRepository commonCodeRepository = mock(CommonCodeRepository.class);
    private final DataPointTypeBootstrap bootstrap =
            new DataPointTypeBootstrap(codeGroupRepository, commonCodeRepository);

    private static final List<String> ALL_CODES = List.of(
            "POWER", "ENERGY", "CURRENT", "VOLTAGE", "TEMPERATURE",
            "PRESSURE", "FLOW", "POWER_FACTOR", "HUMIDITY", "UNCLASSIFIED");

    @Test
    void run_doesNotInsertAnyCodeWhenGroupAndAllCodesAlreadyExist() {
        CodeGroup existingGroup = CodeGroup.createCodeGroup("DATA_POINT_TYPE", "Data Point Type");
        when(codeGroupRepository.findAll()).thenReturn(List.of(existingGroup));
        for (String code : ALL_CODES) {
            when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DATA_POINT_TYPE", code))
                    .thenReturn(Optional.of(mock(CommonCode.class)));
        }

        bootstrap.run();

        verify(codeGroupRepository, never()).save(any());
        verify(commonCodeRepository, never()).save(any());
    }

    @Test
    void run_insertsOnlyMissingCodesWhenSomeAlreadyExist() {
        CodeGroup existingGroup = CodeGroup.createCodeGroup("DATA_POINT_TYPE", "Data Point Type");
        when(codeGroupRepository.findAll()).thenReturn(List.of(existingGroup));
        for (String code : ALL_CODES) {
            boolean alreadyExists = !code.equals("HUMIDITY") && !code.equals("UNCLASSIFIED");
            when(commonCodeRepository.findByCodeGroupGroupKeyAndCode("DATA_POINT_TYPE", code))
                    .thenReturn(alreadyExists ? Optional.of(mock(CommonCode.class)) : Optional.empty());
        }
        when(commonCodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap.run();

        verify(codeGroupRepository, never()).save(any());
        ArgumentCaptor<CommonCode> captor = ArgumentCaptor.forClass(CommonCode.class);
        verify(commonCodeRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(CommonCode::getCode)
                .containsExactlyInAnyOrder("HUMIDITY", "UNCLASSIFIED");
    }

    @Test
    void run_createsCodeGroupOnlyWhenItDoesNotExistYet() {
        when(codeGroupRepository.findAll()).thenReturn(List.of());
        CodeGroup created = CodeGroup.createCodeGroup("DATA_POINT_TYPE", "Data Point Type");
        when(codeGroupRepository.save(any())).thenReturn(created);
        when(commonCodeRepository.findByCodeGroupGroupKeyAndCode(eq("DATA_POINT_TYPE"), anyString()))
                .thenReturn(Optional.empty());
        when(commonCodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap.run();

        verify(codeGroupRepository, times(1)).save(any());
        verify(commonCodeRepository, times(ALL_CODES.size())).save(any());
    }
}
