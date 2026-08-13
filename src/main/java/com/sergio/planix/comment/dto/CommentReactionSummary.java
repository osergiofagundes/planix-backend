package com.sergio.planix.comment.dto;

import com.sergio.planix.auth.dto.UserSummary;
import com.sergio.planix.comment.CommentReaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Um emoji e quem reagiu com ele em um comentário.")
public record CommentReactionSummary(
        @Schema(description = "Emoji da reação", example = "👍") String emoji,
        @Schema(description = "Quantas pessoas reagiram com este emoji", example = "3") int count,
        @Schema(description = "Se a conta autenticada é uma delas", example = "true") boolean reactedByMe,
        @Schema(description = "Quem reagiu, na ordem em que reagiu") List<UserSummary> users
) {

    public static CommentReactionSummary of(String emoji, List<CommentReaction> reactions, Long currentUserId) {
        return new CommentReactionSummary(
                emoji,
                reactions.size(),
                reactions.stream().anyMatch(r -> r.getUser().getId().equals(currentUserId)),
                reactions.stream().map(r -> UserSummary.from(r.getUser())).toList());
    }
}
