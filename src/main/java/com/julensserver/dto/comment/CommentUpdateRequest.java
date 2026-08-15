package com.julensserver.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentUpdateRequest {
    @NotBlank(message = "댓글은 비어 있을 수 없습니다.")
    @Size(max = 500, message = "댓글 최대 크기는 500 입니다.")
    private String content;
}
