package com.menzo.menzo.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.community.CommunityEvent;
import com.menzo.menzo.domain.community.Notification;
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.post.Comment;
import com.menzo.menzo.domain.post.PollOption;
import com.menzo.menzo.domain.post.PollVote;
import com.menzo.menzo.domain.post.Post;
import com.menzo.menzo.domain.post.PostBookmark;
import com.menzo.menzo.domain.post.PostLike;
import com.menzo.menzo.domain.post.PostType;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.post.AbstractVisualResponse;
import com.menzo.menzo.dto.post.CommentRequest;
import com.menzo.menzo.dto.post.CommentResponse;
import com.menzo.menzo.dto.post.CreatePostRequest;
import com.menzo.menzo.dto.post.PollOptionResponse;
import com.menzo.menzo.dto.post.PostResponse;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.community.CommunityEventRepository;
import com.menzo.menzo.repository.community.NotificationRepository;
import com.menzo.menzo.repository.post.CommentRepository;
import com.menzo.menzo.repository.post.PollOptionRepository;
import com.menzo.menzo.repository.post.PollVoteRepository;
import com.menzo.menzo.repository.post.PostBookmarkRepository;
import com.menzo.menzo.repository.post.PostLikeRepository;
import com.menzo.menzo.repository.post.PostRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

@Service
public class PostService {

    private static final int XP_PER_POST = 15;

    private final PostRepository postRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommunityEventRepository communityEventRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileMapper profileMapper;

    public PostService(
            PostRepository postRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            PostLikeRepository postLikeRepository,
            PostBookmarkRepository postBookmarkRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            CommunityEventRepository communityEventRepository,
            NotificationRepository notificationRepository,
            ProfileMapper profileMapper) {
        this.postRepository = postRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.postLikeRepository = postLikeRepository;
        this.postBookmarkRepository = postBookmarkRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.communityEventRepository = communityEventRepository;
        this.notificationRepository = notificationRepository;
        this.profileMapper = profileMapper;
    }

    @Transactional
    public PostResponse createPost(User author, CreatePostRequest request) {
        Post post = new Post();
        post.setAuthor(author);
        post.setType(request.type());
        post.setTitle(request.title());
        post.setBody(request.body());
        post.setImageUri(request.imageUri());
        post.setGradient(request.gradient());
        if (request.abstractVisual() != null) {
            post.setAbstractVisualPreset(request.abstractVisual().preset());
            post.setAbstractVisualCaption(request.abstractVisual().caption());
        }
        if (request.tags() != null) {
            post.setTags(new HashSet<>(request.tags()));
        }
        if (request.eventId() != null) {
            CommunityEvent event = communityEventRepository.findById(request.eventId())
                    .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
            post.setEvent(event);
        }

        post = postRepository.save(post);

        if (request.type() == PostType.poll) {
            if (request.pollOptions() == null || request.pollOptions().size() < 2) {
                throw new BadRequestException("Una encuesta necesita al menos dos opciones");
            }
            List<PollOption> options = new java.util.ArrayList<>();
            int order = 0;
            for (String label : request.pollOptions()) {
                options.add(new PollOption(post, label, order++));
            }
            pollOptionRepository.saveAll(options);
            post.setPollOptions(options);
        }

        author.setXp(author.getXp() + XP_PER_POST);
        author.setLevel(author.getXp() / 500 + 1);
        userRepository.save(author);

        return toPostResponse(post, author.getId());
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId, User viewer) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        return toPostResponse(post, viewer != null ? viewer.getId() : null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listFeed(Pageable pageable, User viewer) {
        return toPageResponse(postRepository.findAllByOrderByCreatedAtDesc(pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listFeatured(Pageable pageable, User viewer) {
        return toPageResponse(postRepository.findByFeaturedTrueOrderByCreatedAtDesc(pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByAuthor(UUID authorId, Pageable pageable, User viewer) {
        return toPageResponse(postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listBookmarked(User viewer, Pageable pageable) {
        return toPageResponse(postRepository.findBookmarkedByUser(viewer.getId(), pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> search(String query, Pageable pageable, User viewer) {
        return toPageResponse(postRepository.search(query, pageable), viewer);
    }

    @Transactional
    public void deletePost(User me, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        if (!post.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("Solo puedes eliminar tus propias publicaciones");
        }
        postRepository.delete(post);
    }

    @Transactional
    public void like(User me, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        if (postLikeRepository.existsByPostIdAndUserId(postId, me.getId())) {
            return;
        }
        postLikeRepository.save(new PostLike(postId, me.getId()));

        if (!post.getAuthor().getId().equals(me.getId())) {
            Notification notification = new Notification();
            notification.setRecipient(post.getAuthor());
            notification.setCategory(NotificationCategory.likes);
            notification.setTitle(me.getDisplayName() + " le dio like a tu publicación");
            notification.setBody(truncate(post.getBody()));
            notification.setRelatedPost(post);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void unlike(User me, UUID postId) {
        postLikeRepository.deleteByPostIdAndUserId(postId, me.getId());
    }

    @Transactional
    public void bookmark(User me, UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("Publicación no encontrada");
        }
        if (!postBookmarkRepository.existsByPostIdAndUserId(postId, me.getId())) {
            postBookmarkRepository.save(new PostBookmark(postId, me.getId()));
        }
    }

    @Transactional
    public void unbookmark(User me, UUID postId) {
        postBookmarkRepository.deleteByPostIdAndUserId(postId, me.getId());
    }

    @Transactional
    public PostResponse votePoll(User me, UUID postId, UUID optionId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        if (post.getType() != PostType.poll) {
            throw new BadRequestException("Esta publicación no es una encuesta");
        }
        boolean optionBelongsToPost = post.getPollOptions().stream().anyMatch(o -> o.getId().equals(optionId));
        if (!optionBelongsToPost) {
            throw new NotFoundException("Opción no encontrada en esta encuesta");
        }

        pollVoteRepository.deleteByPostIdAndUserId(postId, me.getId());
        pollVoteRepository.save(new PollVote(optionId, postId, me.getId()));

        return toPostResponse(post, me.getId());
    }

    @Transactional
    public CommentResponse addComment(User me, UUID postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(me);
        comment.setBody(request.body());
        comment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        if (!post.getAuthor().getId().equals(me.getId())) {
            Notification notification = new Notification();
            notification.setRecipient(post.getAuthor());
            notification.setCategory(NotificationCategory.comentarios);
            notification.setTitle(me.getDisplayName() + " comentó tu publicación");
            notification.setBody(truncate(request.body()));
            notification.setRelatedPost(post);
            notificationRepository.save(notification);
        }

        return toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listComments(UUID postId, Pageable pageable) {
        return PageResponse.of(
                commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable),
                this::toCommentResponse);
    }

    private PageResponse<PostResponse> toPageResponse(Page<Post> page, User viewer) {
        UUID viewerId = viewer != null ? viewer.getId() : null;
        return PageResponse.of(page, p -> toPostResponse(p, viewerId));
    }

    private PostResponse toPostResponse(Post post, UUID viewerId) {
        long likeCount = postLikeRepository.countByPostId(post.getId());
        boolean likedByMe = viewerId != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), viewerId);
        boolean bookmarkedByMe = viewerId != null && postBookmarkRepository.existsByPostIdAndUserId(post.getId(), viewerId);

        AbstractVisualResponse abstractVisual = post.getAbstractVisualPreset() != null
                ? new AbstractVisualResponse(post.getAbstractVisualPreset(), post.getAbstractVisualCaption())
                : null;

        List<PollOptionResponse> pollOptions = List.of();
        if (post.getType() == PostType.poll && !post.getPollOptions().isEmpty()) {
            List<UUID> optionIds = post.getPollOptions().stream().map(PollOption::getId).toList();
            List<PollVote> votes = pollVoteRepository.findByOptionIdIn(optionIds);
            Map<UUID, List<PollVote>> votesByOption = votes.stream()
                    .collect(Collectors.groupingBy(PollVote::getOptionId));

            pollOptions = post.getPollOptions().stream()
                    .map(option -> {
                        List<PollVote> optionVotes = votesByOption.getOrDefault(option.getId(), List.of());
                        boolean votedByMe = viewerId != null
                                && optionVotes.stream().anyMatch(v -> v.getUserId().equals(viewerId));
                        return new PollOptionResponse(option.getId(), option.getLabel(), optionVotes.size(), votedByMe);
                    })
                    .toList();
        }

        return new PostResponse(
                post.getId(),
                profileMapper.toSummary(post.getAuthor()),
                post.getType().name(),
                post.getTitle(),
                post.getBody(),
                post.getImageUri(),
                abstractVisual,
                post.getGradient(),
                post.getTags().stream().sorted().toList(),
                pollOptions,
                post.getEvent() != null ? post.getEvent().getId() : null,
                likeCount,
                likedByMe,
                bookmarkedByMe,
                post.getCommentCount(),
                post.isFeatured(),
                post.getCreatedAt());
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                profileMapper.toSummary(comment.getAuthor()),
                comment.getBody(),
                comment.getCreatedAt());
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 140 ? text.substring(0, 140) + "…" : text;
    }
}
