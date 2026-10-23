package com.acme.catchup.platform.news.application.internal.commandservices;

import com.acme.catchup.platform.news.application.commandservices.FavoriteSourceCommandFailure;
import com.acme.catchup.platform.news.application.commandservices.FavoriteSourceCommandService;
import com.acme.catchup.platform.news.domain.model.aggregates.FavoriteSource;
import com.acme.catchup.platform.news.domain.model.commands.CreateFavoriteSourceCommand;
import com.acme.catchup.platform.news.infrastructure.persistence.jpa.FavoriteSourceRepository;
import static com.acme.catchup.platform.news.domain.model.aggregates.FavoriteSource.NEWS_API_KEY_SOURCE_ID_UNIQUE_CONSTRAINT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acme.catchup.platform.shared.application.result.Result;

@Slf4j
@Service
public class FavoriteSourceCommandServiceImpl implements FavoriteSourceCommandService {
    private static final String DUPLICATE_FAVORITE_SOURCE_CONSTRAINT = NEWS_API_KEY_SOURCE_ID_UNIQUE_CONSTRAINT;
    private final FavoriteSourceRepository favoriteSourceRepository;

    public FavoriteSourceCommandServiceImpl(FavoriteSourceRepository favoriteSourceRepository) {
        this.favoriteSourceRepository = favoriteSourceRepository;
    }

    @Override
    @Transactional
    public Result<FavoriteSource, FavoriteSourceCommandFailure> handle(CreateFavoriteSourceCommand command) {
        if (favoriteSourceRepository.existsByNewsApiKeyAndSourceId(command.newsApiKey(), command.sourceId())) {
            log.warn("Duplicate favorite source detected (pre-check): newsApiKey={}, sourceId={}",
                    command.newsApiKey().maskedForLogging(), command.sourceId().value());
            return duplicateResult();
        }
        try {
            var favoriteSource = new FavoriteSource(command);
            var createdFavoriteSource = favoriteSourceRepository.save(favoriteSource);
            log.info("Favorite source created: newsApiKey={}, sourceId={}, id={}, createdAt={}, updatedAt={}",
                    command.newsApiKey().maskedForLogging(),
                    command.sourceId().value(),
                    createdFavoriteSource.getId(),
                    createdFavoriteSource.getCreatedAt(),
                    createdFavoriteSource.getUpdatedAt());
            return Result.success(createdFavoriteSource);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateFavoriteSourceViolation(exception)) {
                log.warn("Duplicate favorite source detected (constraint violation): newsApiKey={}, sourceId={}",
                        command.newsApiKey().maskedForLogging(), command.sourceId().value());
                return duplicateResult();
            }
            log.error("Unexpected data integrity violation saving favorite source: newsApiKey={}, sourceId={}",
                    command.newsApiKey().maskedForLogging(), command.sourceId().value(), exception);
            throw exception;
        }
    }

    private Result<FavoriteSource, FavoriteSourceCommandFailure> duplicateResult() {
        return Result.failure(new FavoriteSourceCommandFailure.Duplicate());
    }

    private boolean isDuplicateFavoriteSourceViolation(DataIntegrityViolationException exception) {
        Throwable violationCause = exception;
        while (violationCause != null) {
            String message = violationCause.getMessage();
            if (message != null && message.contains(DUPLICATE_FAVORITE_SOURCE_CONSTRAINT)) return true;
            violationCause = violationCause.getCause();
        }
        return false;
    }
}