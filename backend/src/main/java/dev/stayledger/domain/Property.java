package dev.stayledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "properties")
public class Property {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 4000)
    private String description;

    @Column(name = "house_rules", length = 4000)
    private String houseRules;

    @Column(length = 4000)
    private String amenities;

    @Column(name = "check_in_info", length = 4000)
    private String checkInInfo;

    @Column(name = "check_out_info", length = 4000)
    private String checkOutInfo;

    @Column(name = "wifi_network", length = 255)
    private String wifiNetwork;

    @Column(name = "wifi_password", length = 255)
    private String wifiPassword;

    @Column(name = "faq_notes", length = 4000)
    private String faqNotes;

    @Column(name = "export_token", nullable = false, unique = true)
    private UUID exportToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (exportToken == null) {
            exportToken = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
