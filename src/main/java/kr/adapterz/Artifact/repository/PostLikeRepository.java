package kr.adapterz.Artifact.repository;

import kr.adapterz.Artifact.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);

    Optional<PostLike> findByUser_IdAndPost_Id(Long userId, Long postId);

    @Modifying
    @Query("delete from PostLike pl where pl.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("delete from PostLike pl where pl.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
