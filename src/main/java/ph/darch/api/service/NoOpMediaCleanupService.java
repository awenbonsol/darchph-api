package ph.darch.api.service;

import org.springframework.stereotype.Service;
import ph.darch.api.entity.MediaAsset;

import java.util.Collection;

/**
 * No-op placeholder (TASK_5). TASK_6 provides the real implementation that
 * deletes orphaned {@code media_assets} rows and their Supabase Storage objects.
 */
@Service
public class NoOpMediaCleanupService implements MediaCleanupService {

    @Override
    public void deleteAssets(Collection<MediaAsset> assets) {
        // Intentionally a no-op until TASK_6 implements Storage cleanup.
    }
}
