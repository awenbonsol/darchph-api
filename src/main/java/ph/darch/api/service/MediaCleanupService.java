package ph.darch.api.service;

import ph.darch.api.entity.MediaAsset;

import java.util.Collection;

/**
 * Hook for removing media assets that are no longer referenced by any product.
 * The current no-op implementation (TASK_5) is replaced by a real implementation
 * in TASK_6 that also deletes the backing Supabase Storage objects.
 */
public interface MediaCleanupService {

    void deleteAssets(Collection<MediaAsset> assets);
}
