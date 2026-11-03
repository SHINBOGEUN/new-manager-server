package net.vivans.dcim.module.device.application;

import net.vivans.dcim.module.common.domain.model.CodeGroup;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.device.api.dto.DeviceRackPlacementRequest;
import net.vivans.dcim.module.device.domain.model.*;
import net.vivans.dcim.module.device.domain.repository.*;
import net.vivans.dcim.module.device.infrastructure.persistence.*;
import net.vivans.dcim.module.location.domain.model.LocationNode;
import net.vivans.dcim.module.location.domain.repository.LocationNodeRepository;
import net.vivans.dcim.module.location.infrastructure.persistence.LocationNodeSpringDataRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RackModelPlacementServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final DeviceAssetSpringDataRepository assets = mock(DeviceAssetSpringDataRepository.class);
    private final DeviceRackPlacementRepository placements = mock(DeviceRackPlacementRepository.class);
    private final DeviceRackPlacementHistoryRepository histories = mock(DeviceRackPlacementHistoryRepository.class);
    private final DeviceImageRepository images = mock(DeviceImageRepository.class);
    private final LocationNodeRepository locations = mock(LocationNodeRepository.class);
    private final SceneModelRepository models = mock(SceneModelRepository.class);
    private final RackModelPlacementRepository modelPlacements = mock(RackModelPlacementRepository.class);
    private final LocationNodeSpringDataRepository locks = mock(LocationNodeSpringDataRepository.class);
    private final DeviceAssetRackService service = new DeviceAssetRackService(devices, assets, placements, histories, images, locations, models, modelPlacements, locks);
    private static final String RACK = "0000000001";

    private LocationNode rack() {
        var type = CommonCode.createCommonCode(CodeGroup.createCodeGroup("LOCATION_TYPE", "위치"), "RACK", "랙", 1);
        return LocationNode.createRoot(RACK, type, "랙", 42);
    }
    private DeviceRackPlacementRequest request(int start, int height) {
        return new DeviceRackPlacementRequest(DeviceMountType.RACK_U, RACK, null, start, height, null, null, null, null, false, "MODEL", true);
    }
    private void prepare() {
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack()));
        SceneModel model = SceneModel.uploaded("MODEL", "서버", "랙", "x.glb", "x.glb", 1, 1, 1);
        model.setRackInstallable(true);
        when(models.findById("MODEL")).thenReturn(Optional.of(model));
    }
    @Test void modelCannotOccupyAnExistingDeviceOrModel() {
        prepare();
        when(placements.existsOverlapping(RACK, 5, 8, null)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.createModelPlacement(RACK, request(5, 4)));
        verify(modelPlacements, never()).save(any());
        when(placements.existsOverlapping(RACK, 5, 8, null)).thenReturn(false);
        when(modelPlacements.overlaps(RACK, 5, 8)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.createModelPlacement(RACK, request(5, 4)));
        verify(modelPlacements, never()).save(any());
    }
    @Test void modelCannotExceedRackCapacity() {
        prepare();
        assertThrows(IllegalArgumentException.class, () -> service.createModelPlacement(RACK, request(40, 4)));
        verify(modelPlacements, never()).save(any());
    }
    @Test void modelNotAllowedForRackCannotBeInstalled() {
        prepare();
        var model = SceneModel.uploaded("MODEL", "설비", "기타", "x.glb", "x.glb", 1, 1, 1);
        when(models.findById("MODEL")).thenReturn(Optional.of(model));
        assertThrows(IllegalArgumentException.class, () -> service.createModelPlacement(RACK, request(1, 4)));
        verify(modelPlacements, never()).save(any());
    }
    @Test void samePduModelCanBeInstalledOnBothSidesAndInAUnitSlot() {
        prepare();
        when(locations.findByCode(RACK)).thenReturn(Optional.of(rack()));
        for (DeviceRackSide side : new DeviceRackSide[]{DeviceRackSide.LEFT, DeviceRackSide.RIGHT}) {
            service.createModelPlacement(RACK, new DeviceRackPlacementRequest(DeviceMountType.RACK_SIDE, RACK, side,
                    null, null, null, null, null, null, false, "MODEL", true));
        }
        service.createModelPlacement(RACK, request(1, 4));
        var capture = org.mockito.ArgumentCaptor.forClass(RackModelPlacement.class);
        verify(modelPlacements, times(3)).save(capture.capture());
        assertEquals(DeviceRackSide.LEFT, capture.getAllValues().get(0).getRackSide());
        assertEquals(DeviceRackSide.RIGHT, capture.getAllValues().get(1).getRackSide());
        assertEquals(DeviceMountType.RACK_U, capture.getAllValues().get(2).getMountType());
    }
    @Test void realDeviceCannotOverlapAnUnlinkedModel() {
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack()));
        when(devices.findById(10)).thenReturn(Optional.of(mock(Device.class)));
        when(modelPlacements.overlaps(RACK, 5, 8)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.saveRackPlacement(10, request(5, 4)));
        verify(placements, never()).save(any());
    }
    @Test void alreadyPlacedDeviceCannotBeBoundWithoutExplicitRemoval() {
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack()));
        when(modelPlacements.findById(7)).thenReturn(Optional.of(RackModelPlacement.create(RACK, "MODEL", DeviceMountType.RACK_U, null, 1, 4, null, null, null)));
        when(placements.findByDeviceId(10)).thenReturn(Optional.of(mock(DeviceRackPlacement.class)));
        assertThrows(IllegalArgumentException.class, () -> service.bindModelPlacement(RACK, 7, 10));
        verify(modelPlacements, never()).delete(any());
    }
    @Test void bindingConvertsTheModelToADevicePlacementWithoutChangingItsUOrModel() {
        LocationNode rack = rack();
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack));
        var model = RackModelPlacement.create(RACK, "MODEL", DeviceMountType.RACK_U, null, 5, 4, null, null, null);
        when(modelPlacements.findById(7)).thenReturn(Optional.of(model));
        when(placements.findByDeviceId(10)).thenReturn(Optional.empty());
        when(devices.findById(10)).thenReturn(Optional.of(mock(Device.class)));
        SceneModel rackModel = SceneModel.uploaded("MODEL", "서버", "랙", "x.glb", "x.glb", 1, 1, 1);
        rackModel.setRackInstallable(true);
        when(models.findById("MODEL")).thenReturn(Optional.of(rackModel));
        when(models.existsById("MODEL")).thenReturn(true);
        when(placements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(locations.findByCode(RACK)).thenReturn(Optional.of(rack));
        service.bindModelPlacement(RACK, 7, 10);
        var capture = org.mockito.ArgumentCaptor.forClass(DeviceRackPlacement.class);
        verify(placements).save(capture.capture());
        assertEquals(5, capture.getValue().getUPosition());
        assertEquals(4, capture.getValue().getUHeight());
        assertEquals("MODEL", capture.getValue().getSceneModelKey());
        var order = inOrder(modelPlacements, placements);
        order.verify(modelPlacements).delete(model);
        order.verify(modelPlacements).flush();
        order.verify(placements).save(any());
    }
    @Test void replacingTheModelKeepsTheExistingDeviceAndRackPosition() {
        prepare();
        var rack = rack(); var device = mock(Device.class);
        var item = DeviceRackPlacement.create(device, rack, DeviceMountType.RACK_U, null, 5, 4, "4U");
        item.setSceneModelKey("OLD");
        when(placements.findByDeviceId(10)).thenReturn(Optional.of(item));
        when(locations.findByCode(RACK)).thenReturn(Optional.of(rack));
        service.replacePlacementModel(RACK, 10, "MODEL");
        assertSame(device, item.getDevice());
        assertEquals(5, item.getUPosition());
        assertEquals(4, item.getUHeight());
        assertEquals("MODEL", item.getSceneModelKey());
        verify(placements).save(item);
    }
    @Test void replacingAnUnlinkedModelPreservesItsSideAndCoordinates() {
        prepare();
        var rack = rack();
        var item = RackModelPlacement.create(RACK, "OLD", DeviceMountType.RACK_SIDE, DeviceRackSide.RIGHT, null, null, .9, .75, .5);
        when(modelPlacements.findById(7)).thenReturn(Optional.of(item));
        when(locations.findByCode(RACK)).thenReturn(Optional.of(rack));
        service.replaceUnlinkedModel(RACK, 7, "MODEL");
        assertEquals("MODEL", item.getSceneModelKey());
        assertEquals(DeviceRackSide.RIGHT, item.getRackSide());
        assertEquals(.75, item.getPositionY());
        verify(modelPlacements).save(item);
    }
    @Test void replacementRejectsAModelThatIsNotAllowedForRack() {
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack()));
        var model = SceneModel.uploaded("MODEL", "설비", "기타", "x.glb", "x.glb", 1, 1, 1);
        when(models.findById("MODEL")).thenReturn(Optional.of(model));
        assertThrows(IllegalArgumentException.class, () -> service.replacePlacementModel(RACK, 10, "MODEL"));
        verify(placements, never()).save(any());
    }
    @Test void modelUResizeChecksCapacityAndConflictingDevices() {
        var rack = rack(); when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack));
        var item = RackModelPlacement.create(RACK, "MODEL", DeviceMountType.RACK_U, null, 5, 4, null, null, null);
        when(modelPlacements.findById(7)).thenReturn(Optional.of(item));
        when(locations.findByCode(RACK)).thenReturn(Optional.of(rack));
        service.moveModelPlacement(RACK, 7, request(8, 5));
        assertEquals(8, item.getUPosition()); assertEquals(5, item.getUHeight());
        when(placements.existsOverlapping(RACK, 10, 15, null)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.moveModelPlacement(RACK, 7, request(10, 6)));
        assertThrows(IllegalArgumentException.class, () -> service.moveModelPlacement(RACK, 7, request(40, 5)));
        assertEquals(8, item.getUPosition()); assertEquals(5, item.getUHeight());
    }
    @Test void replacingConnectedDevicePreservesPlacementAndHistory() {
        var rack = rack(); var oldDevice = mock(Device.class); var newDevice = mock(Device.class);
        var item = DeviceRackPlacement.create(oldDevice, rack, DeviceMountType.RACK_SIDE, DeviceRackSide.RIGHT, null, null, null);
        item.setSceneModelKey("SR_PDU_VERTICAL"); item.setVisualizationPosition(.97, .6, .8); item.setVisualLengthRatio(.7);
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack));
        when(placements.findByDeviceId(10)).thenReturn(Optional.of(item));
        when(placements.findByDeviceId(20)).thenReturn(Optional.empty());
        when(devices.findById(20)).thenReturn(Optional.of(newDevice));
        when(locations.findByCode(RACK)).thenReturn(Optional.of(rack));
        service.replacePlacementDevice(RACK, 10, 20);
        assertSame(newDevice, item.getDevice()); assertEquals(DeviceRackSide.RIGHT, item.getRackSide());
        assertEquals("SR_PDU_VERTICAL", item.getSceneModelKey()); assertEquals(.6, item.getPositionY()); assertEquals(.7, item.getVisualLengthRatio());
        verify(histories, times(2)).save(any()); verify(newDevice).reassignLocation(rack);
    }
    @Test void replacingConnectedDeviceRejectsDeviceAlreadyPlacedElsewhere() {
        var rack = rack(); var item = DeviceRackPlacement.create(mock(Device.class), rack, DeviceMountType.RACK_U, null, 5, 4, null);
        when(locks.lockByCode(RACK)).thenReturn(Optional.of(rack));
        when(placements.findByDeviceId(10)).thenReturn(Optional.of(item));
        when(placements.findByDeviceId(20)).thenReturn(Optional.of(mock(DeviceRackPlacement.class)));
        assertThrows(IllegalArgumentException.class, () -> service.replacePlacementDevice(RACK, 10, 20));
        verify(placements, never()).save(any()); verify(histories, never()).save(any());
    }
}
