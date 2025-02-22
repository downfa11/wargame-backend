package com.ns.feed.application.service;


import com.ns.common.anotation.UseCase;
import com.ns.feed.application.port.in.UpdateLikeUseCase;
import com.ns.feed.application.port.out.like.AddLikePort;
import com.ns.feed.application.port.out.like.FindLikePort;
import com.ns.feed.application.port.out.like.RemoveLikePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@UseCase
@RequiredArgsConstructor
public class UpdateLikeService implements UpdateLikeUseCase {
    private final AddLikePort addLikePort;
    private final FindLikePort findLikePort;
    private final RemoveLikePort removeLikePort;

    @Override
    public Mono<Long> updateLikes(Long userId, Long boardId) {
        return findLikePort.isUserLiked(boardId, userId)
                .flatMap(userLiked -> {
                    if (!userLiked) { // 좋아요
                        return addLikePort.addLike(boardId, String.valueOf(userId))
                                .then(findLikePort.getLikesCount(boardId));
                    } else if (userLiked) { // 좋아요 취소
                        return removeLikePort.removeLike(boardId, userId)
                                .then(findLikePort.getLikesCount(boardId));
                    } else {
                        return findLikePort.getLikesCount(boardId);
                    }
                });
    }
}
