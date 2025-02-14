package com.ns.feed.adapter.out.persistence.post;

import static com.ns.feed.exception.ErrorCode.NOT_FOUND_POST_ERROR_MESSAGE;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.feed.application.port.out.TaskProducerPort;
import com.ns.feed.exception.FeedException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@PersistanceAdapter
public class PostPersistenceAdapter  implements RegisterPostPort, ModifyPostPort, DeletePostPort, FindPostPort {

    private final PostR2dbcRepository postR2dbcRepository;


    @Override
    public Mono<Post> registerPost(Post post) {
        return postR2dbcRepository.save(post);
    }

    @Override
    public Mono<Post> modifyPost(String nickname, PostModifyRequest request) {
        return postR2dbcRepository.findById(request.getBoardId())
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_POST_ERROR_MESSAGE)))
                .flatMap(post ->{
                    post.setCategoryId(request.getCategoryId());
                    post.setNickname(nickname);
                    post.setSortStatus(request.getSortStatus());
                    post.setTitle(request.getTitle());
                    post.setContent(request.getContent());
                    post.setEventStartDate(request.getEventStartDate());
                    post.setEventEndDate(request.getEventEndDate());
                    return postR2dbcRepository.save(post);
                });
    }

    @Override
    public Mono<Post> update(Post post) { return postR2dbcRepository.save(post); }

    @Override
    public Mono<Void> deletePost(Long boardId) {
        return postR2dbcRepository.deleteById(boardId);
    }

    @Override
    public Mono<Post> findPostByPostId(Long postId) {
        return postR2dbcRepository.findById(postId);
    }

    @Override
    public Mono<Long> countByCategoryId(Long categoryId) {
        return postR2dbcRepository.countByCategoryId(categoryId);
    }

    @Override
    public Flux<Post> findPostsByMembershipId(Long membershipId) {
        return postR2dbcRepository.findAllByUserId(membershipId);
    }

    @Override
    public Flux<Post> findAllByCategoryId(Long categoryId, PageRequest pageRequest) {
        return postR2dbcRepository.findAllByCategoryId(categoryId, pageRequest);
    }

    @Override
    public Flux<Post> findPostsAll() {
        return postR2dbcRepository.findAll();
    }

    @Override
    public Flux<Post> findLatestAnnounces(Integer count) {
        return postR2dbcRepository.findLatestAnnounces(count);
    }

    @Override
    public Flux<Post> findInProgressEvents(LocalDateTime now) {
        return postR2dbcRepository.findInProgressEvents(now);
    }
}
