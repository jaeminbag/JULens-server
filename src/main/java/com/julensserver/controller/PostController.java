package com.julensserver.controller;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.dto.post.PostCreateRequest;
import com.julensserver.dto.post.PostListResponse;
import com.julensserver.dto.post.PostResponse;
import com.julensserver.dto.post.PostUpdateRequest;
import com.julensserver.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService){
        this.postService=postService;
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id, @AuthenticationPrincipal Long userId){
        PostResponse postResponse = postService.getPostById(id, userId);

        return ApiResponse.success("게시물 조회에 성공했습니다.", postResponse);
    }

    @GetMapping
    public ApiResponse<Page<PostListResponse>> getPosts(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable){
        Page<PostListResponse> postListResponses = postService.getPosts(pageable);

        return ApiResponse.success("게시물 목록 조회에 성공했습니다.", postListResponses);
    }

    @PostMapping
    public ApiResponse<PostResponse> createPost(@AuthenticationPrincipal Long userId, @RequestBody @Valid PostCreateRequest postCreateRequest){
        PostResponse postResponse = postService.createPost(userId, postCreateRequest);

        return ApiResponse.success("게시물 작성에 성공했습니다.", postResponse);
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePostById(@PathVariable Long postId, @AuthenticationPrincipal Long userId, @RequestBody @Valid PostUpdateRequest postUpdateRequest){
        PostResponse postResponse = postService.updatePostById(postId,userId,postUpdateRequest);

        return ApiResponse.success("게시물 수정에 성공했습니다.", postResponse);
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePostById(@PathVariable Long postId, @AuthenticationPrincipal Long userId){
        postService.deletePostById(postId,userId);

        return ApiResponse.success("게시물 삭제에 성공했습니다.");
    }

    @GetMapping("/popular")
    public ApiResponse<Page<PostListResponse>> getPopularPosts(
            @PageableDefault(size = 20) Pageable pageable
    ){
        Page<PostListResponse> postListResponses = postService.getPopularPosts(pageable);
        return ApiResponse.success("인기 게시물 조회에 성공했습니다.", postListResponses);
    }

}

