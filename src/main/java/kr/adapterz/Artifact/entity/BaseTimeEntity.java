package kr.adapterz.Artifact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

// 생성, 수정 기록 엔티지
// 추상메소드로 만들고 상속 받으면 생성, 수정 기록 표시
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {
    // 중요: 모든 영속 엔티티는 생성 시각과 수정 시각을 반드시 가집니다.
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
