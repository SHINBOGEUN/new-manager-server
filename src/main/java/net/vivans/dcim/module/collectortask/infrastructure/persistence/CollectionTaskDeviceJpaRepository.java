package net.vivans.dcim.module.collectortask.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import net.vivans.dcim.module.collectortask.domain.repository.CollectionTaskDeviceRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectionTaskDeviceJpaRepository implements CollectionTaskDeviceRepository {

    private final CollectionTaskDeviceSpringDataRepository springDataRepository;

    @Override
    public void deleteByDeviceId(Integer deviceId) {
        springDataRepository.deleteByDeviceId(deviceId);
    }
}
