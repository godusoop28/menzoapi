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
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.moderation.ModerationActionType;
import com.menzo.menzo.domain.post.Comment;
import com.menzo.menzo.domain.post.PollOption;
import com.menzo.menzo.domain.post.PollVote;
import com.menzo.menzo.domain.post.Post;
import com.menzo.menzo.domain.post.PostBlock;
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
import com.menzo.menzo.dto.post.UpdatePostRequest;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.community.CommunityEventRepository;
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
    private static final int MAX_BLOCKS = 40;
    private static final int MAX_BLOCKS_TOTAL_CHARS = 50_000;
    private static final int MAX_PARAGRAPH_CHARS = 2000;
    private static final int MAX_HEADING_CHARS = 150;
    private static final int MAX_ALT_CHARS = 200;

    private final PostRepository postRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommunityEventRepository communityEventRepository;
    private final NotificationService notificationService;
    private final ProfileMapper profileMapper;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ModerationLogService moderationLogService;

    public PostService(
            PostRepository postRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            PostLikeRepository postLikeRepository,
            PostBookmarkRepository postBookmarkRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            CommunityEventRepository communityEventRepository,
            NotificationService notificationService,
            ProfileMapper profileMapper,
            AdminAuthorizationService adminAuthorizationService,
            ModerationLogService moderationLogService) {
        this.postRepository = postRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.postLikeRepository = postLikeRepository;
        this.postBookmarkRepository = postBookmarkRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.communityEventRepository = communityEventRepository;
        this.notificationService = notificationService;
        this.profileMapper = profileMapper;
        this.adminAuthorizationService = adminAuthorizationService;
        this.moderationLogService = moderationLogService;
    }

    @Transactional
    public PostResponse createPost(User author, CreatePostRequest request) {
        Post post = new Post();
        post.setAuthor(author);
        post.setType(request.type());
        post.setTitle(request.title());
        applyBodyAndBlocks(post, request.type(), request.body(), request.blocks());
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

        // saveAndFlush: @CreationTimestamp recién completa createdAt al ejecutarse el INSERT (al
        // hacer flush), no al llamar a save(). Sin esto, toPostResponse (misma transacción, más
        // abajo) leería createdAt en null.
        post = postRepository.saveAndFlush(post);

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
        return toPageResponse(postRepository.findByHiddenFalseOrderByCreatedAtDesc(pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listFeatured(Pageable pageable, User viewer) {
        return toPageResponse(postRepository.findByFeaturedTrueAndHiddenFalseOrderByCreatedAtDesc(pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByAuthor(UUID authorId, Pageable pageable, User viewer) {
        return toPageResponse(postRepository.findByAuthorIdAndHiddenFalseOrderByCreatedAtDesc(authorId, pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listBookmarked(User viewer, Pageable pageable) {
        return toPageResponse(postRepository.findBookmarkedByUser(viewer.getId(), pageable), viewer);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> search(String query, Pageable pageable, User viewer) {
        return toPageResponse(postRepository.search(query, pageable), viewer);
    }

    /** El autor siempre puede borrar la suya, sin motivo. Un no-autor necesita LEADER+ y un
     * motivo obligatorio (queda registrado en el log de moderación, visible para MASTER). */
    @Transactional
    public void deletePost(User me, UUID postId, String staffReason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        boolean isAuthor = post.getAuthor().getId().equals(me.getId());
        if (!isAuthor) {
            adminAuthorizationService.requireLeader(me);
            if (staffReason == null || staffReason.isBlank()) {
                throw new BadRequestException("Necesitás indicar un motivo");
            }
        }
        UUID postIdCopy = post.getId();
        postRepository.delete(post);
        if (!isAuthor) {
            moderationLogService.record(me, ModerationActionType.DELETE_POST, "POST", postIdCopy, staffReason);
        }
    }

    /** CURATOR+: oculta una publicación de los listados públicos sin borrarla. Siempre requiere
     * motivo y queda registrado. */
    @Transactional
    public void hidePost(User me, UUID postId, String reason) {
        adminAuthorizationService.requireCurator(me);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        post.setHidden(true);
        postRepository.save(post);
        moderationLogService.record(me, ModerationActionType.HIDE_POST, "POST", post.getId(), reason);
    }

    @Transactional
    public void unhidePost(User me, UUID postId, String reason) {
        adminAuthorizationService.requireCurator(me);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        post.setHidden(false);
        postRepository.save(post);
        moderationLogService.record(me, ModerationActionType.UNHIDE_POST, "POST", post.getId(), reason);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> searchForAdmin(User me, String query, Pageable pageable) {
        adminAuthorizationService.requireCurator(me);
        return toPageResponse(postRepository.searchForAdmin(query, pageable), me);
    }

    @Transactional
    public PostResponse updatePost(User me, UUID postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Publicación no encontrada"));
        if (!post.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("Solo puedes editar tus propias publicaciones");
        }
        post.setTitle(request.title());
        applyBodyAndBlocks(post, post.getType(), null, request.blocks());
        if (request.tags() != null) {
            post.setTags(new HashSet<>(request.tags()));
        }
        post = postRepository.saveAndFlush(post);
        return toPostResponse(post, me.getId());
    }

    /**
     * Único punto que decide `body`/`blocks` para un post, tanto en creación como en edición.
     * Solo los tipos text/image usan bloques (poll/question/event siguen mandando su body crudo
     * directamente, como siempre) — con bloques presentes, `body` se PISA con un excerpt derivado
     * (nunca lo que mandó el cliente en ese campo), así que search/notificaciones (que leen
     * `Post.body`) siguen funcionando sin tocarlos. Sin bloques (lista vacía/nula), se cae al
     * `rawBody` tal cual llegó — cubre tanto los tipos sin bloques como cualquier post viejo/
     * cliente que todavía mande el cuerpo plano de antes.
     */
    private void applyBodyAndBlocks(Post post, PostType type, String rawBody, List<PostBlock> blocks) {
        boolean usesBlocks = (type == PostType.text || type == PostType.image)
                && blocks != null && !blocks.isEmpty();
        if (usesBlocks) {
            validateBlocks(blocks);
            post.setBlocks(blocks);
            post.setBody(deriveBodyFromBlocks(blocks));
        } else {
            if (rawBody == null || rawBody.isBlank()) {
                throw new BadRequestException("La publicación necesita contenido");
            }
            post.setBlocks(List.of());
            post.setBody(rawBody);
        }
    }

    /** Package-private + static a propósito (sin estado de instancia) — testeable sin contexto de
     * Spring ni base de datos, mismo criterio ya usado para ChatService.INBOX_ORDER. */
    static void validateBlocks(List<PostBlock> blocks) {
        if (blocks.size() > MAX_BLOCKS) {
            throw new BadRequestException("Una publicación admite hasta " + MAX_BLOCKS + " bloques");
        }
        int totalChars = 0;
        for (PostBlock block : blocks) {
            if (block.type() == null) {
                throw new BadRequestException("Cada bloque necesita un tipo");
            }
            switch (block.type()) {
                case PostBlock.TYPE_PARAGRAPH -> {
                    requireNonBlank(block.text(), "El párrafo no puede estar vacío");
                    requireMaxLength(block.text(), MAX_PARAGRAPH_CHARS, "El párrafo es demasiado largo");
                }
                case PostBlock.TYPE_HEADING -> {
                    requireNonBlank(block.text(), "El título no puede estar vacío");
                    requireMaxLength(block.text(), MAX_HEADING_CHARS, "El título es demasiado largo");
                }
                case PostBlock.TYPE_IMAGE, PostBlock.TYPE_GIF -> {
                    if (block.url() == null || !block.url().startsWith("https://")) {
                        throw new BadRequestException("Cada imagen/gif necesita una URL ya subida");
                    }
                    requireMaxLength(block.alt(), MAX_ALT_CHARS, "El texto alternativo es demasiado largo");
                }
                case PostBlock.TYPE_DIVIDER -> {
                    // Sin contenido que validar — solo marca una pausa visual.
                }
                default -> throw new BadRequestException("Tipo de bloque desconocido: " + block.type());
            }
            totalChars += length(block.text()) + length(block.url()) + length(block.alt());
        }
        if (totalChars > MAX_BLOCKS_TOTAL_CHARS) {
            throw new BadRequestException("El contenido de la publicación es demasiado extenso");
        }
    }

    static String deriveBodyFromBlocks(List<PostBlock> blocks) {
        return blocks.stream()
                .filter(b -> PostBlock.TYPE_PARAGRAPH.equals(b.type()) || PostBlock.TYPE_HEADING.equals(b.type()))
                .map(PostBlock::text)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private static void requireMaxLength(String value, int max, String message) {
        if (value != null && value.length() > max) {
            throw new BadRequestException(message);
        }
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
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
            notificationService.create(
                    post.getAuthor(),
                    NotificationCategory.likes,
                    me.getDisplayName() + " le dio like a tu publicación",
                    truncate(post.getBody()),
                    post, null, null, null);
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
        comment = commentRepository.saveAndFlush(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        if (!post.getAuthor().getId().equals(me.getId())) {
            notificationService.create(
                    post.getAuthor(),
                    NotificationCategory.comentarios,
                    me.getDisplayName() + " comentó tu publicación",
                    truncate(request.body()),
                    post, null, null, null);
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
                post.getCreatedAt(),
                post.getBlocks(),
                post.isHidden());
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
