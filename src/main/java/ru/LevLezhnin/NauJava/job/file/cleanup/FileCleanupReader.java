package ru.LevLezhnin.NauJava.job.file.cleanup;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;

import java.util.Iterator;
import java.util.List;

public class FileCleanupReader implements ItemReader<FileCleanupRecord> {

    private final FileRepository fileRepository;
    private final int fetchSize;

    private List<FileCleanupRecord> currentBatch;
    private Iterator<FileCleanupRecord> currentBatchIterator;

    public FileCleanupReader(FileRepository fileRepository, int fetchSize) {
        this.fileRepository = fileRepository;
        this.fetchSize = fetchSize;
    }

    private void fetchNextBatch() {
        currentBatch = fileRepository.lockExpiredFiles(fetchSize);
        currentBatchIterator = currentBatch.iterator();
    }

    @Override
    public @Nullable FileCleanupRecord read() throws Exception {
        if (currentBatch == null) {
            fetchNextBatch();
        } else if (!currentBatchIterator.hasNext()) {

            if (currentBatch.size() < fetchSize) {
                return null;
            }

            fetchNextBatch();
        }

        if (!currentBatchIterator.hasNext()) {
            currentBatch = null;
            return null;
        }

        return currentBatchIterator.next();
    }
}
