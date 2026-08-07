package kr.adapterz.Artifact.repository;


import kr.adapterz.Artifact.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 게시글 엔티티의 JPA 저장소입니다. */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    //작성한 유저 조회
    //p.user.id -> 게시글이.지고있는 유저.유저id
    List<Post> findByUser_Id(Long userId);

    // 고정 여부는 일반 목록의 제외 조건이 아닙니다. 이 조회는 상단 바로가기만 별도로 구성합니다.
    List<Post> findTop2ByPinnedTrueOrderByPinnedAtDescIdDesc();

    long countByPinnedTrue();

    //삭제관련
    //@Modifying 벌크 연겟
    @Modifying
    @Query("delete from Post p where p.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
