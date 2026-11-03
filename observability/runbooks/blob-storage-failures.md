# Azure Blob news-image failures

- **Signal:** `MyComplexBlobStorageFailures` and `azure_blob_news_image_seconds_count{outcome="error"}`.
- **Dashboard:** MyComplex Overview → Azure Blob outcomes, average duration, and external integration errors.
- **Safe checks:** confirm backend and Azurite/Azure Storage health; inspect aggregate failures by `operation`; verify connection-string/container presence without printing values; test container reachability from the backend network.
- **Likely causes:** Azurite or Azure Storage unavailable, invalid credentials, missing container permissions, timeout, network failure, or quota exhaustion.
- **Recovery:** restore the storage endpoint or credentials and retry one controlled image upload/read/delete through the application.
- **Escalate:** stored metadata points to unavailable content, multiple operations fail, or production data durability is uncertain.
- **Do not:** do not log blob paths, property/news IDs, filenames, image bytes, SAS tokens, or connection strings; do not delete the storage volume or container.
