package com.julensserver.service;

import com.julensserver.domain.Post;
import com.julensserver.domain.User;
import com.julensserver.dto.post.PostCreateRequest;
import com.julensserver.dto.post.PostListResponse;
import com.julensserver.dto.post.PostResponse;
import com.julensserver.dto.post.PostUpdateRequest;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.CommentRepository;
import com.julensserver.repository.PostLikeRepository;
import com.julensserver.repository.PostRepository;
import com.julensserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;


    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, Long userId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new BusinessException(ErrorCode.POST_NOT_FOUND));

        return PostResponse.from(post, postLikeRepository.countByPost_Id(postId), postLikeRepository.existsByPost_IdAndUser_Id(postId, userId));
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getPosts(Pageable pageable){
        return postRepository.findAllWithLikeCount(pageable)
                .map(p->PostListResponse.from(p.getPost(),p.getLikeCount(), p.getCommentCount()));

    }

    @Transactional
    public PostResponse createPost(Long userId, PostCreateRequest postCreateRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = new Post(user, postCreateRequest.getTitle(), postCreateRequest.getContent());

        Post savedPost = postRepository.save(post);
        Long postId = savedPost.getId();
        return PostResponse.from(savedPost, postLikeRepository.countByPost_Id(postId), postLikeRepository.existsByPost_IdAndUser_Id(postId,userId));
    }

    @Transactional
    public PostResponse updatePostById(Long postId, Long userId, PostUpdateRequest postUpdateRequest){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if(!post.isWrittenBy(userId)){
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        post.update(postUpdateRequest.getTitle(), postUpdateRequest.getContent());

        return PostResponse.from(post, postLikeRepository.countByPost_Id(postId), postLikeRepository.existsByPost_IdAndUser_Id(postId,userId));
    }

    @Transactional
    public void deletePostById(Long postId, Long userId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if(!post.isWrittenBy(userId)){
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
        
        commentRepository.deleteAllByPost_Id(postId);
        postLikeRepository.deleteAllByPost_Id(postId);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponse> getPopularPosts(Pageable pageable){
        return postRepository.findPopularPosts(pageable)
                .map(p->PostListResponse.from(p.getPost(), p.getLikeCount(), p.getCommentCount()));
    }
}
