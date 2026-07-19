package com.menzo.menzo.domain.community;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "community_config")
@Getter
@Setter
@NoArgsConstructor
public class CommunityConfig {

    @Id
    private short id = 1;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String subtitle;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 200)
    private String motto;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "community_config_tags", joinColumns = @JoinColumn(name = "config_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "tag", length = 40)
    private List<String> tags = new ArrayList<>();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
