package com.julensserver.controller;

import com.julensserver.dto.comment.CommentCreateRequest;
import com.julensserver.dto.comment.CommentResponse;
import com.julensserver.dto.comment.CommentUpdateRequest;
import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.post.PostUpdateRequest;
import com.julensserver.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService=commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> createCommentByPostId(@PathVariable Long postId, @AuthenticationPrincipal Long userId, @Valid @RequestBody CommentCreateRequest commentCreateRequest){
        CommentResponse commentResponse = commentService.createCommentByPostId(postId,userId, commentCreateRequest);

        return ApiResponse.success("댓글 작성에 성공했습니다.", commentResponse);
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getCommentsByPostId(@PathVariable Long postId){
        List<CommentResponse> commentResponse = commentService.getCommentByPostId(postId);

        return ApiResponse.success("댓글 조회에 성공했습니다.", commentResponse);
    }

    @PutMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateCommentById(@PathVariable Long commentId, @AuthenticationPrincipal Long userId,
        @Valid @RequestBody CommentUpdateRequest commentUpdateRequest){
            CommentResponse commentResponse = commentService.updateCommentById(commentId, userId, commentUpdateRequest);

            return ApiResponse.success("댓글 수정에 성공했습니다.", commentResponse);
        }
    
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteCommentById(@PathVariable Long commentId, @AuthenticationPrincipal Long userId){
        commentService.deleteCommentById(commentId, userId);

        return ApiResponse.success("댓글 삭제에 성공했습니다.");
    }    


}
