package com.dibya.knowledgehub.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "document_metadata")
@Getter
@Setter
@NoArgsConstructor
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(length = 10)
    private String language;

    @Column(length = 500)
    private String title;

    @Column(length = 255)
    private String author;
}
