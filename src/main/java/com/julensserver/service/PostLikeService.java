package com.julensserver.service;

import org.springframework.stereotype.Service;

import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.PostLikeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;

    public void addLike(Long postId, Long userId){
        if(postLikeRepository.existsByPost_IdAndUser_Id(postId, userId)){
            throw new BusinessException(ErrorCode.POST_ALREADY_LIKED);
        }
    }
}
