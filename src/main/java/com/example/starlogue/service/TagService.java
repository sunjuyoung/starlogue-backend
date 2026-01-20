package com.example.starlogue.service;

import com.example.starlogue.domain.Tag;
import com.example.starlogue.domain.User;
import com.example.starlogue.repository.TagRepository;
import com.example.starlogue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    // 기본 색상 팔레트 (별 색상)
    private static final String[] DEFAULT_COLORS = {
            "#FF6B6B", // 빨강 (열정)
            "#4ECDC4", // 청록 (집중)
            "#45B7D1", // 하늘 (창의)
            "#96CEB4", // 민트 (휴식)
            "#FFEAA7", // 노랑 (에너지)
            "#DDA0DD", // 보라 (예술)
            "#98D8C8", // 연두 (성장)
            "#F7DC6F"  // 금색 (성취)
    };

    /**
     * 태그 생성
     */
    @Transactional
    public Tag createTag(UUID userId, String name, String colorHex, String icon) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 중복 체크
        if (tagRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다: " + name);
        }

        // 색상이 없으면 기본 색상 할당
        if (colorHex == null || colorHex.isBlank()) {
            int tagCount = tagRepository.findByUserIdOrderByUsageCountDesc(userId).size();
            colorHex = DEFAULT_COLORS[tagCount % DEFAULT_COLORS.length];
        }

        Tag tag = Tag.builder()
                .user(user)
                .name(name)
                .colorHex(colorHex)
                .icon(icon)
                .build();

        return tagRepository.save(tag);
    }

    /**
     * 사용자의 활성 태그 목록 조회
     */
    public List<Tag> getActiveTags(UUID userId) {
        return tagRepository.findByUserIdAndIsActiveTrueOrderByUsageCountDesc(userId);
    }

    /**
     * 사용자의 모든 태그 조회 (비활성 포함)
     */
    public List<Tag> getAllTags(UUID userId) {
        return tagRepository.findByUserIdOrderByUsageCountDesc(userId);
    }

    /**
     * 태그 조회
     */
    public Tag getTag(UUID tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("태그를 찾을 수 없습니다: " + tagId));
    }

    /**
     * 이름으로 태그 조회 (없으면 생성)
     */
    @Transactional
    public Tag getOrCreateTag(UUID userId, String name) {
        return tagRepository.findByUserIdAndName(userId, name)
                .orElseGet(() -> createTag(userId, name, null, null));
    }

    /**
     * 태그 업데이트
     */
    @Transactional
    public Tag updateTag(UUID tagId, String name, String colorHex, String icon) {
        Tag tag = getTag(tagId);

        // 이름 변경 시 중복 체크
        if (!tag.getName().equals(name) &&
                tagRepository.existsByUserIdAndName(tag.getUser().getId(), name)) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다: " + name);
        }

        tag.update(name, colorHex, icon);
        return tag;
    }

    /**
     * 태그 사용 횟수 증가 (세션 시작 시 호출)
     */
    @Transactional
    public void incrementUsageCount(UUID tagId) {
        Tag tag = getTag(tagId);
        tag.incrementUsageCount();
    }

    /**
     * 태그 비활성화 (삭제 대신)
     */
    @Transactional
    public void deactivateTag(UUID tagId) {
        Tag tag = getTag(tagId);
        tag.deactivate();
    }

    /**
     * 태그 재활성화
     */
    @Transactional
    public void activateTag(UUID tagId) {
        Tag tag = getTag(tagId);
        tag.activate();
    }

    /**
     * 가장 많이 사용된 태그 조회
     */
    public List<Tag> getTopUsedTags(UUID userId, int limit) {
        return tagRepository.findTopUsedTags(userId, limit);
    }

    /**
     * 기본 태그 세트 생성 (신규 사용자용)
     */
    @Transactional
    public void createDefaultTags(UUID userId) {
        String[][] defaults = {
                {"수학", "#FF6B6B", "📐"},
                {"영어", "#4ECDC4", "📚"},
                {"코딩", "#45B7D1", "💻"},
                {"독서", "#96CEB4", "📖"},
                {"기타", "#FFEAA7", "✨"}
        };

        for (String[] tagInfo : defaults) {
            try {
                createTag(userId, tagInfo[0], tagInfo[1], tagInfo[2]);
            } catch (IllegalArgumentException ignored) {
                // 이미 존재하면 무시
            }
        }
    }
}