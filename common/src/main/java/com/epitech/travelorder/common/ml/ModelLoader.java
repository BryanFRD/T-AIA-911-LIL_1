package com.epitech.travelorder.common.ml;

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
import org.mlflow.api.proto.Service;
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
 * Service for loading ML models from MLflow into DJL
 */
@Slf4j
public class ModelLoader {

    private final MlFlowConfig config;
    private final MlflowClient mlflowClient;
    private final Map<ModelType, ZooModel<?, ?>> modelCache;
    private final Map<ModelType, Path> artifactsPathCache;
    private final Map<ModelType, ModelMetadata> modelMetadataCache;

    public ModelLoader(MlFlowConfig config) {
        this.config = config;
        BasicMlflowHostCreds creds = new BasicMlflowHostCreds(config.trackingUri());
        this.mlflowClient = new MlflowClient(creds);
        this.modelCache = new ConcurrentHashMap<>();
        this.artifactsPathCache = new ConcurrentHashMap<>();
        this.modelMetadataCache = new ConcurrentHashMap<>();

        // Create model cache directory
        try {
            Files.createDirectories(Paths.get(config.modelCacheDir()));
        } catch (IOException e) {
            log.error("Failed to create model cache directory", e);
        }
    }

    /**
     * Load a model from MLflow and create a DJL ZooModel
     *
     * @param modelType The type of model to load
     * @param translator The DJL translator for input/output processing
     * @return ZooModel instance ready for inference
     * @throws ModelNotFoundException if model cannot be found in MLflow
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

        log.info("Loading model {} from MLflow ({})", modelType.getDescription(), modelType.getMlflowName());

        // Download model from MLflow
        Path modelPath = downloadModelFromMLflow(modelType);

        // Determine engine based on file extension
        String engine = determineEngine(modelPath);

        // Create DJL Criteria
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
     * Download model from MLflow registry to local cache
     */
    private Path downloadModelFromMLflow(ModelType modelType) throws IOException {
        String modelName = modelType.getMlflowName();

        log.info("Fetching latest version of model {} from MLflow", modelName);

        try {
            // Get latest versions of the model
            List<ModelRegistry.ModelVersion> versions = mlflowClient.getLatestVersions(modelName);

            if (versions.isEmpty()) {
                throw new IOException("No versions found for model: " + modelName);
            }

            // Sort by version number and get the latest
            ModelRegistry.ModelVersion latestVersion = versions.stream()
                .max(Comparator.comparing(ModelRegistry.ModelVersion::getVersion))
                .orElseThrow(() -> new IOException("No valid version found for model: " + modelName));

            String runId = latestVersion.getRunId();
            String version = latestVersion.getVersion();
            log.info("Found model {} version {} (run_id: {})", modelName, version, runId);

            // Cache model metadata
            modelMetadataCache.put(modelType, new ModelMetadata(modelName, version, runId));

            // Download model artifacts from the run using the run_id
            String artifactsPath = mlflowClient.downloadArtifacts(runId, "").getAbsolutePath();
            Path sourcePath = Paths.get(artifactsPath);

            // Cache the artifacts path for this model type
            artifactsPathCache.put(modelType, sourcePath);

            log.info("Downloaded model artifacts to: {}", sourcePath);

            // Find the model file (.onnx)
            Path modelFile = Files.walk(sourcePath)
                .filter(p -> p.toString().endsWith(".onnx"))
                .findFirst()
                .orElseThrow(() -> new IOException("No .onnx file found in model artifacts"));

            // Copy to cache directory
            Path cachedPath = Paths.get(config.modelCacheDir(), modelType.getDescription() + getFileExtension(modelFile));
            Files.copy(modelFile, cachedPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Model {} version {} downloaded to {}", modelName, latestVersion.getVersion(), cachedPath);
            return cachedPath;

        } catch (Exception e) {
            throw new IOException("Failed to download model from MLflow: " + e.getMessage(), e);
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

        // Download model artifacts (this will cache the path)
        downloadModelFromMLflow(modelType);

        return artifactsPathCache.get(modelType);
    }
}
