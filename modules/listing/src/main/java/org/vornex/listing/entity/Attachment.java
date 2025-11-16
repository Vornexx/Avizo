package org.vornex.listing.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "attachments")
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // порядок изображений
    @Column(name = "position")
    private Integer position;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "processed")
    private boolean processed = false; // for thumbnails/scan
}
