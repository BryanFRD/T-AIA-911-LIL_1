package com.epitech.travelorder.common.model;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import lombok.extern.slf4j.Slf4j;
import org.mlflow.api.proto.ModelRegistry;
import org.mlflow.tracking.MlflowClient;
import org.mlflow.tracking.creds.BasicMlflowHostCreds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading ML models from MLFlow into DJL
 */
@Slf4j
public class ModelLoader {

    private final MlFlowConfig config;
    private final MlflowClient mlflowClient;
    private final Map<ModelType, ZooModel<?, ?>> modelCache;
    private final Map<ModelType, Path> artifactsPathCache;
    private final Map<ModelType, ModelMetadata> modelMetadataCache;
    private final Map<ModelType, ModelStatus> modelStatusCache;

    public ModelLoader(MlFlowConfig config) {
        this.config = config;
        BasicMlflowHostCreds mlflowHostCredentials = new BasicMlflowHostCreds(config.trackingUri());
        this.mlflowClient = new MlflowClient(mlflowHostCredentials);
        this.modelCache = new ConcurrentHashMap<>();
        this.artifactsPathCache = new ConcurrentHashMap<>();
        this.modelStatusCache = new ConcurrentHashMap<>();
        this.modelMetadataCache = new ConcurrentHashMap<>();

        try {
            Files.createDirectories(Paths.get(config.modelCacheDir()));
        } catch (IOException e) {
            log.error("Failed to create model cache directory", e);
        }
    }

    /**
     * Load a model from MLFlow and create a DJL ZooModel
     *
     * @param modelType The type of model to load
     * @param translator The DJL translator for input/output processing
     * @return ZooModel instance ready for inference
     * @throws ModelNotFoundException if model cannot be found in MLFlow
     * @throws IOException if model download or loading fails
     */
    public <I, O> ZooModel<I, O> loadModel(ModelType modelType, Translator<I, O> translator, Class<I> inputClass, Class<O> outputClass)
            throws ModelNotFoundException, IOException, MalformedModelException {

        @SuppressWarnings("unchecked")
        ZooModel<I, O> cachedModel = (ZooModel<I, O>) modelCache.get(modelType);
        if (cachedModel != null) {
            log.info("Using cached model for {}", modelType.getDescription());
            return cachedModel;
        }

        log.info("Loading model {} from MLFlow ({})", modelType.getDescription(), modelType.getMlflowName());

        Path modelPath = downloadModelFromMLFlow(modelType);
        String engine = determineEngine(modelPath);

        Criteria<I, O> criteria = Criteria.builder()
            .setTypes(inputClass, outputClass)
            .optModelPath(modelPath)
            .optEngine(engine)
            .optTranslator(translator)
            .optApplication(Application.UNDEFINED)
            .build();

        ZooModel<I, O> model = criteria.loadModel();
        modelCache.put(modelType, model);

        log.info("Successfully loaded model {} using {} engine", modelType.getDescription(), engine);
        return model;
    }

    /**
     * Download model from MLFlow registry to local cache
     */
    private Path downloadModelFromMLFlow(ModelType modelType) throws IOException {
        String modelName = modelType.getMlflowName();

        // Set status to LOADING
        modelStatusCache.put(modelType, ModelStatus.loading(modelType));

        log.info("Fetching latest version of model {} from MLFlow", modelName);

        try {
            List<ModelRegistry.ModelVersion> versions = mlflowClient.getLatestVersions(modelName);

            if (versions.isEmpty()) {
                throw new IOException("No versions found for model: " + modelName);
            }

            ModelRegistry.ModelVersion latestVersion = versions.stream()
                .max(Comparator.comparing(ModelRegistry.ModelVersion::getVersion))
                .orElseThrow(() -> new IOException("No valid version found for model: " + modelName));

            String runId = latestVersion.getRunId();
            String version = latestVersion.getVersion();
            log.info("Found model {} version {} (run_id: {})", modelName, version, runId);

            ModelMetadata metadata = new ModelMetadata(modelName, version, runId);
            modelMetadataCache.put(modelType, metadata);

            log.info("Downloading artifacts for model {} from MLFlow (this may take a while for large models)...", modelName);
            long startTime = System.currentTimeMillis();

            String artifactsPath = mlflowClient.downloadArtifacts(runId, "").getAbsolutePath();
            Path sourcePath = Paths.get(artifactsPath);

            long downloadTime = System.currentTimeMillis() - startTime;
            artifactsPathCache.put(modelType, sourcePath);

            log.info("Downloaded model artifacts to: {} (took {}s)", sourcePath, downloadTime / 1000.0);

            Path modelFile;
            try (var paths = Files.walk(sourcePath)) {
                modelFile = paths
                    .filter(p -> p.toString().endsWith(".onnx"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No .onnx file found in model artifacts"));
            }

            long fileSize = Files.size(modelFile);
            String fileSizeStr = formatFileSize(fileSize);

            Path cachedPath = Paths.get(config.modelCacheDir(), modelType.getDescription() + getFileExtension(modelFile));
            Files.copy(modelFile, cachedPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Model {} version {} downloaded to {} (size: {})", modelName, latestVersion.getVersion(), cachedPath, fileSizeStr);

            // Set status to READY
            modelStatusCache.put(modelType, ModelStatus.ready(modelType, metadata));

            return cachedPath;

        } catch (Exception e) {
            // Set status to FAILED
            modelStatusCache.put(modelType, ModelStatus.failed(modelType, "Failed to download model"));
            throw new IOException("Failed to download model from MLFlow: " + e.getMessage(), e);
        }
    }

    /**
     * Determine which DJL engine to use based on model file extension
     */
    private String determineEngine(Path modelPath) {
        String fileName = modelPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".onnx")) {
            return "OnnxRuntime";
        }
        throw new IllegalArgumentException("Unsupported model format: " + fileName + " (only ONNX is supported)");
    }

    /**
     * Get file extension from path
     */
    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : "";
    }

    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Create a predictor for a loaded model
     */
    public <I, O> Predictor<I, O> createPredictor(ModelType modelType, Translator<I, O> translator, Class<I> inputClass, Class<O> outputClass)
            throws ModelNotFoundException, IOException, MalformedModelException {
        ZooModel<I, O> model = loadModel(modelType, translator, inputClass, outputClass);
        return model.newPredictor();
    }

    /**
     * Clear cached models
     */
    public void clearCache() {
        modelCache.values().forEach(Model::close);
        modelCache.clear();
        log.info("Model cache cleared");
    }

    /**
     * Get available engines
     */
    public void logAvailableEngines() {
        log.info("Available DJL engines:");
        Engine.getAllEngines().forEach(engine ->
            log.info("  - {} (version: xxx)", engine)
        );
    }

    /**
     * Get the artifacts path for a specific model type
     * @param modelType The model type
     * @return Path to the downloaded artifacts directory, or null if not yet downloaded
     */
    public Path getArtifactsPath(ModelType modelType) {
        return artifactsPathCache.get(modelType);
    }

    /**
     * Get model metadata for a specific model type
     * @param modelType The model type
     * @return Model metadata, or null if not yet downloaded
     */
    public ModelMetadata getModelMetadata(ModelType modelType) {
        return modelMetadataCache.get(modelType);
    }

    /**
     * Ensure model artifacts are downloaded and return the artifacts path
     * This method downloads the model if not already cached
     * @param modelType The model type
     * @return Path to the downloaded artifacts directory
     * @throws IOException if download fails
     */
    public Path ensureArtifactsDownloaded(ModelType modelType) throws IOException {
        Path cachedPath = artifactsPathCache.get(modelType);
        if (cachedPath != null) {
            return cachedPath;
        }

        downloadModelFromMLFlow(modelType);

        return artifactsPathCache.get(modelType);
    }

    /**
     * Check if a model is loaded
     * @param modelType The model type
     * @return true if the model artifacts are cached
     */
    public boolean isModelLoaded(ModelType modelType) {
        return artifactsPathCache.containsKey(modelType);
    }

    /**
     * Get status for all model types
     * @return Map of model type to ModelStatus
     */
    public Map<ModelType, ModelStatus> getAllModelStatuses() {
        Map<ModelType, ModelStatus> statuses = new java.util.EnumMap<>(ModelType.class);
        for (ModelType modelType : ModelType.values()) {
            ModelStatus status = modelStatusCache.get(modelType);
            if (status != null) {
                statuses.put(modelType, status);
            } else {
                statuses.put(modelType, ModelStatus.notLoaded(modelType));
            }
        }
        return statuses;
    }
}
