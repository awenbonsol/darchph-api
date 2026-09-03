package ph.darch.api.service;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ph.darch.api.config.AppProperties;
import ph.darch.api.exception.UpstreamException;

/**
 * Thin client over the Supabase Storage REST API (server-side only).
 * Uses the service-role key from {@link AppProperties} and never exposes it.
 */
@Service
public class StorageService {

    private final AppProperties appProperties;
    private final RestClient restClient;

    public StorageService(AppProperties appProperties,
                          RestClient.Builder restClientBuilder) {
        this.appProperties = appProperties;
        this.restClient = restClientBuilder.baseUrl(appProperties.getSupabase().getUrl()).build();
    }

    /**
     * Uploads raw bytes to the given bucket under {@code key}.
     *
     * @throws UpstreamException if the storage API does not respond 2xx
     */
    public void upload(String bucket, String key, byte[] data, String contentType) {
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .headers(h -> applyAuth(h))
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(data)
                    .retrieve()
                    .onStatus(HttpStatusCode -> HttpStatusCode.value() >= 400,
                            (req, res) -> {
                                throw new StorageFailure(res.getStatusCode().value());
                            })
                    .toBodilessEntity();
        } catch (StorageFailure e) {
            throw new UpstreamException("Storage upload failed with status " + e.status);
        } catch (UpstreamException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new UpstreamException("Storage upload failed: " + e.getMessage());
        }
    }

    /**
     * Deletes the given object. Best-effort: intended to be wrapped by callers so
     * failures can be logged without breaking the main operation.
     */
    public void delete(String bucket, String key) {
        try {
            restClient.delete()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .headers(h -> applyAuth(h))
                    .retrieve()
                    .onStatus(HttpStatusCode -> HttpStatusCode.value() >= 400,
                            (req, res) -> {
                                throw new StorageFailure(res.getStatusCode().value());
                            })
                    .toBodilessEntity();
        } catch (StorageFailure e) {
            throw new UpstreamException("Storage delete failed with status " + e.status);
        } catch (UpstreamException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new UpstreamException("Storage delete failed: " + e.getMessage());
        }
    }

    public String publicUrl(String bucket, String key) {
        return appProperties.getSupabase().getUrl()
                + "/storage/v1/object/public/" + bucket + "/" + key;
    }

    private void applyAuth(HttpHeaders headers) {
        String key = appProperties.getSupabase().getServiceKey();
        headers.set("Authorization", "Bearer " + key);
        headers.set("apikey", key);
    }

    private static class StorageFailure extends RuntimeException {
        final int status;

        StorageFailure(int status) {
            this.status = status;
        }
    }
}
