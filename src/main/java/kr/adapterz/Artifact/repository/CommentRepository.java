package kr.adapterz.Artifact.repository;


import kr.adapterz.Artifact.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    //댓글이 달린 게시글id
    List<Comment> findByPost_Id(Long postId);
    //해당 게시글에 달린 댓글수
    long countByPost_Id(Long postId);


    //게시글별로 댓글수
    @Query("""
       select c.post.id, count(c)
       from Comment c
       where c.post.id in :postIds
       group by c.post.id
       """)
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);


    // 삭제관련, 작성한 게시글, 사용자
    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
    @Modifying
    @Query("delete from Comment c where c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
