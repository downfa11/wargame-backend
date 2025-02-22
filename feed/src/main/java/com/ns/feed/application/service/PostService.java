package com.ns.feed.application.service;


import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;
import static com.ns.feed.exception.ErrorCode.NOT_FOUND_MEMBERSHIP_ERROR_MESSAGE;
import static com.ns.feed.exception.ErrorCode.NOT_FOUND_POST_ERROR_MESSAGE;

import com.ns.common.anotation.UseCase;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.in.post.DeletePostUseCase;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.in.post.ModifyPostUseCase;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.application.port.out.TaskConsumerPort;
import com.ns.feed.application.port.out.TaskProducerPort;
import com.ns.feed.application.port.out.comment.DeleteCommentPort;
import com.ns.feed.application.port.out.comment.FindCommentPort;
import com.ns.feed.application.port.out.image.DeleteImagePort;
import com.ns.feed.application.port.out.image.FindImagePort;
import com.ns.feed.application.port.out.image.UpdateImagePort;
import com.ns.feed.application.port.out.like.AddLikePort;
import com.ns.feed.application.port.out.like.FindLikePort;
import com.ns.feed.application.port.out.like.FindPostViewPort;
import com.ns.feed.application.port.out.like.IncreasePostViewPort;
import com.ns.feed.application.port.out.like.RemoveLikePort;
import com.ns.feed.application.port.out.like.RemovePostViewPort;
import com.ns.feed.application.port.out.post.DeletePostPort;
import com.ns.feed.application.port.out.post.FindPostPort;
import com.ns.feed.application.port.out.post.ModifyPostPort;
import com.ns.feed.application.port.out.post.RegisterPostPort;
import com.ns.feed.dto.Banner;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.dto.PostModifyRequest;
import com.ns.feed.dto.PostRegisterRequest;
import com.ns.feed.dto.PostResponse;
import com.ns.feed.dto.PostSummary;
import com.ns.feed.exception.FeedException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.convert.RemoveIndexedData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class PostService implements RegisterPostUseCase, ModifyPostUseCase, DeletePostUseCase, FindPostUseCase {

    private final RegisterPostPort registerPostPort;
    private final ModifyPostPort modifyPostPort;
    private final DeletePostPort deletePostPort;
    private final FindPostPort findPostPort;

    private final IncreasePostViewPort increasePostViewPort;
    private final FindPostViewPort findPostViewPort;
    private final RemovePostViewPort removePostViewPort;

    private final FindLikePort findLikePort;
    private final RemoveLikePort removeLikePort;

    private final TaskProducerPort taskProducerPort;

    private final FindCommentPort findCommentPort;
    private final DeleteCommentPort deleteCommentPort;

    private final FindImagePort findImagePort;

    private Mono<PostResponse> fetchCommentsForPost(PostResponse post) {
        return findCommentPort.findCommentResponseByPostId(post.getId())
                .collectList()
                .map(comments -> {
                    post.setCommentList(comments);
                    return post;
                });
    }


    @Override
    public Mono<Void> postByMembershipId(String taskId, String membershipId) {
        Long memberId = Long.parseLong(membershipId);

        return findPostPort.findPostsByMembershipId(memberId)
                .flatMap(post -> Mono.zip(
                        findPostViewPort.getPostViews(post.getId()),
                        findLikePort.getLikesCount(post.getId()),
                        (views, likes) -> updatePostSummary(post, views, likes)
                )
                .onErrorResume(e -> {
                    log.error("Error processing post: " + post.getId(), e);
                    return Mono.empty();
                }))
                .collectList()
                .flatMap(this::createSubTaskPostSummaryList)
                .flatMap(subTasks -> sendPostByMembershipIdTask(taskId, membershipId, subTasks));
    }

    @Override
    public Flux<Post> findPostsAll() {
        return findPostPort.findPostsAll();
    }

    @Override
    public Mono<Page<PostSummary>> findPostAllPagination(Long categoryId, PageRequest pageRequest) {
        Flux<Post> postFlux = findPostPort.findAllByCategoryId(categoryId, pageRequest);

        Mono<List<PostSummary>> postsMono = postFlux
                .flatMap(post -> Mono.zip(
                        findPostViewPort.getPostViews(post.getId()),
                        findLikePort.getLikesCount(post.getId()),
                        (views, likes) -> updatePostSummary(post, views, likes)
                ))
                .collectList();

        Mono<Long> countMono = findPostPort.countByCategoryId(categoryId);

        return Mono.zip(postsMono, countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()));
    }

    @Override
    public Mono<PostResponse> findPostResponseById(Long boardId) {
        return findPostPort.findPostByPostId(boardId)
                .flatMap(post -> increasePostViewPort.incrPostViews(boardId)
                        .flatMap(views -> findLikePort.getLikesCount(boardId)
                                .map(likes -> updatePostResponse(post, views, likes))
                        ))
                .flatMap(this::fetchCommentsForPost);
    }

    private PostSummary updatePostSummary(Post post, Long views, Long likes) {
        PostSummary summary = PostSummary.of(post);
        summary.setViews(views);
        summary.setLikes(likes);
        return summary;
    }


    private PostResponse updatePostResponse(Post post, Long views, Long likes) {
        PostResponse response = PostResponse.of(post);
        response.setViews(views);
        response.setLikes(likes);
        return response;
    }

    @Override
    public Mono<PostResponse> updatePostResponseById(Long boardId) {
        return findPostPort.findPostByPostId(boardId)
                .flatMap(post -> findPostViewPort.getPostViews(boardId)
                        .flatMap(views -> findLikePort.getLikesCount(boardId)
                                .map(likes -> updatePostResponse(post, views, likes))
                        ))
                .flatMap(this::fetchCommentsForPost);
    }

    @Override
    public Mono<BannerListWrapper> findLatestAnnounces(Integer count) {
        return findPostPort.findLatestAnnounces(count)
                .flatMap(post -> findImagePort.findImageUrlsById(post.getId())
                        .map(imageUrls -> PostResponse.of(post, imageUrls)))
                .collectList()
                .map(posts -> posts.stream()
                        .map(post -> createBanner(post.getTitle(), generatePostUrl(post.getId()), post.getFirstImageUrl()))
                        .collect(Collectors.toList()))
                .map(banners -> new BannerListWrapper(banners));
    }

    @Override
    public Mono<BannerListWrapper> findInProgressEvents() {
        return findPostPort.findInProgressEvents(LocalDateTime.now())
                .flatMap(post -> findImagePort.findImageUrlsById(post.getId())
                        .map(imageUrls -> PostResponse.of(post, imageUrls)))
                .collectList()
                .map(posts -> posts.stream()
                        .map(post -> createBanner(post.getTitle(), generatePostUrl(post.getId()), post.getFirstImageUrl()))
                        .collect(Collectors.toList()))
                .map(banners -> new BannerListWrapper(banners));
    }

    private Banner createBanner(String name, String url, String imageUrl) {
        return Banner.builder()
                .name(name)
                .url(url)
                .imageUrl(imageUrl)
                .build();
    }

    private String generatePostUrl(Long postId) {
        return String.format("/v1/posts/%d", postId);
    }

    @Override
    public Mono<Post> modify(Long userId, PostModifyRequest request) {
        return taskProducerPort.getUserNameByPost(userId)
                .flatMap(nickname -> modifyPostPort.modifyPost(nickname, request));
    }

    @Override
    public Mono<Post> create(Long userId, PostRegisterRequest request) {
        return taskProducerPort.getUserNameByPost(userId)
                .flatMap(userName -> registerPostPort.registerPost(createPost(request, userId, userName)))
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_MEMBERSHIP_ERROR_MESSAGE)));
    }

    private Post createPost(PostRegisterRequest request, Long userId, String userName) {
        return Post.builder()
                .userId(userId)
                .nickname(userName)
                .categoryId(request.getCategoryId())
                .sortStatus(request.getSortStatus())
                .title(request.getTitle())
                .content(request.getContent())
                .comments(0L)
                .eventStartDate(request.getEventStartDate())
                .eventEndDate(request.getEventEndDate())
                .build();
    }

    @Override
    public Mono<Void> deleteById(Long boardId) {
        return findPostPort.findPostByPostId(boardId)
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_POST_ERROR_MESSAGE)))
                .flatMap(post -> {
                    // redis에 기록된 조회수도 지워야한다.
                    removeLikePort.removeLikeAllByPostId(boardId);
                    removePostViewPort.removePostView(boardId);

                    return deleteCommentPort.deleteByBoardId(boardId)
                            .then(deletePostPort.deletePost(boardId));
                });
    }

    private Mono<List<SubTask>> createSubTaskPostSummaryList(List<PostSummary> posts) {
        return Flux.fromIterable(posts)
                .map(this::createSubTaskPostSummary)
                .collectList();
    }

    private SubTask createSubTaskPostSummary(PostSummary postSummary) {
        return createSubTask("PostSummary",
                String.valueOf(postSummary.getId()),
                SubTask.TaskType.post,
                SubTask.TaskStatus.success,
                postSummary);
    }

    private Mono<Void> sendPostByMembershipIdTask(String taskId, String membershipId, List<SubTask> subTasks) {
        return taskProducerPort.sendTask("task.membership.request", createTask(taskId, "Post Response", membershipId, subTasks));
    }
}
