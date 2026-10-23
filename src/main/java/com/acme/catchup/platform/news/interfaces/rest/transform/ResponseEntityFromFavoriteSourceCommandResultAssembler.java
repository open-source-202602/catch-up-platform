package com.acme.catchup.platform.news.interfaces.rest.transform;

import com.acme.catchup.platform.news.application.commandservices.FavoriteSourceCommandFailure;
import com.acme.catchup.platform.news.domain.model.aggregates.FavoriteSource;
import com.acme.catchup.platform.shared.application.result.Result;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.CREATED;

public class ResponseEntityFromFavoriteSourceCommandResultAssembler {
    public static ResponseEntity<?> toResponseEntityFromResult(
            Result<FavoriteSource, FavoriteSourceCommandFailure> result,
            MessageSource messageSource) {
        return result.fold(
                source -> new ResponseEntity<>(FavoriteSourceResourceFromEntityAssembler.toResourceFromEntity(source), CREATED),
                failure -> problemDetailResponse(failure, messageSource));
    }

    private static ResponseEntity<ProblemDetail> problemDetailResponse(FavoriteSourceCommandFailure failure, MessageSource messageSource) {
        var status = statusFromFailure(failure);
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(
                status,
                localizedMessage(messageSource, failure.messageKey())));
    }

    private static HttpStatus statusFromFailure(FavoriteSourceCommandFailure failure) {
        return switch (failure) {
            case FavoriteSourceCommandFailure.Duplicate duplicate -> HttpStatus.CONFLICT;
        };
    }

    private static String localizedMessage(MessageSource messageSource, String messageKey) {
        return messageSource.getMessage(messageKey, null, messageKey, LocaleContextHolder.getLocale());
    }
}