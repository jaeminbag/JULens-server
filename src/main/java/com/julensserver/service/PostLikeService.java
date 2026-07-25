package com.julensserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.julensserver.domain.Post;
import com.julensserver.domain.PostLike;
import com.julensserver.domain.User;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.PostLikeRepository;
import com.julensserver.repository.PostRepository;
import com.julensserver.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class PostLikeService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    

    @Transactional
    public void addLike(Long postId, Long userId){
        if(postLikeRepository.existsByPost_IdAndUser_Id(postId, userId)){
            throw new BusinessException(ErrorCode.POST_ALREADY_LIKED);
        }

        Post post = postRepository.findById(postId)
                    .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                    .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PostLike postLike = new PostLike(user, post);

        postLikeRepository.save(postLike);
    
    }

    @Transactional
    public void deleteLike(Long postId, Long userId){
        PostLike postLike = postLikeRepository.findByPost_IdAndUser_Id(postId, userId)
                            .orElseThrow(()-> new BusinessException(ErrorCode.POST_LIKE_NOT_FOUND));

        
        postLikeRepository.delete(postLike);
        
    }
}
