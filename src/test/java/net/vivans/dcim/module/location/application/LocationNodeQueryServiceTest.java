package net.vivans.dcim.module.location.application;

import jakarta.persistence.EntityNotFoundException;
import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
import net.vivans.dcim.module.device.domain.model.Device;
import net.vivans.dcim.module.device.domain.repository.DeviceRepository;
import net.vivans.dcim.module.location.api.dto.LocationNodeBulkCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeDeleteResponse;
import net.vivans.dcim.module.location.api.dto.LocationNodeParentUpdateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeResponse;
import net.vivans.dcim.module.location.api.dto.LocationNodeTreeCreateRequest;
import net.vivans.dcim.module.location.api.dto.LocationNodeUpdateRequest;
import net.vivans.dcim.module.devicemodel.domain.model.DeviceModel;
import net.vivans.dcim.shared.exception.ConflictException;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.model.LocationNodeCodeGenerator;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationNodeQueryServiceTest {

    private static final CommonCode CONTAINER_TYPE;
    private static final CommonCode ZONE_TYPE;
    private static final CommonCode ROW_TYPE;
    private static final CommonCode MODEL_TYPE;

    static {
        CodeGroup locationGroup = CodeGroup.createCodeGroup("LOCATION_TYPE", "장소 유형");
        CONTAINER_TYPE = CommonCode.createCommonCode(locationGroup, "CONTAINER", "컨테이너", 1);
        ZONE_TYPE = CommonCode.createCommonCode(locationGroup, "ZONE", "존", 2);
        ROW_TYPE = CommonCode.createCommonCode(locationGroup, "ROW", "열", 3);
        ReflectionTestUtils.setField(CONTAINER_TYPE, "id", 1);
        ReflectionTestUtils.setField(ZONE_TYPE, "id", 2);
        ReflectionTestUtils.setField(ROW_TYPE, "id", 3);

        CodeGroup modelGroup = CodeGroup.createCodeGroup("MODEL_TYPE", "Model Type");
        MODEL_TYPE = CommonCode.createCommonCode(modelGroup, "PDU", "PDU", 1);
        ReflectionTestUtils.setField(MODEL_TYPE, "id", 99);
    }

    @Mock
    private LocationNodeRepository locationNodeRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private LocationNodeQueryService locationNodeQueryService;

    private LocationNode container;
    private LocationNode row;

    @BeforeEach
    void setUp() {
        container = LocationNode.createRoot("TSTCNTR001", CONTAINER_TYPE, "컨테이너 A");
        row = LocationNode.createChild("TSTROW0001", container, ROW_TYPE, "A열");
        lenient().when(locationNodeRepository.findByParent_Code(anyString())).thenReturn(List.of());
    }

    @Test
    void createLocationNode_root_returnsResponseWithGeneratedCode() {
        when(commonCodeRepository.findById(1)).thenReturn(Optional.of(CONTAINER_TYPE));
        when(locationNodeRepository.existsByParentIsNullAndName("컨테이너 A")).thenReturn(false);
        when(locationNodeRepository.existsByCode(anyString())).thenReturn(false);
        when(locationNodeRepository.save(any(LocationNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationNodeResponse response = locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest(null, 1, "컨테이너 A")
        );

        assertThat(response.code()).hasSize(LocationNodeCodeGenerator.CODE_LENGTH);
        assertThat(response.parentCode()).isNull();
        assertThat(response.locationTypeId()).isEqualTo(1);
        assertThat(response.name()).isEqualTo("컨테이너 A");
        assertThat(response.children()).isEmpty();
        verify(locationNodeRepository).save(any(LocationNode.class));
    }

    @Test
    void createLocationNode_child_returnsResponseWithParentCode() {
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));
        when(locationNodeRepository.existsByParentAndName(container, "A열")).thenReturn(false);
        when(locationNodeRepository.existsByCode(anyString())).thenReturn(false);
        when(locationNodeRepository.save(any(LocationNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationNodeResponse response = locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest("TSTCNTR001", 3, "A열")
        );

        assertThat(response.parentCode()).isEqualTo("TSTCNTR001");
        assertThat(response.locationTypeId()).isEqualTo(3);
        assertThat(response.name()).isEqualTo("A열");
    }

    @Test
    void createLocationNode_throwsWhenParentNotFound() {
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.findByCode("UNKNOWN01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest("UNKNOWN01", 3, "A열")
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("LocationNode not found: UNKNOWN01");

        verify(locationNodeRepository, never()).save(any());
    }

    @Test
    void createLocationNode_throwsWhenLocationTypeNotFound() {
        when(commonCodeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest(null, 999, "컨테이너 A")
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CommonCode not found: 999");
    }

    @Test
    void createLocationNode_throwsWhenLocationTypeIsNotLocationTypeGroup() {
        when(commonCodeRepository.findById(99)).thenReturn(Optional.of(MODEL_TYPE));
        when(locationNodeRepository.existsByParentIsNullAndName("컨테이너 A")).thenReturn(false);
        when(locationNodeRepository.existsByCode(anyString())).thenReturn(false);

        assertThatThrownBy(() -> locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest(null, 99, "컨테이너 A")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("locationType must belong to LOCATION_TYPE group");
    }

    @Test
    void createLocationNode_throwsWhenDuplicateRootName() {
        when(commonCodeRepository.findById(1)).thenReturn(Optional.of(CONTAINER_TYPE));
        when(locationNodeRepository.existsByParentIsNullAndName("컨테이너 A")).thenReturn(true);

        assertThatThrownBy(() -> locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest(null, 1, "컨테이너 A")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name already exists under parent");
    }

    @Test
    void updateLocationNode_updatesNameAndType() {
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.existsByParentIsNullAndNameAndCodeNot("컨테이너 B", "TSTCNTR001"))
                .thenReturn(false);
        when(locationNodeRepository.save(container)).thenReturn(container);

        LocationNodeResponse response = locationNodeQueryService.updateLocationNode(
                "TSTCNTR001",
                new LocationNodeUpdateRequest(3, "컨테이너 B")
        );

        assertThat(response.code()).isEqualTo("TSTCNTR001");
        assertThat(response.name()).isEqualTo("컨테이너 B");
        assertThat(response.locationTypeId()).isEqualTo(3);
    }

    @Test
    void updateLocationNode_throwsWhenNodeNotFound() {
        when(locationNodeRepository.findByCode("UNKNOWN01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationNodeQueryService.updateLocationNode(
                "UNKNOWN01",
                new LocationNodeUpdateRequest(1, "이름")
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("LocationNode not found: UNKNOWN01");
    }

    @Test
    void updateParentLocationNode_promotesToRoot() {
        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));
        when(locationNodeRepository.existsByParentIsNullAndNameAndCodeNot("A열", "TSTROW0001"))
                .thenReturn(false);
        when(locationNodeRepository.save(row)).thenReturn(row);

        LocationNodeResponse response = locationNodeQueryService.updateParentLocationNode(
                "TSTROW0001",
                new LocationNodeParentUpdateRequest(null)
        );

        assertThat(response.parentCode()).isNull();
        assertThat(row.getParent()).isNull();
    }

    @Test
    void updateParentLocationNode_changesParent() {
        LocationNode anotherContainer = LocationNode.createRoot("TSTCNTR002", CONTAINER_TYPE, "컨테이너 B");

        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));
        when(locationNodeRepository.findByCode("TSTCNTR002")).thenReturn(Optional.of(anotherContainer));
        when(locationNodeRepository.existsByParentAndNameAndCodeNot(anotherContainer, "A열", "TSTROW0001"))
                .thenReturn(false);
        when(locationNodeRepository.save(row)).thenReturn(row);

        LocationNodeResponse response = locationNodeQueryService.updateParentLocationNode(
                "TSTROW0001",
                new LocationNodeParentUpdateRequest("TSTCNTR002")
        );

        assertThat(response.parentCode()).isEqualTo("TSTCNTR002");
        assertThat(row.getParent()).isEqualTo(anotherContainer);
    }

    @Test
    void updateParentLocationNode_throwsWhenNewParentNotFound() {
        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));
        when(locationNodeRepository.findByCode("UNKNOWN01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationNodeQueryService.updateParentLocationNode(
                "TSTROW0001",
                new LocationNodeParentUpdateRequest("UNKNOWN01")
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("LocationNode not found: UNKNOWN01");
    }

    @Test
    void getLocationNodes_withoutFilter_returnsForest() {
        when(locationNodeRepository.findAll()).thenReturn(List.of(container, row));

        List<LocationNodeResponse> result = locationNodeQueryService.getLocationNodes(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("TSTCNTR001");
        assertThat(result.get(0).children()).hasSize(1);
    }

    @Test
    void getLocationNodes_throwsWhenParentCodeNotFound() {
        when(locationNodeRepository.existsByCode("UNKNOWN01")).thenReturn(false);

        assertThatThrownBy(() -> locationNodeQueryService.getLocationNodes(null, "UNKNOWN01", null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("LocationNode not found: UNKNOWN01");
    }

    @Test
    void getLocationNodes_throwsWhenLocationTypeIdNotFound() {
        when(commonCodeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationNodeQueryService.getLocationNodes(null, null, 999))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("CommonCode not found: 999");
    }

    @Test
    void getLocationNodes_withParentCodeAndUnmatchedType_returnsRootWithMatchingChild() {
        when(locationNodeRepository.existsByCode("TSTCNTR001")).thenReturn(true);
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.findAll()).thenReturn(List.of(container, row));

        List<LocationNodeResponse> result = locationNodeQueryService.getLocationNodes(
                null,
                "TSTCNTR001",
                3
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("TSTCNTR001");
        assertThat(result.get(0).children()).hasSize(1);
        assertThat(result.get(0).children().get(0).code()).isEqualTo("TSTROW0001");
    }

    @Test
    void getLocationNodes_withParentCodeAndNoMatch_returnsEmptyList() {
        when(locationNodeRepository.existsByCode("TSTCNTR001")).thenReturn(true);
        when(locationNodeRepository.findAll()).thenReturn(List.of(container, row));

        List<LocationNodeResponse> result = locationNodeQueryService.getLocationNodes(
                "없는이름",
                "TSTCNTR001",
                null
        );

        assertThat(result).isEmpty();
    }

    @Test
    void getLocationNodes_withName_includesAncestorPath() {
        when(locationNodeRepository.findAll()).thenReturn(List.of(container, row));

        List<LocationNodeResponse> result = locationNodeQueryService.getLocationNodes(
                "A열",
                null,
                null
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("TSTCNTR001");
        assertThat(result.get(0).children().get(0).code()).isEqualTo("TSTROW0001");
    }

    @Test
    void createBatchLocationNodes_registersTreeDepthFirst() {
        when(commonCodeRepository.findById(1)).thenReturn(Optional.of(CONTAINER_TYPE));
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.existsByParentIsNullAndName("컨테이너 A")).thenReturn(false);
        when(locationNodeRepository.existsByCode(anyString())).thenReturn(false);
        when(locationNodeRepository.save(any(LocationNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationNodeTreeCreateRequest rowRequest = new LocationNodeTreeCreateRequest(3, "A열", List.of());
        LocationNodeTreeCreateRequest containerRequest = new LocationNodeTreeCreateRequest(1, "컨테이너 A", List.of(rowRequest)
        );
        LocationNodeBulkCreateRequest bulkRequest = new LocationNodeBulkCreateRequest(null, List.of(containerRequest));

        List<LocationNodeResponse> result = locationNodeQueryService.createBatchLocationNodes(bulkRequest);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("컨테이너 A");
        assertThat(result.get(0).parentCode()).isNull();
        assertThat(result.get(0).children()).hasSize(1);
        assertThat(result.get(0).children().get(0).name()).isEqualTo("A열");
        assertThat(result.get(0).children().get(0).parentCode()).isEqualTo(result.get(0).code());
    }

    @Test
    void createBatchLocationNodes_underExistingParent() {
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.existsByParentAndName(container, "A열")).thenReturn(false);
        when(locationNodeRepository.existsByCode(anyString())).thenReturn(false);
        when(locationNodeRepository.save(any(LocationNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationNodeTreeCreateRequest rowRequest = new LocationNodeTreeCreateRequest(3, "A열", List.of());
        LocationNodeBulkCreateRequest bulkRequest = new LocationNodeBulkCreateRequest(
                "TSTCNTR001",
                List.of(rowRequest)
        );

        List<LocationNodeResponse> result = locationNodeQueryService.createBatchLocationNodes(bulkRequest);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).parentCode()).isEqualTo("TSTCNTR001");
        assertThat(result.get(0).name()).isEqualTo("A열");
    }

    @Test
    void createBatchLocationNodes_throwsWhenDuplicateSiblingNameInRequest() {
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));

        LocationNodeTreeCreateRequest first = new LocationNodeTreeCreateRequest(3, "A열", List.of());
        LocationNodeTreeCreateRequest second = new LocationNodeTreeCreateRequest(3, "A열", List.of());
        LocationNodeBulkCreateRequest bulkRequest = new LocationNodeBulkCreateRequest(
                "TSTCNTR001",
                List.of(first, second)
        );

        assertThatThrownBy(() -> locationNodeQueryService.createBatchLocationNodes(bulkRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name already exists under parent");
    }

    @Test
    void deleteLocationNode_deletesLeafNode() {
        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));
        when(locationNodeRepository.existsByParent_Code("TSTROW0001")).thenReturn(false);
        when(deviceRepository.findByLocationNodeCodeIn(any())).thenReturn(List.of());

        LocationNodeDeleteResponse response = locationNodeQueryService.deleteLocationNode("TSTROW0001");

        assertThat(response.deletedCode()).isEqualTo("TSTROW0001");
        assertThat(response.reassignedDeviceCount()).isZero();
        verify(locationNodeRepository).delete(row);
    }

    @Test
    void deleteLocationNode_reassignsDevicesToUnassigned() {
        LocationNode unassigned = LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, CONTAINER_TYPE, "미배정");
        DeviceModel model = DeviceModel.create("AP8959", "APC", MODEL_TYPE, null);
        Device device = Device.create(model, row, "PDU-좌", null);

        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));
        when(locationNodeRepository.existsByParent_Code("TSTROW0001")).thenReturn(false);
        when(deviceRepository.findByLocationNodeCodeIn(any())).thenReturn(List.of(device));
        when(locationNodeRepository.findByCode(Device.UNASSIGNED_LOCATION_CODE)).thenReturn(Optional.of(unassigned));
        when(deviceRepository.findByLocationNodeCode(Device.UNASSIGNED_LOCATION_CODE)).thenReturn(List.of());

        LocationNodeDeleteResponse response = locationNodeQueryService.deleteLocationNode("TSTROW0001");

        assertThat(response.reassignedDeviceCount()).isEqualTo(1);
        assertThat(device.getLocationNode()).isEqualTo(unassigned);
        verify(deviceRepository).saveAll(List.of(device));
        verify(locationNodeRepository).delete(row);
    }

    @Test
    void deleteLocationNode_throwsWhenDeviceNameConflictsAtUnassigned() {
        LocationNode unassigned = LocationNode.createRoot(Device.UNASSIGNED_LOCATION_CODE, CONTAINER_TYPE, "미배정");
        DeviceModel model = DeviceModel.create("AP8959", "APC", MODEL_TYPE, null);
        Device device = Device.create(model, row, "PDU-좌", null);
        Device existing = Device.create(model, unassigned, "PDU-좌", null);

        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));
        when(locationNodeRepository.existsByParent_Code("TSTROW0001")).thenReturn(false);
        when(deviceRepository.findByLocationNodeCodeIn(any())).thenReturn(List.of(device));
        when(locationNodeRepository.findByCode(Device.UNASSIGNED_LOCATION_CODE)).thenReturn(Optional.of(unassigned));
        when(deviceRepository.findByLocationNodeCode(Device.UNASSIGNED_LOCATION_CODE)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> locationNodeQueryService.deleteLocationNode("TSTROW0001"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("device name conflict at UNASSIGNED; rename devices before deleting location");

        verify(locationNodeRepository, never()).delete(row);
    }

    @Test
    void deleteLocationNode_throwsWhenDeletingSystemNode() {
        assertThatThrownBy(() -> locationNodeQueryService.deleteLocationNode(Device.UNASSIGNED_LOCATION_CODE))
                .isInstanceOf(ConflictException.class)
                .hasMessage("cannot delete system location node");

        verify(locationNodeRepository, never()).delete(any());
    }

    @Test
    void deleteLocationNode_throwsWhenNodeHasChildren() {
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));
        when(locationNodeRepository.existsByParent_Code("TSTCNTR001")).thenReturn(true);

        assertThatThrownBy(() -> locationNodeQueryService.deleteLocationNode("TSTCNTR001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cannot delete node with children");

        verify(locationNodeRepository, never()).delete(container);
    }

    @Test
    void deleteLocationNodeSubtree_deletesDeepestFirst() {
        when(locationNodeRepository.existsByCode("TSTCNTR001")).thenReturn(true);
        when(locationNodeRepository.findAll()).thenReturn(List.of(container, row));
        when(deviceRepository.findByLocationNodeCodeIn(any())).thenReturn(List.of());

        LocationNodeDeleteResponse response = locationNodeQueryService.deleteLocationNodeSubtree("TSTCNTR001");

        assertThat(response.deletedCode()).isEqualTo("TSTCNTR001");
        assertThat(response.reassignedDeviceCount()).isZero();
        verify(locationNodeRepository).deleteAll(List.of(row, container));
    }

    @Test
    void createLocationNode_child_throwsWhenLocationTypeOrderInvalid() {
        when(commonCodeRepository.findById(1)).thenReturn(Optional.of(CONTAINER_TYPE));
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));

        assertThatThrownBy(() -> locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest("TSTCNTR001", 1, "중복 컨테이너")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("child location type must be deeper than parent");

        verify(locationNodeRepository, never()).save(any());
    }

    @Test
    void createLocationNode_child_reparentsExistingChildrenWhenIntermediateTypeInserted() {
        when(commonCodeRepository.findById(2)).thenReturn(Optional.of(ZONE_TYPE));
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));
        when(locationNodeRepository.existsByParentAndName(container, "존 1")).thenReturn(false);
        when(locationNodeRepository.existsByCode(anyString())).thenReturn(false);
        when(locationNodeRepository.findByParent_Code("TSTCNTR001")).thenReturn(List.of(row));
        when(locationNodeRepository.save(any(LocationNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationNodeResponse response = locationNodeQueryService.createLocationNode(
                new LocationNodeCreateRequest("TSTCNTR001", 2, "존 1")
        );

        assertThat(response.name()).isEqualTo("존 1");
        assertThat(response.locationTypeId()).isEqualTo(2);
        assertThat(row.getParent().getCode()).isEqualTo(response.code());
        verify(locationNodeRepository).save(row);
    }

    @Test
    void updateLocationNode_throwsWhenNewTypeBreaksChildOrder() {
        when(locationNodeRepository.findByCode("TSTCNTR001")).thenReturn(Optional.of(container));
        when(commonCodeRepository.findById(3)).thenReturn(Optional.of(ROW_TYPE));
        when(locationNodeRepository.findByParent_Code("TSTCNTR001")).thenReturn(List.of(row));
        when(locationNodeRepository.existsByParentIsNullAndNameAndCodeNot("컨테이너 B", "TSTCNTR001"))
                .thenReturn(false);

        assertThatThrownBy(() -> locationNodeQueryService.updateLocationNode(
                "TSTCNTR001",
                new LocationNodeUpdateRequest(3, "컨테이너 B")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("child location type must be deeper than parent");

        verify(locationNodeRepository, never()).save(container);
    }

    @Test
    void updateParentLocationNode_throwsWhenParentTypeOrderInvalid() {
        LocationNode zone = LocationNode.createChild("TSTZONE001", container, ZONE_TYPE, "존 1");

        when(locationNodeRepository.findByCode("TSTZONE001")).thenReturn(Optional.of(zone));
        when(locationNodeRepository.findByCode("TSTROW0001")).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> locationNodeQueryService.updateParentLocationNode(
                "TSTZONE001",
                new LocationNodeParentUpdateRequest("TSTROW0001")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("child location type must be deeper than parent");

        verify(locationNodeRepository, never()).save(zone);
    }
}
