package com.sap.sailing.windestimation.model.store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.sap.sailing.windestimation.model.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.model.TrainableModel;
import com.sap.sailing.windestimation.model.exception.ModelLoadingException;
import com.sap.sailing.windestimation.model.exception.ModelNotFoundException;
import com.sap.sailing.windestimation.model.exception.ModelPersistenceException;

public class ClassPathModelStore extends AbstractModelStore {

    private final String destinationFolder;

    public ClassPathModelStore(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private String getFilePath(String filename) {
        if (destinationFolder.isEmpty()) {
            return filename;
        }
        return destinationFolder + "/" + filename;
    }

    @Override
    public <InstanceType, T extends ContextSpecificModelMetadata<InstanceType>, ModelType extends TrainableModel<InstanceType, T>> ModelType loadPersistedState(
            ModelType newModel) throws ModelPersistenceException {
        PersistenceSupport persistenceSupport = checkAndGetPersistenceSupport(newModel);
        String fileName = getFilename(newModel);
        String filePath = getFilePath(fileName);
        InputStream input = getResourceAsStream(filePath);
        if (input != null) {
            try {
                @SuppressWarnings("unchecked")
                ModelType loadedModel = (ModelType) persistenceSupport.loadFromStream(input);
                if (!newModel.getContextSpecificModelMetadata().equals(loadedModel.getContextSpecificModelMetadata())) {
                    throw new ModelPersistenceException(
                            "The configuration of the loaded model is: " + loadedModel.getContextSpecificModelMetadata()
                                    + ". \nExpected: " + newModel.getContextSpecificModelMetadata());
                }
                return loadedModel;
            } catch (IOException e) {
                throw new ModelPersistenceException(e);
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        throw new ModelNotFoundException(newModel.getContextSpecificModelMetadata());
    }

    @Override
    public <T extends PersistableModel<?, ?>> void persistState(T trainedModel) throws ModelPersistenceException {
        // ignore
    }

    public <T extends PersistableModel<?, ?>> void delete(T newModel) throws ModelPersistenceException {
        // ignore
    }

    @Override
    public void deleteAll(PersistenceContextType contextType) throws ModelPersistenceException {
        // ignore
    }

    @Override
    public Map<String, byte[]> exportAllPersistedModels(PersistenceContextType contextType)
            throws ModelPersistenceException {
        Map<String, byte[]> exportedModels = new HashMap<>();
        try {
            for (String fileName : getResourceFiles(destinationFolder)) {
                if (isFileBelongingToContextType(fileName, contextType)) {
                    String filePath = getFilePath(fileName);
                    byte[] exportedModel;
                    try (InputStream inputStream = getResourceAsStream(filePath)) {
                        exportedModel = IOUtils.toByteArray(inputStream);
                    } catch (IOException e) {
                        throw new ModelPersistenceException("Could not read model \"" + fileName + "\" from filesystem",
                                e);
                    }
                    exportedModels.put(fileName, exportedModel);
                }
            }
        } catch (IOException e) {
            throw new ModelPersistenceException(e);
        }
        return exportedModels;
    }

    @Override
    public void importPersistedModels(Map<String, byte[]> exportedPersistedModels, PersistenceContextType contextType)
            throws ModelPersistenceException {
        // ignore
    }

    @Override
    public List<PersistableModel<?, ?>> loadAllPersistedModels(PersistenceContextType contextType) {
        List<PersistableModel<?, ?>> loadedModels = new ArrayList<>();
        try {
            for (String fileName : getResourceFiles(destinationFolder)) {
                if (isFileBelongingToContextType(fileName, contextType)) {
                    String filePath = getFilePath(fileName);
                    try (InputStream inputStream = getResourceAsStream(filePath)) {
                        PersistenceSupport persistenceSupport = getPersistenceSupportFromFilename(fileName);
                        if (persistenceSupport == null) {
                            throw new ModelLoadingException(
                                    "Persistence support could not be determined due to invalid filename pattern: \""
                                            + fileName + "\"");
                        }
                        PersistableModel<?, ?> loadedModel;
                        try (InputStream input = getResourceAsStream(filePath)) {
                            loadedModel = persistenceSupport.loadFromStream(input);
                        } catch (Exception e) {
                            throw new ModelLoadingException("Could not read model \"" + fileName + "\" from filesystem",
                                    e);
                        }
                        loadedModels.add(loadedModel);
                    }
                }
            }
        } catch (IOException e) {
            throw new ModelLoadingException(e);
        }
        return loadedModels;
    }

}
