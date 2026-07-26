package com.julensserver.service;

import com.julensserver.domain.Post;
import com.julensserver.domain.User;
import com.julensserver.dto.post.PostCreateRequest;
import com.julensserver.dto.post.PostListResponse;
import com.julensserver.dto.post.PostResponse;
import com.julensserver.dto.post.PostUpdateRequest;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.PostRepository;
import com.julensserver.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository){
        this.postRepository=postRepository;
        this.userRepository=userRepository;
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new BusinessException(ErrorCode.POST_NOT_FOUND));

        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getPosts(Pageable pageable){
        return postRepository.findAllWithLikeCount(pageable)
                .map(p->PostListResponse.from(p.getPost(),p.getLikeCount()));

    }

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest postCreateRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = new Post(user, postCreateRequest.getTitle(), postCreateRequest.getContent());

        Post savedPost = postRepository.save(post);

        return PostResponse.from(savedPost);
    }

    @Transactional
    public PostResponse updatePostById(Long postId, Long userId, PostUpdateRequest postUpdateRequest){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if(!post.isWrittenBy(userId)){
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        post.update(postUpdateRequest.getTitle(), postUpdateRequest.getContent());

        return PostResponse.from(post);
    }

    @Transactional
    public void deletePostById(Long postId, Long userId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if(!post.isWrittenBy(userId)){
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getPopularPosts(Pageable pageable){
        return postRepository.findPopularPosts(pageable)
                .map(p->PostListResponse.from(p.getPost(), p.getLikeCount()));
    }
}
