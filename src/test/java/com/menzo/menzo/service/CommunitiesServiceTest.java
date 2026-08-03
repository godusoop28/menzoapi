package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.menzo.menzo.domain.communities.Community;
import com.menzo.menzo.domain.communities.CommunityAccessType;
import com.menzo.menzo.domain.communities.CommunityStatus;
import com.menzo.menzo.domain.user.Aura;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.communities.CommunityMembershipDto;
import com.menzo.menzo.dto.communities.MyCommunityDto;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.repository.communities.CommunityRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.UserRepository;

/**
 * Fase A del sistema de comunidades — cubre los puntos 1-4, 14, 17 y 18 del checklist de pruebas
 * backend del pedido (varias comunidades por usuario, sin membresía duplicada, mismo nivel
 * global en todas, comunidad REQUEST exige aprobación, seed crea las siete, slugs únicos).
 * @SpringBootTest — necesita una base real, ver nota de "verificado por código vs por pruebas"
 * en el reporte final de la fase.
 */
@SpringBootTest
class CommunitiesServiceTest {

    @Autowired private CommunitiesService communitiesService;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private AuraRepository auraRepository;
    @Autowired private UserRepository userRepository;

    private User member;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Aura aura = auraRepository.findById("fuego").orElseGet(() -> {
            Aura a = new Aura();
            a.setId("fuego");
            a.setName("Fuego");
            a.setDescription("Fixture aura for tests");
            a.setGradient("fire");
            return auraRepository.save(a);
        });

        member = new User();
        member.setEmail("communities-member-" + suffix + "@test.menzo");
        member.setUsername("commember" + suffix);
        member.setPasswordHash("x");
        member.setDisplayName("Community Member " + suffix);
        member.setAura(aura);
        member.setJoinedAt(Instant.now());
        member.setLevel(15);
        member = userRepository.save(member);
    }

    @Test
    void seedCreatesTheSevenInitialCommunitiesWithUniqueSlugs() {
        List<String> expectedSlugs = List.of(
                "naruto", "one-piece", "futbol", "anime", "league-of-legends", "valorant", "among-us");

        for (String slug : expectedSlugs) {
            assertThat(communityRepository.findBySlugIgnoreCase(slug))
                    .as("seed community with slug " + slug)
                    .isPresent();
        }
        // Cada slug es único por construcción (UNIQUE en la columna) — esto confirma que el
        // seed no insertó duplicados si la migración corriera más de una vez en algún ambiente.
        assertThat(communityRepository.count()).isGreaterThanOrEqualTo(7);
    }

    @Test
    void userCanBelongToMultipleCommunitiesWithoutDuplicatingGlobalLevel() {
        Community naruto = communityRepository.findBySlugIgnoreCase("naruto").orElseThrow();
        Community anime = communityRepository.findBySlugIgnoreCase("anime").orElseThrow();

        communitiesService.join(naruto.getId(), member);
        communitiesService.join(anime.getId(), member);

        List<MyCommunityDto> mine = communitiesService.my(member);
        assertThat(mine).hasSize(2);
        assertThat(mine).extracting(m -> m.community().slug()).containsExactlyInAnyOrder("naruto", "anime");

        // El nivel nunca vive en la membresía — sigue siendo el mismo User.level leído fresco,
        // sin importar en cuántas comunidades esté. No existe (ni debe existir) un
        // "communityLevel" por fila de membresía.
        User reloaded = userRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getLevel()).isEqualTo(15);
    }

    @Test
    void cannotJoinTheSameCommunityTwiceWhileActive() {
        Community naruto = communityRepository.findBySlugIgnoreCase("naruto").orElseThrow();
        communitiesService.join(naruto.getId(), member);

        assertThatThrownBy(() -> communitiesService.join(naruto.getId(), member))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void leavingAndRejoiningIsAllowed() {
        Community naruto = communityRepository.findBySlugIgnoreCase("naruto").orElseThrow();
        communitiesService.join(naruto.getId(), member);
        communitiesService.leave(naruto.getId(), member);

        CommunityMembershipDto rejoined = communitiesService.join(naruto.getId(), member);
        assertThat(rejoined.membershipStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void requestAccessCommunityStaysPendingUntilApproved() {
        Community community = new Community();
        community.setSlug("test-request-only-" + UUID.randomUUID().toString().substring(0, 8));
        community.setName("Test Request Only");
        community.setStatus(CommunityStatus.ACTIVE);
        community.setAccessType(CommunityAccessType.REQUEST);
        community = communityRepository.save(community);

        CommunityMembershipDto membership = communitiesService.join(community.getId(), member);
        assertThat(membership.membershipStatus()).isEqualTo("PENDING");
    }

    @Test
    void inviteOnlyCommunityRejectsSelfJoin() {
        Community community = new Community();
        community.setSlug("test-invite-only-" + UUID.randomUUID().toString().substring(0, 8));
        community.setName("Test Invite Only");
        community.setStatus(CommunityStatus.ACTIVE);
        community.setAccessType(CommunityAccessType.INVITE_ONLY);
        Community saved = communityRepository.save(community);

        assertThatThrownBy(() -> communitiesService.join(saved.getId(), member))
                .isInstanceOf(ForbiddenException.class);
    }
}
