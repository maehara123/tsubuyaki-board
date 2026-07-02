package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.PostLike;
import com.example.tsubuyaki.repository.PostLikeRepository;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.web.dto.PostDto;
import com.example.tsubuyaki.web.dto.PostDetailDto;
import com.example.tsubuyaki.web.dto.PostForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private ClientHashGenerator clientHashGenerator;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("投稿一覧_投稿がないとき_空リストを返す")
    void latest_whenNoPosts_returnsEmpty() {
        given(postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of());

        List<PostDto> actual = postService.latest();

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("投稿一覧_最新投稿があるとき_Repositoryから最新50件を新着順で取得する")
    void latest_whenPostsExist_returnsLatest50PostsInNewestOrder() {
        Post newerPost = new Post("alice", "新しい投稿", Instant.parse("2026-05-23T10:00:00Z"));
        Post olderPost = new Post("bob", "古い投稿", Instant.parse("2026-05-23T09:00:00Z"));
        List<Post> latestPosts = List.of(newerPost, olderPost);
        given(postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(latestPosts);

        List<PostDto> actual = postService.latest();

        assertThat(actual)
                .extracting(PostDto::id, PostDto::author, PostDto::body, PostDto::createdAt)
                .containsExactly(
                        tuple(newerPost.getId(), newerPost.getAuthor(), newerPost.getBody(), newerPost.getCreatedAt()),
                        tuple(olderPost.getId(), olderPost.getAuthor(), olderPost.getBody(), olderPost.getCreatedAt()));
    }

    @Test
    @DisplayName("投稿検索_q指定_本文部分一致検索の結果を返す")
    void search_whenQueryGiven_returnsBodySearchResults() {
        Post matchedPost = new Post("alice", "検索できる本文", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findTop50ByDeletedAtIsNullAndBodyContainingOrderByCreatedAtDesc("検索"))
                .willReturn(List.of(matchedPost));

        List<PostDto> actual = postService.search("検索");

        assertThat(actual)
                .extracting(PostDto::author, PostDto::body, PostDto::createdAt)
                .containsExactly(tuple("alice", "検索できる本文", Instant.parse("2026-05-23T10:00:00Z")));
        then(postRepository).should(never()).findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_q未指定_最新50件を返す")
    void search_whenQueryIsNull_returnsLatestPosts() {
        Post post = new Post("alice", "最新投稿", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of(post));

        List<PostDto> actual = postService.search(null);

        assertThat(actual)
                .extracting(PostDto::body)
                .containsExactly("最新投稿");
        then(postRepository).should(never()).findTop50ByDeletedAtIsNullAndBodyContainingOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("投稿検索_q空白のみ_最新50件を返す")
    void search_whenQueryIsBlank_returnsLatestPosts() {
        Post post = new Post("alice", "最新投稿", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of(post));

        List<PostDto> actual = postService.search("   ");

        assertThat(actual)
                .extracting(PostDto::body)
                .containsExactly("最新投稿");
        then(postRepository).should(never()).findTop50ByDeletedAtIsNullAndBodyContainingOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("投稿作成_入力正常のとき_投稿者本文投稿日を保存する")
    void create_whenValid_savesPostWithCreatedAt() {
        PostForm form = new PostForm();
        form.setAuthor("alice");
        form.setBody("登録した投稿");
        Instant before = Instant.now();

        postService.create(form);

        Instant after = Instant.now();
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        then(postRepository).should().save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getAuthor()).isEqualTo("alice");
        assertThat(saved.getBody()).isEqualTo("登録した投稿");
        assertThat(saved.getAvatarColor()).isNull();
        assertThat(saved.getCreatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("投稿作成_avatarColor選択_投稿者本文アバター色投稿日を保存する")
    void create_whenAvatarColorSelected_savesPostWithAvatarColor() {
        PostForm form = new PostForm();
        form.setAuthor("alice");
        form.setBody("登録した投稿");
        form.setAvatarColor("blue");

        postService.create(form);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        then(postRepository).should().save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getAuthor()).isEqualTo("alice");
        assertThat(saved.getBody()).isEqualTo("登録した投稿");
        assertThat(saved.getAvatarColor()).isEqualTo("blue");
    }

    @Test
    @DisplayName("投稿詳細_存在する投稿_投稿DTOを返す")
    void findById_whenPostExists_returnsPostDto() {
        Post post = new Post("alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(post));

        Optional<PostDto> actual = postService.findById(1L);

        assertThat(actual).hasValueSatisfying(dto -> {
            assertThat(dto.id()).isEqualTo(post.getId());
            assertThat(dto.author()).isEqualTo("alice");
            assertThat(dto.body()).isEqualTo("詳細本文");
            assertThat(dto.avatarColor()).isNull();
            assertThat(dto.createdAt()).isEqualTo(Instant.parse("2026-05-23T10:00:00Z"));
        });
    }

    @Test
    @DisplayName("投稿詳細_存在しない投稿_空を返す")
    void findById_whenPostDoesNotExist_returnsEmpty() {
        given(postRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

        Optional<PostDto> actual = postService.findById(999L);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("投稿詳細_存在する投稿_投稿といいね状態をまとめて返す")
    void getDetail_whenPostExists_returnsPostDetail() {
        Post post = new Post("alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        PostLike like = new PostLike(post, "abcd1234", Instant.parse("2026-05-23T11:00:00Z"));
        given(postRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(post));
        given(clientHashGenerator.generate("203.0.113.10", "JUnit UA")).willReturn("abcd1234");
        given(postLikeRepository.countByPostId(1L)).willReturn(3L);
        given(postLikeRepository.findByPostIdAndClientHash(1L, "abcd1234")).willReturn(Optional.of(like));

        PostDetailDto actual = postService.getDetail(1L, "203.0.113.10", "JUnit UA");

        assertThat(actual.post())
                .extracting(PostDto::id, PostDto::author, PostDto::body, PostDto::createdAt)
                .containsExactly(post.getId(), "alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        assertThat(actual.likeCount()).isEqualTo(3L);
        assertThat(actual.liked()).isTrue();
    }

    @Test
    @DisplayName("投稿詳細_存在しない投稿_例外を投げる")
    void getDetail_whenPostDoesNotExist_throwsException() {
        given(postRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getDetail(999L, "203.0.113.10", "JUnit UA"))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage("Post not found: 999");
    }

    @Test
    @DisplayName("いいね_詳細画面から押したとき_IPとUserAgentからclientHashを生成してトグルする")
    void toggleLike_whenClientRequestGiven_generatesClientHashAndToggles() {
        Post post = new Post("alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        given(clientHashGenerator.generate("203.0.113.10", "JUnit UA")).willReturn("abcd1234");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndClientHash(1L, "abcd1234")).willReturn(Optional.empty());

        postService.toggleLike(1L, "203.0.113.10", "JUnit UA");

        ArgumentCaptor<PostLike> captor = ArgumentCaptor.forClass(PostLike.class);
        then(postLikeRepository).should().save(captor.capture());
        assertThat(captor.getValue().getClientHash()).isEqualTo("abcd1234");
    }

    @Test
    @DisplayName("いいね_未いいねのclientHash_いいねを追加する")
    void toggleLike_whenClientHasNotLiked_savesLike() {
        Post post = new Post("alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndClientHash(1L, "abcd1234")).willReturn(Optional.empty());

        postService.toggleLike(1L, "abcd1234");

        ArgumentCaptor<PostLike> captor = ArgumentCaptor.forClass(PostLike.class);
        then(postLikeRepository).should().save(captor.capture());
        assertThat(captor.getValue().getPost()).isSameAs(post);
        assertThat(captor.getValue().getClientHash()).isEqualTo("abcd1234");
    }

    @Test
    @DisplayName("いいね_同一clientHashが再度押したとき_いいねを解除する")
    void toggleLike_whenClientAlreadyLiked_deletesLike() {
        Post post = new Post("alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        PostLike like = new PostLike(post, "abcd1234", Instant.parse("2026-05-23T11:00:00Z"));
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndClientHash(1L, "abcd1234")).willReturn(Optional.of(like));

        postService.toggleLike(1L, "abcd1234");

        then(postLikeRepository).should().delete(like);
        then(postLikeRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("いいね_存在しない投稿_例外を投げる")
    void toggleLike_whenPostDoesNotExist_throwsException() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.toggleLike(999L, "abcd1234"))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage("Post not found: 999");
    }

    @Test
    @DisplayName("投稿削除_存在する投稿_物理削除せずdeletedAtを設定する")
    void delete_whenPostExists_marksPostDeletedWithoutPhysicalDelete() {
        Post post = new Post("alice", "削除する投稿", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(post));
        Instant before = Instant.now();

        postService.delete(1L);

        Instant after = Instant.now();
        assertThat(post.getDeletedAt()).isBetween(before, after);
        then(postRepository).should(never()).delete(org.mockito.ArgumentMatchers.any(Post.class));
    }

    @Test
    @DisplayName("投稿削除_存在しない投稿_PostNotFoundExceptionを投げる")
    void delete_whenPostDoesNotExist_throwsException() {
        given(postRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(999L))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessage("Post not found: 999");
    }

    @Test
    @DisplayName("いいね_詳細表示_いいね数を返す")
    void countLikes_whenPostExists_returnsLikeCount() {
        given(postLikeRepository.countByPostId(1L)).willReturn(2L);

        long actual = postService.countLikes(1L);

        assertThat(actual).isEqualTo(2L);
    }

    @Test
    @DisplayName("いいね_詳細表示_同一clientHashがいいね済みならtrueを返す")
    void hasLiked_whenClientAlreadyLiked_returnsTrue() {
        Post post = new Post("alice", "詳細本文", Instant.parse("2026-05-23T10:00:00Z"));
        PostLike like = new PostLike(post, "abcd1234", Instant.parse("2026-05-23T11:00:00Z"));
        given(postLikeRepository.findByPostIdAndClientHash(1L, "abcd1234")).willReturn(Optional.of(like));

        boolean actual = postService.hasLiked(1L, "abcd1234");

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("いいね_詳細表示_同一clientHashが未いいねならfalseを返す")
    void hasLiked_whenClientHasNotLiked_returnsFalse() {
        given(postLikeRepository.findByPostIdAndClientHash(1L, "abcd1234")).willReturn(Optional.empty());

        boolean actual = postService.hasLiked(1L, "abcd1234");

        assertThat(actual).isFalse();
    }
}
