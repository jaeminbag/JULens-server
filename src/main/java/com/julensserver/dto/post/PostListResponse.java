package com.julensserver.dto.post;

import com.julensserver.domain.Post;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostListResponse {

    private static final int CONTENT_PREVIEW_LENGTH = 100;

    private Long postId;
    private String title;
    private String contentPreview;
    private Long authorId;
    private String authorNickname;
    private LocalDateTime createdAt;

    public static PostListResponse from(Post post) {
        String contentPreview = createContentPreview(post.getContent());

        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                contentPreview,
                post.getUser().getId(),
                post.getUser().getNickname(),
                post.getCreatedAt()
        );
    }

    private static String createContentPreview(String content){
        if (content.length() <= CONTENT_PREVIEW_LENGTH){
            return content;
        }

        return content.substring(0, CONTENT_PREVIEW_LENGTH)+"...";
    }

}
