package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.SceneModel;
import net.vivans.dcim.module.device.infrastructure.persistence.DevicePageSceneItemRepository;
import net.vivans.dcim.module.device.infrastructure.persistence.SceneModelRepository;
import net.vivans.dcim.module.device.infrastructure.storage.SceneModelFileStorage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneModelService {
    private final SceneModelRepository repository;
    private final DevicePageSceneItemRepository sceneItemRepository;
    private final SceneModelFileStorage storage;
    private final net.vivans.dcim.module.device.infrastructure.persistence.DeviceRackPlacementSpringDataRepository rackPlacements;
    private final net.vivans.dcim.module.device.infrastructure.persistence.RackModelPlacementRepository modelPlacements;

    public record ModelResponse(String key, String label, String category, String file, double width, double height,
                                double depth, boolean enabled, boolean builtin, String url, boolean inUse, boolean rackInstallable, boolean rackUInstallable, boolean rackAttachmentInstallable, boolean deviceLinkable) {
        static ModelResponse from(SceneModel model, boolean inUse) {
            return new ModelResponse(model.getKey(), model.getName(), model.getCategory(), model.getFileName(),
                    model.getWidth(), model.getHeight(), model.getDepth(), model.isEnabled(),
                    model.isBuiltin(), "/api/manager/scene-models/" + model.getKey() + "/content?v="
                    + (model.getUpdatedDt() == null ? 0 : model.getUpdatedDt().toEpochMilli()), inUse, model.isRackInstallable(), model.isRackUInstallable(), model.isRackAttachmentInstallable(), model.isDeviceLinkable());
        }
    }

    public List<ModelResponse> list() {
        return repository.findAll().stream().map(model -> ModelResponse.from(model,
                sceneItemRepository.existsByModelKey(model.getKey()) || rackPlacements.existsBySceneModelKey(model.getKey()) || modelPlacements.existsBySceneModelKey(model.getKey()))).toList();
    }

    public boolean usable(String key) {
        return repository.existsById(key);
    }

    @Transactional
    public ModelResponse setDeviceLinkable(String key, boolean allowed) {
        SceneModel model = find(key);
        model.setDeviceLinkable(allowed);
        repository.flush();
        return ModelResponse.from(model, sceneItemRepository.existsByModelKey(key) || rackPlacements.existsBySceneModelKey(key) || modelPlacements.existsBySceneModelKey(key));
    }

    @Transactional
    public ModelResponse setRackInstallable(String key, boolean allowed) {
        SceneModel model = find(key);
        model.setRackInstallable(allowed);
        repository.flush();
        return ModelResponse.from(model, sceneItemRepository.existsByModelKey(key) || rackPlacements.existsBySceneModelKey(key) || modelPlacements.existsBySceneModelKey(key));
    }

    @Transactional
    public ModelResponse setRackMountOptions(String key, boolean rackU, boolean attachment) {
        SceneModel model = find(key);
        model.setRackMountOptions(rackU, attachment);
        repository.flush();
        return ModelResponse.from(model, sceneItemRepository.existsByModelKey(key) || rackPlacements.existsBySceneModelKey(key) || modelPlacements.existsBySceneModelKey(key));
    }

    @Transactional
    public ModelResponse create(String name, String category, double width, double height, double depth, MultipartFile file) {
        storage.validate(file);
        String key = "USR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        SceneModel model = SceneModel.uploaded(key, name, category, "pending.glb", safeName(file), width, height, depth);
        String storageKey = storage.store(file);
        model.replaceFile(storageKey, safeName(file));
        try {
            model = repository.saveAndFlush(model);
            final String cleanupKey = storageKey;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) storage.delete(cleanupKey);
                }
            });
            return ModelResponse.from(model, false);
        } catch (RuntimeException error) {
            storage.delete(storageKey);
            throw error;
        }
    }

    @Transactional
    public ModelResponse update(String key, String name, String category, double width, double height, double depth,
                                boolean enabled, MultipartFile file) {
        SceneModel model = find(key);
        model.update(name, category == null ? model.getCategory() : category, width, height, depth, enabled);
        if (file != null && !file.isEmpty()) {
            storage.validate(file);
            String oldStorageKey = model.getStorageKey();
            String newStorageKey = storage.store(file);
            model.replaceFile(newStorageKey, safeName(file));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        if (oldStorageKey != null) storage.delete(oldStorageKey);
                    } else storage.delete(newStorageKey);
                }
            });
        }
        repository.flush();
        return ModelResponse.from(model, sceneItemRepository.existsByModelKey(key) || rackPlacements.existsBySceneModelKey(key) || modelPlacements.existsBySceneModelKey(key));
    }

    @Transactional
    public void delete(String key) {
        SceneModel model = find(key);
        if (sceneItemRepository.existsByModelKey(key) || rackPlacements.existsBySceneModelKey(key) || modelPlacements.existsBySceneModelKey(key)) {
            throw new IllegalArgumentException("도면에서 사용 중인 모델은 삭제할 수 없습니다. 먼저 도면에서 제거하세요.");
        }
        String storageKey = model.getStorageKey();
        repository.delete(model);
        repository.flush();
        if (storageKey != null) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { storage.delete(storageKey); }
        });
    }

    public Resource content(String key) {
        SceneModel model = find(key);
        if (model.getStorageKey() != null) return storage.load(model.getStorageKey());
        String file = model.getBuiltinFile();
        if (file == null || !file.matches("[A-Za-z0-9_. -]+\\.glb") || !Path.of(file).getFileName().toString().equals(file)) {
            throw new EntityNotFoundException("3D 모델 파일이 없습니다.");
        }
        Resource resource = new ClassPathResource("static/scene-assets/showroom/" + file);
        if (!resource.exists()) throw new EntityNotFoundException("3D 모델 파일이 없습니다.");
        return resource;
    }

    private SceneModel find(String key) {
        return repository.findById(key).orElseThrow(() -> new EntityNotFoundException("3D 모델이 없습니다: " + key));
    }

    private static String safeName(MultipartFile file) {
        String name = Path.of(file.getOriginalFilename()).getFileName().toString();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
