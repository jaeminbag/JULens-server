package com.julensserver.repository;

import com.julensserver.domain.Post;

public interface PostWithLikeCount {
    Post getPost();
    Long getLikeCount();
}
