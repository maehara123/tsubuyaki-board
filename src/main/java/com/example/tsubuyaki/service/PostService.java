package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.PostLike;
import com.example.tsubuyaki.repository.PostLikeRepository;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.web.dto.PostDto;
import com.example.tsubuyaki.web.dto.PostDetailDto;
import com.example.tsubuyaki.web.dto.PostForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository repository;
    private final PostLikeRepository likeRepository;
    private final ClientHashGenerator clientHashGenerator;

    public PostService(
            PostRepository repository,
            PostLikeRepository likeRepository,
            ClientHashGenerator clientHashGenerator) {
        this.repository = repository;
        this.likeRepository = likeRepository;
        this.clientHashGenerator = clientHashGenerator;
    }

    public List<PostDto> latest() {
        List<Post> posts = repository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();
        return toDtoList(posts);
    }

    public List<PostDto> search(String query) {
        if (query == null || query.isBlank()) {
            return latest();
        }
        List<Post> posts = repository.findTop50ByDeletedAtIsNullAndBodyContainingOrderByCreatedAtDesc(query);
        return toDtoList(posts);
    }

    private List<PostDto> toDtoList(List<Post> posts) {
        if (posts == null) {
            return List.of();
        }
        return posts.stream()
                .map(PostDto::from)
                .toList();
    }

    @Transactional
    public void create(PostForm form) {
        repository.save(new Post(
                form.getAuthor(),
                form.getBody(),
                Instant.now(),
                normalizedAvatarColor(form.getAvatarColor())));
    }

    private String normalizedAvatarColor(String avatarColor) {
        return avatarColor == null || avatarColor.isBlank() ? null : avatarColor;
    }

    public Optional<PostDto> findById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id).map(PostDto::from);
    }

    public PostDetailDto getDetail(Long postId, String ipAddress, String userAgent) {
        return getDetailByClientHash(postId, clientHashGenerator.generate(ipAddress, userAgent));
    }

    private PostDetailDto getDetailByClientHash(Long postId, String clientHash) {
        Post post = repository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        return new PostDetailDto(
                PostDto.from(post),
                countLikes(postId),
                hasLiked(postId, clientHash));
    }

    @Transactional
    public void toggleLike(Long postId, String ipAddress, String userAgent) {
        toggleLike(postId, clientHashGenerator.generate(ipAddress, userAgent));
    }

    @Transactional
    public void toggleLike(Long postId, String clientHash) {
        Post post = repository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        Optional<PostLike> existingLike = likeRepository.findByPostIdAndClientHash(postId, clientHash);
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            return;
        }
        likeRepository.save(new PostLike(post, clientHash, Instant.now()));
    }

    public long countLikes(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    public boolean hasLiked(Long postId, String clientHash) {
        return likeRepository.findByPostIdAndClientHash(postId, clientHash).isPresent();
    }

    @Transactional
    public void delete(Long postId) {
        Post post = repository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.markDeleted(Instant.now());
    }
}
