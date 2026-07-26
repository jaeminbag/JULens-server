package com.julensserver.repository;

import com.julensserver.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(
            value = """
                    SELECT p AS post, COUNT(pl) AS likeCount
                    FROM Post P
                    LEFT JOIN PostLike pl ON pl.post = p
                    GROUP BY p
                    """,
            countQuery = """
                        SELECT count(p)
                        from Post p
                        """
    )
    Page<PostWithLikeCount> findAllWithLikeCount(Pageable pageable);

    @Query(
            value = """
                    SELECT p AS post, COUNT(pl) AS likeCount
                    FROM Post p 
                    LEFT JOIN PostLike pl ON pl.post = p
                    GROUP BY p
                    HAVING COUNT(pl)>=10
                    ORDER BY COUNT(pl) DESC 
                    """,
            countQuery = """
                         SELECT COUNT(p)
                         FROM Post p
                         WHERE (
                              SELECT COUNT(pl2)
                              FROM PostLike pl2
                              WHERE pl2.post=p
                               )>=10
                         """
    )
    Page<PostWithLikeCount> findPopularPosts(Pageable pageable);
}
