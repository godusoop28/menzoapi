package com.menzo.menzo.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.post.CommentRequest;
import com.menzo.menzo.dto.post.CommentResponse;
import com.menzo.menzo.dto.post.CreatePostRequest;
import com.menzo.menzo.dto.post.PostResponse;
import com.menzo.menzo.dto.post.VoteRequest;
import com.menzo.menzo.service.PostService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public PageResponse<PostResponse> feed(
            @PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal User viewer) {
        return postService.listFeed(pageable, viewer);
    }

    @GetMapping("/posts/featured")
    public PageResponse<PostResponse> featured(
            @PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal User viewer) {
        return postService.listFeatured(pageable, viewer);
    }

    @GetMapping("/posts/bookmarked")
    public PageResponse<PostResponse> bookmarked(
            @PageableDefault(size = 20) Pageable pageable, @AuthenticationPrincipal User me) {
        return postService.listBookmarked(me, pageable);
    }

    @GetMapping("/posts/search")
    public PageResponse<PostResponse> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User viewer) {
        return postService.search(query, pageable, viewer);
    }

    @GetMapping("/users/{id}/posts")
    public PageResponse<PostResponse> byAuthor(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User viewer) {
        return postService.listByAuthor(id, pageable, viewer);
    }

    @GetMapping("/posts/{id}")
    public PostResponse getPost(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        return postService.getPost(id, viewer);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@AuthenticationPrincipal User me, @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(me, request);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        postService.deletePost(me, id);
    }

    @PutMapping("/posts/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        postService.like(me, id);
    }

    @DeleteMapping("/posts/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        postService.unlike(me, id);
    }

    @PutMapping("/posts/{id}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bookmark(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        postService.bookmark(me, id);
    }

    @DeleteMapping("/posts/{id}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbookmark(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        postService.unbookmark(me, id);
    }

    @PostMapping("/posts/{id}/vote")
    public PostResponse vote(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody VoteRequest request) {
        return postService.votePoll(me, id, request.optionId());
    }

    @GetMapping("/posts/{id}/comments")
    public PageResponse<CommentResponse> comments(
            @PathVariable UUID id, @PageableDefault(size = 30) Pageable pageable) {
        return postService.listComments(id, pageable);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody CommentRequest request) {
        return postService.addComment(me, id, request);
    }
}
