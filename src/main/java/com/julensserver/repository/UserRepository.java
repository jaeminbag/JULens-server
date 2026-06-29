package com.julensserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.julensserver.domain.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    
}
