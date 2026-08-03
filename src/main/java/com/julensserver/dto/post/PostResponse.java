package com.julensserver.dto.post;

import com.julensserver.domain.Post;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostResponse {
    private Long postId;
    private Long userId;
    private String title;
    private String content;
    private String nickname;
    private long likeCount;
    private boolean liked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse from(Post post, long likeCount, boolean liked) {
        return new PostResponse(
                post.getId(),
                post.getUser().getId(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getNickname(),
                likeCount,
                liked,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}


