package com.menzo.menzo.config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.Message;
import com.menzo.menzo.domain.chat.MessageType;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.community.CommunityEvent;
import com.menzo.menzo.domain.community.EventAttendee;
import com.menzo.menzo.domain.community.Notification;
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.post.Comment;
import com.menzo.menzo.domain.post.PollOption;
import com.menzo.menzo.domain.post.PollVote;
import com.menzo.menzo.domain.post.Post;
import com.menzo.menzo.domain.post.PostLike;
import com.menzo.menzo.domain.post.PostType;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.Interest;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.domain.user.UserBadge;
import com.menzo.menzo.domain.user.UserSettings;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.MessageRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.community.CommunityEventRepository;
import com.menzo.menzo.repository.community.EventAttendeeRepository;
import com.menzo.menzo.repository.community.NotificationRepository;
import com.menzo.menzo.repository.post.CommentRepository;
import com.menzo.menzo.repository.post.PollOptionRepository;
import com.menzo.menzo.repository.post.PollVoteRepository;
import com.menzo.menzo.repository.post.PostLikeRepository;
import com.menzo.menzo.repository.post.PostRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.InterestRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.repository.user.UserSettingsRepository;

/**
 * Seeds demo content for local testing. Only runs on the "dev" profile and only
 * if the database is empty, so it never touches a real deployment.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuraRepository auraRepository;
    private final InterestRepository interestRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final PostRepository postRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommunityEventRepository communityEventRepository;
    private final EventAttendeeRepository eventAttendeeRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            UserRepository userRepository,
            AuraRepository auraRepository,
            InterestRepository interestRepository,
            UserSettingsRepository userSettingsRepository,
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            MessageRepository messageRepository,
            PostRepository postRepository,
            PollOptionRepository pollOptionRepository,
            PollVoteRepository pollVoteRepository,
            PostLikeRepository postLikeRepository,
            CommentRepository commentRepository,
            CommunityEventRepository communityEventRepository,
            EventAttendeeRepository eventAttendeeRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auraRepository = auraRepository;
        this.interestRepository = interestRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.postRepository = postRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.communityEventRepository = communityEventRepository;
        this.eventAttendeeRepository = eventAttendeeRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String DEMO_PASSWORD = "Menzo123!";

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User emy = createUser("emy@menzo.dev", "emy", "Emy", "fuego", "fire",
                "Volví por los openings y me quedé por la gente.", List.of("anime", "musica", "nostalgia"));
        User dais = createUser("dais@menzo.dev", "dais", "Dais", "tormenta", "connection",
                "Sigo esperando que alguien recuerde mi antiguo apodo.", List.of("manga", "escritura", "nostalgia"));
        User ren = createUser("ren@menzo.dev", "ren", "Ren", "eclipse", "midnight",
                "Aquí desde la primera versión de todo esto.", List.of("arte", "videojuegos"));

        ChatRoom mainRoom = chatRoomRepository.findBySlug("main").orElseThrow();
        for (User user : List.of(emy, dais, ren)) {
            roomMemberRepository.save(new RoomMember(mainRoom.getId(), user.getId()));
        }

        Message welcomeMessage = new Message();
        welcomeMessage.setRoom(mainRoom);
        welcomeMessage.setType(MessageType.system);
        welcomeMessage.setBody("La sala del reencuentro está lista. Sé bienvenido.");
        messageRepository.save(welcomeMessage);

        Message chat = new Message();
        chat.setRoom(mainRoom);
        chat.setAuthor(emy);
        chat.setType(MessageType.text);
        chat.setBody("¿Alguien más está escuchando openings viejos ahorita?");
        messageRepository.save(chat);

        Post textPost = new Post();
        textPost.setAuthor(emy);
        textPost.setType(PostType.text);
        textPost.setBody("Esto lo escribí yo mil veces en mi cabeza y nunca lo publiqué. Volvimos.");
        textPost.setFeatured(true);
        textPost.setTags(new java.util.HashSet<>(List.of("nostalgia", "reencuentro")));
        textPost = postRepository.save(textPost);
        postLikeRepository.save(new PostLike(textPost.getId(), dais.getId()));
        postLikeRepository.save(new PostLike(textPost.getId(), ren.getId()));

        Comment comment = new Comment();
        comment.setPost(textPost);
        comment.setAuthor(dais);
        comment.setBody("Esto lo escribí yo mil veces en mi cabeza y nunca lo publiqué.");
        commentRepository.save(comment);
        textPost.setCommentCount(textPost.getCommentCount() + 1);
        postRepository.save(textPost);

        Post pollPost = new Post();
        pollPost.setAuthor(dais);
        pollPost.setType(PostType.poll);
        pollPost.setBody("¿Cuál openings escuchamos en la maratón de esta semana?");
        pollPost = postRepository.save(pollPost);
        PollOption optionA = pollOptionRepository.save(new PollOption(pollPost, "Openings 2005-2010", 0));
        pollOptionRepository.save(new PollOption(pollPost, "Openings 2011-2016", 1));
        pollVoteRepository.save(new PollVote(optionA.getId(), pollPost.getId(), ren.getId()));

        CommunityEvent event = new CommunityEvent();
        event.setTitle("Noche de openings y recuerdos");
        event.setDescription("Una maratón de openings viejos para recordar viejos tiempos, con chat en vivo.");
        event.setDate(LocalDate.now().plusDays(3));
        event.setTime("21:00");
        event.setKind("Encuentro");
        event.setCreatedBy(emy);
        event = communityEventRepository.save(event);
        eventAttendeeRepository.save(new EventAttendee(event.getId(), emy.getId()));
        eventAttendeeRepository.save(new EventAttendee(event.getId(), ren.getId()));

        Notification notification = new Notification();
        notification.setRecipient(emy);
        notification.setCategory(NotificationCategory.comentarios);
        notification.setTitle("Dais comentó tu publicación");
        notification.setBody("\"Esto lo escribí yo mil veces en mi cabeza y nunca lo publiqué.\"");
        notification.setRelatedPost(textPost);
        notificationRepository.save(notification);
    }

    private User createUser(
            String email, String username, String displayName, String auraId, String gradient,
            String bio, List<String> interestIds) {
        Aura aura = auraRepository.findById(auraId).orElseThrow();

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setDisplayName(displayName);
        user.setAvatarGradient(gradient);
        user.setAura(aura);
        user.setBio(bio);
        user.setStatusText("Acaba de regresar");
        user.setJoinedAt(Instant.now());
        user.setOnline(true);
        user.setLastActiveAt(Instant.now());
        user.setOnboardingCompleted(true);
        user.setLevel(3);
        user.setXp(1200);
        user.setReputation(80);

        for (String interestId : interestIds) {
            Interest interest = interestRepository.findById(interestId).orElseThrow();
            user.getInterests().add(interest);
        }
        user.getBadges().add(new UserBadge("recien-llegado", Instant.now()));

        user = userRepository.save(user);
        userSettingsRepository.save(new UserSettings(user.getId()));
        return user;
    }
}
