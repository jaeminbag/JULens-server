package com.julensserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.julensserver.domain.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<PostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    long countByPost_Id(Long postId);
}
