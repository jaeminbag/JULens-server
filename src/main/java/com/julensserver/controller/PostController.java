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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService){
        this.postService=postService;
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id){
        PostResponse postResponse = postService.getPostById(id);

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
    public ApiResponse<PostResponse> createPost(@RequestBody @Valid PostCreateRequest postCreateRequest){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        PostResponse postResponse = postService.createPost(userId, postCreateRequest);

        return ApiResponse.success("게시물 작성에 성공했습니다.", postResponse);
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePostById(Long postId, @RequestBody @Valid PostUpdateRequest postUpdateRequest){
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        PostResponse postResponse = postService.updatePostById(postId,userId,postUpdateRequest);

        return ApiResponse.success("게시물 수정에 성공했습니다.", postResponse);
    }

}

