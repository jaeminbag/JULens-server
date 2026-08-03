package com.julensserver.repository;

import com.julensserver.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 모든 게시글을 좋아요 수와 댓글 수를 포함해 조회한다.
    @Query(
            value = """
                    SELECT p AS post,
                           COUNT(DISTINCT pl) AS likeCount,
                           COUNT(DISTINCT c) AS commentCount
                    FROM Post p
                    LEFT JOIN PostLike pl ON pl.post = p
                    LEFT JOIN Comment c ON c.post = p
                    GROUP BY p
                    """,
            // 페이징에 필요한 전체 게시글 수를 계산한다.
            countQuery = """
                    SELECT COUNT(p)
                    FROM Post p
                    """
    )
    Page<PostWithLikeCount> findAllWithLikeCount(Pageable pageable);

    // 좋아요가 10개 이상인 게시글을 좋아요 수 내림차순으로 조회한다.
    @Query(
            value = """
                    SELECT p AS post,
                           COUNT(DISTINCT pl) AS likeCount,
                           COUNT(DISTINCT c) AS commentCount
                    FROM Post p
                    LEFT JOIN PostLike pl ON pl.post = p
                    LEFT JOIN Comment c ON c.post = p
                    GROUP BY p
                    HAVING COUNT(DISTINCT pl) >= 10
                    ORDER BY COUNT(DISTINCT pl) DESC
                    """,
            // 인기글 조건을 만족하는 게시글 수를 계산한다.
            countQuery = """
                    SELECT COUNT(p)
                    FROM Post p
                    WHERE (
                        SELECT COUNT(pl2)
                        FROM PostLike pl2
                        WHERE pl2.post = p
                    ) >= 10
                    """
    )
    Page<PostWithLikeCount> findPopularPosts(Pageable pageable);
}