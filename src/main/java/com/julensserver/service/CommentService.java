package com.julensserver.service;

import com.julensserver.domain.Comment;
import com.julensserver.domain.Post;
import com.julensserver.domain.User;
import com.julensserver.dto.comment.CommentCreateRequest;
import com.julensserver.dto.comment.CommentResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.repository.CommentRepository;
import com.julensserver.repository.PostRepository;
import com.julensserver.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, UserRepository userRepository, PostRepository postRepository){
        this.commentRepository=commentRepository;
        this.userRepository=userRepository;
        this.postRepository=postRepository;
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentCreateRequest commentCreateRequest){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Comment comment = new Comment(user,post, commentCreateRequest.getContent());

        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.from(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentByPostId(Long postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return commentRepository.findAllByPost_IdOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

}
