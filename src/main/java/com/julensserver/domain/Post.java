package com.julensserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Post(User user, String title, String content){
        this.user=user;
        this.title=title;
        this.content=content;
        this.createdAt=LocalDateTime.now();
        this.updatedAt=LocalDateTime.now();
    }

    public void update(String title, String content){
        this.title=title;
        this.content=content;
        this.updatedAt=LocalDateTime.now();
    }

    public boolean isWrittenBy(Long userId){
        return this.user.getId().equals(userId);
    }

}
