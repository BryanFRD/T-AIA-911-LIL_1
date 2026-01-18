package com.epitech.travelorder.common.domain.mlflow;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import com.epitech.travelorder.common.domain.enumeration.ModelType;
import com.epitech.travelorder.common.util.LogUtils;
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

@Slf4j
public class ModelLoader {

    private static final LogUtils LOG_UTILS_CTX_INIT = LogUtils.of(LogUtils.COMMON, "ModelLoader:Init");

    private static final LogUtils LOG_UTILS_CTX_LOAD = LogUtils.of(LogUtils.COMMON, "ModelLoader:Load");

    private static final LogUtils LOG_UTILS_CTX_DL = LogUtils.of(LogUtils.COMMON, "ModelLoader:Download");

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
            log.info(LOG_UTILS_CTX_INIT.format("Model cache directory: %s", config.modelCacheDir()));
        } catch (IOException e) {
            log.error(LOG_UTILS_CTX_INIT.format("Failed to create model cache directory"), e);
        }
    }

    public <I, O> ZooModel<I, O> loadModel(ModelType modelType, Translator<I, O> translator, Class<I> inputClass, Class<O> outputClass)
            throws ModelNotFoundException, IOException, MalformedModelException {

        @SuppressWarnings("unchecked")
        ZooModel<I, O> cachedModel = (ZooModel<I, O>) modelCache.get(modelType);
        if (cachedModel != null) {
            log.info(LOG_UTILS_CTX_LOAD.format("Using cached model for %s", modelType.getDescription()));
            return cachedModel;
        }

        log.info(LOG_UTILS_CTX_LOAD.format("Loading model %s from MLFlow (%s)", modelType.getDescription(), modelType.getMlflowName()));

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

        log.info(LOG_UTILS_CTX_LOAD.format("Successfully loaded model %s using %s engine", modelType.getDescription(), engine));
        return model;
    }

    public <I, O> Predictor<I, O> createPredictor(ModelType modelType, Translator<I, O> translator, Class<I> inputClass, Class<O> outputClass)
            throws ModelNotFoundException, IOException, MalformedModelException {
        ZooModel<I, O> model = loadModel(modelType, translator, inputClass, outputClass);
        return model.newPredictor();
    }

    public void clearCache() {
        modelCache.values().forEach(Model::close);
        modelCache.clear();
        log.info(LOG_UTILS_CTX_INIT.format("Model cache cleared"));
    }

    public void logAvailableEngines() {
        log.info(LOG_UTILS_CTX_INIT.format("Available DJL engines:"));
        Engine.getAllEngines().forEach(engine ->
                log.info(LOG_UTILS_CTX_INIT.format("  - %s", engine))
        );
    }

    public ModelMetadata getModelMetadata(ModelType modelType) {
        return modelMetadataCache.get(modelType);
    }

    public Path ensureArtifactsDownloaded(ModelType modelType) throws IOException {
        Path cachedPath = artifactsPathCache.get(modelType);
        if (cachedPath != null) {
            return cachedPath;
        }

        downloadModelFromMLFlow(modelType);

        return artifactsPathCache.get(modelType);
    }

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

    private Path downloadModelFromMLFlow(ModelType modelType) throws IOException {
        String modelName = modelType.getMlflowName();

        modelStatusCache.put(modelType, ModelStatus.loading(modelType));

        log.info(LOG_UTILS_CTX_DL.format("Fetching latest version of model %s from MLFlow", modelName));

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
            log.info(LOG_UTILS_CTX_DL.format("Found model %s version %s (run_id: %s)", modelName, version, runId));

            ModelMetadata metadata = new ModelMetadata(modelName, version, runId);
            modelMetadataCache.put(modelType, metadata);

            log.info(LOG_UTILS_CTX_DL.format("Downloading artifacts for model %s...", modelName));
            long startTime = System.currentTimeMillis();

            String artifactsPath = mlflowClient.downloadArtifacts(runId, "").getAbsolutePath();
            Path sourcePath = Paths.get(artifactsPath);

            long downloadTime = System.currentTimeMillis() - startTime;
            artifactsPathCache.put(modelType, sourcePath);

            log.info(LOG_UTILS_CTX_DL.format("Downloaded model artifacts to: %s (took %.2fs)", sourcePath, downloadTime / 1000.0));

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

            log.info(LOG_UTILS_CTX_DL.format("Model %s version %s downloaded to %s (size: %s)", modelName, latestVersion.getVersion(), cachedPath, fileSizeStr));
            log.info(LOG_UTILS_CTX_DL.format("Model %s version %s downloaded to %s (size: %s)", modelName, latestVersion.getVersion(), cachedPath, fileSizeStr));

            modelStatusCache.put(modelType, ModelStatus.ready(modelType, metadata));

            return cachedPath;
        } catch (Exception e) {
            modelStatusCache.put(modelType, ModelStatus.failed(modelType, "Failed to download model"));
            throw new IOException("Failed to download model from MLFlow: " + e.getMessage(), e);
        }
    }

    private String determineEngine(Path modelPath) {
        String fileName = modelPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".onnx")) {
            return "OnnxRuntime";
        }
        throw new IllegalArgumentException("Unsupported model format: " + fileName + " (only ONNX is supported)");
    }

    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : "";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
