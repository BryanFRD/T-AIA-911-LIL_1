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
import org.mlflow.tracking.MlflowClient;
import org.mlflow.tracking.creds.BasicMlflowHostCreds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading ML models from MLflow into DJL
 */
@Slf4j
public class ModelLoader {

    private final MLflowConfig config;
    private final MlflowClient mlflowClient;
    private final Map<ModelType, ZooModel<?, ?>> modelCache;

    public ModelLoader(MLflowConfig config) {
        this.config = config;
        this.mlflowClient = new MlflowClient(
            new BasicMlflowHostCreds(config.getTrackingUri())
        );
        this.modelCache = new ConcurrentHashMap<>();

        // Create model cache directory
        try {
            Files.createDirectories(Paths.get(config.getModelCacheDir()));
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
            log.info("Using cached model for {}", modelType.getAlias());
            return cachedModel;
        }

        log.info("Loading model {} from MLflow ({})", modelType.getAlias(), modelType.getMlflowName());

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

        log.info("Successfully loaded model {} using {} engine", modelType.getAlias(), engine);
        return model;
    }

    /**
     * Download model from MLflow registry to local cache
     */
    private Path downloadModelFromMLflow(ModelType modelType) throws IOException {
        String modelName = modelType.getMlflowName();
        String alias = config.getModelAlias();

        log.debug("Fetching model {} from MLflow (alias: {})", modelName, alias);

        try {
            // Get model version by alias (e.g., "latest", "production", "staging") or version number
            String modelUri = String.format("models:/%s@%s", modelName, alias);

            // Download model artifacts
            String artifactsPath = mlflowClient.downloadArtifacts(modelUri).getAbsolutePath();
            Path sourcePath = Paths.get(artifactsPath);

            // Find the model file (.pt or .onnx)
            Path modelFile = Files.walk(sourcePath)
                .filter(p -> p.toString().endsWith(".pt") || p.toString().endsWith(".onnx"))
                .findFirst()
                .orElseThrow(() -> new IOException("No .pt or .onnx file found in model artifacts"));

            // Copy to cache directory
            Path cachedPath = Paths.get(config.getModelCacheDir(), modelType.getAlias() + getFileExtension(modelFile));
            Files.copy(modelFile, cachedPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Model {} downloaded to {}", modelType.getAlias(), cachedPath);
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
        if (fileName.endsWith(".pt") || fileName.endsWith(".pth")) {
            return "PyTorch";
        } else if (fileName.endsWith(".onnx")) {
            return "OnnxRuntime";
        }
        throw new IllegalArgumentException("Unsupported model format: " + fileName);
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
        try (ZooModel<I, O> model = loadModel(modelType, translator, inputClass, outputClass)) {
            return model.newPredictor();
        }
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
}
