package com.julensserver.repository;

import com.julensserver.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 모든 게시글을 조회하면서 게시글별 좋아요 수를 함께 계산한다.
    // LEFT JOIN이라 좋아요가 0개인 게시글도 결과에 포함된다.
    @Query(
            value = """
                    SELECT p AS post, COUNT(pl) AS likeCount
                    FROM Post p
                    LEFT JOIN PostLike pl ON pl.post = p
                    GROUP BY p
                    ORDER BY p.createdAt DESC
                    """,
            // 페이징 전체 개수는 게시글 수 기준으로 센다.
            countQuery = """
                    SELECT COUNT(p)
                    FROM Post p
                    """
    )
    Page<PostWithLikeCount> findAllWithLikeCountOrderByLatest(Pageable pageable);

    // 좋아요 개수와 관계없이 모든 게시글을 좋아요가 많은 순서로 조회한다.
    // 좋아요 수가 같으면 더 최근에 작성된 게시글을 먼저 표시한다.
    @Query(
            value = """
                    SELECT p AS post, COUNT(pl) AS likeCount
                    FROM Post p
                    LEFT JOIN PostLike pl ON pl.post = p
                    GROUP BY p
                    ORDER BY COUNT(pl) DESC, p.createdAt DESC
                    """,
            // 좋아요 수에 따른 제외 조건이 없으므로 전체 게시글 수를 센다.
            countQuery = """
                    SELECT COUNT(p)
                    FROM Post p
                    """
    )
    Page<PostWithLikeCount> findAllWithLikeCountOrderByLikes(Pageable pageable);

    // 좋아요가 10개 이상인 인기글만, 좋아요 수 내림차순으로 조회한다.
    @Query(
            value = """
                    SELECT p AS post, COUNT(pl) AS likeCount
                    FROM Post p
                    LEFT JOIN PostLike pl ON pl.post = p
                    GROUP BY p
                    HAVING COUNT(pl) >= 10
                    ORDER BY COUNT(pl) DESC, p.createdAt DESC
                    """,
            // 인기글 조건을 만족하는 게시글 수를 페이징용으로 계산한다.
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