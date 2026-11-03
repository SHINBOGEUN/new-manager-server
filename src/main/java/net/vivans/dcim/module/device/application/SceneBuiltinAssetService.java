package net.vivans.dcim.module.device.application;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.device.domain.model.SceneBuiltinAsset;
import net.vivans.dcim.module.device.infrastructure.persistence.SceneBuiltinAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneBuiltinAssetService {
    private final SceneBuiltinAssetRepository repository;

    public record AssetResponse(String key, String label, String category, boolean enabled) {
        static AssetResponse from(SceneBuiltinAsset asset) {
            return new AssetResponse(asset.getKey(), asset.getLabel(), asset.getCategory(), asset.isEnabled());
        }
    }

    public List<AssetResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(SceneBuiltinAsset::getCategory).thenComparing(SceneBuiltinAsset::getLabel))
                .map(AssetResponse::from).toList();
    }

    @Transactional
    public AssetResponse update(String key, String label, Boolean enabled) {
        if (enabled == null) throw new IllegalArgumentException("사용 여부를 지정하세요.");
        SceneBuiltinAsset asset = repository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("기본 3D 자산이 없습니다: " + key));
        asset.update(label, enabled);
        repository.flush();
        return AssetResponse.from(asset);
    }
}
