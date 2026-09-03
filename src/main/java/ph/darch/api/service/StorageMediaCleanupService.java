package ph.darch.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.entity.MediaAsset;
import ph.darch.api.repository.MediaAssetRepository;
import ph.darch.api.repository.ProductMediaRepository;

import java.util.Collection;

/**
 * Real implementation of {@link MediaCleanupService}. For each asset supplied it
 * removes it only if it is no longer referenced by any {@code product_media} row
 * (an asset may be shared by several products), deletes the backing Supabase
 * Storage object (best-effort, failures are logged), then removes the
 * {@code media_assets} row.
 */
@Service
public class StorageMediaCleanupService implements MediaCleanupService {

    private static final Logger log = LoggerFactory.getLogger(StorageMediaCleanupService.class);

    private final MediaAssetRepository mediaAssetRepository;
    private final ProductMediaRepository productMediaRepository;
    private final StorageService storageService;

    public StorageMediaCleanupService(MediaAssetRepository mediaAssetRepository,
                                      ProductMediaRepository productMediaRepository,
                                      StorageService storageService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.productMediaRepository = productMediaRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public void deleteAssets(Collection<MediaAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return;
        }
        for (MediaAsset asset : assets) {
            boolean stillReferenced = productMediaRepository.existsByMediaAssetId(asset.getId());
            if (stillReferenced) {
                continue;
            }
            try {
                storageService.delete(asset.getBucket(), asset.getObjectPath());
            } catch (RuntimeException e) {
                log.warn("Failed to delete Storage object bucket={} key={}: {}",
                        asset.getBucket(), asset.getObjectPath(), e.getMessage());
            }
            mediaAssetRepository.delete(asset);
        }
    }
}
