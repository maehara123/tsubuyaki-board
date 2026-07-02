package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("投稿一覧_51件あるとき_最新50件を新着順で返す")
    void findTop50ByOrderByCreatedAtDesc_when51PostsExist_returnsLatest50InNewestOrder() {
        Instant baseTime = Instant.parse("2026-05-23T09:00:00Z");
        List<Post> posts = IntStream.rangeClosed(1, 51)
                .mapToObj(number -> new Post(
                        "user" + number,
                        "投稿" + number,
                        baseTime.plusSeconds(number)))
                .toList();
        postRepository.saveAllAndFlush(posts);

        List<Post> actual = postRepository.findTop50ByOrderByCreatedAtDesc();

        assertThat(actual)
                .hasSize(50)
                .extracting(Post::getBody)
                .containsExactlyElementsOf(
                        IntStream.iterate(51, number -> number - 1)
                                .limit(50)
                                .mapToObj(number -> "投稿" + number)
                                .toList());
    }

    @Test
    @DisplayName("投稿作成_avatarColorあり_投稿と一緒に保存できる")
    void save_whenAvatarColorSelected_persistsAvatarColor() {
        Post post = postRepository.saveAndFlush(new Post(
                "alice",
                "アバター色つき投稿",
                Instant.parse("2026-05-23T10:00:00Z"),
                "blue"));

        Post actual = postRepository.findById(post.getId()).orElseThrow();

        assertThat(actual.getAvatarColor()).isEqualTo("blue");
    }

    @Test
    @DisplayName("投稿検索_本文にキーワードを含む投稿だけを新着順で返す")
    void findTop50ByBodyContainingOrderByCreatedAtDesc_whenBodyContainsKeyword_returnsMatchingPosts() {
        Post olderMatchedPost = new Post("alice", "検索できる本文", Instant.parse("2026-05-23T09:00:00Z"));
        Post unmatchedPost = new Post("bob", "関係ない本文", Instant.parse("2026-05-23T10:00:00Z"));
        Post newerMatchedPost = new Post("carol", "あとから検索した本文", Instant.parse("2026-05-23T11:00:00Z"));
        postRepository.saveAllAndFlush(List.of(olderMatchedPost, unmatchedPost, newerMatchedPost));

        List<Post> actual = postRepository.findTop50ByBodyContainingOrderByCreatedAtDesc("検索");

        assertThat(actual)
                .extracting(Post::getBody)
                .containsExactly("あとから検索した本文", "検索できる本文");
    }
}
