package com.julensserver.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.julensserver.dto.common.ApiResponse;
import com.julensserver.service.PostLikeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostLikeController {
    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/likes")
    public ApiResponse<Void> addLike(@PathVariable Long postId, @AuthenticationPrincipal Long userId){
        
    }

}
