package ru.LevLezhnin.NauJava.job.cleanup;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemReader;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;

import java.util.Iterator;
import java.util.List;

public class FileCleanupReader implements ItemReader<FileCleanupRecord> {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupReader.class);

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

        log.debug("Загружена новая пачка файлов для очистки: size={}, fetchSize={}",
                currentBatch.size(), fetchSize);

        if (currentBatch.size() < fetchSize) {
            log.debug("Достигнут конец данных: последняя пачка содержит {} записей", currentBatch.size());
        }
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
