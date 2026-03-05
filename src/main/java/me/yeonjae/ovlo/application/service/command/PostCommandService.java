package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateCommentCommand;
import me.yeonjae.ovlo.application.dto.command.CreatePostCommand;
import me.yeonjae.ovlo.application.dto.command.DeletePostCommand;
import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.in.post.CreateCommentUseCase;
import me.yeonjae.ovlo.application.port.in.post.CreatePostUseCase;
import me.yeonjae.ovlo.application.port.in.post.DeletePostUseCase;
import me.yeonjae.ovlo.application.port.in.post.ReactToPostUseCase;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import me.yeonjae.ovlo.domain.post.model.ReactionType;
import org.springframework.stereotype.Service;

@Service
public class PostCommandService
        implements CreatePostUseCase, CreateCommentUseCase, ReactToPostUseCase, DeletePostUseCase {

    private final LoadPostPort loadPostPort;
    private final SavePostPort savePostPort;

    public PostCommandService(LoadPostPort loadPostPort, SavePostPort savePostPort) {
        this.loadPostPort = loadPostPort;
        this.savePostPort = savePostPort;
    }

    @Override
    public PostResult create(CreatePostCommand command) {
        Post post = Post.create(
                new BoardId(command.boardId()),
                new MemberId(command.authorId()),
                command.title(),
                command.content()
        );
        return PostResult.from(savePostPort.save(post));
    }

    @Override
    public CommentResult createComment(CreateCommentCommand command) {
        Post post = loadPostPort.findById(new PostId(command.postId()))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + command.postId()));

        var comment = post.addComment(new MemberId(command.authorId()), command.content());
        savePostPort.save(post);
        return CommentResult.from(comment);
    }

    @Override
    public void react(ReactToPostCommand command) {
        Post post = loadPostPort.findById(new PostId(command.postId()))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + command.postId()));

        ReactionType type = ReactionType.valueOf(command.reactionType());
        post.react(new MemberId(command.memberId()), type);
        savePostPort.save(post);
    }

    @Override
    public void unreact(ReactToPostCommand command) {
        Post post = loadPostPort.findById(new PostId(command.postId()))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + command.postId()));

        post.unreact(new MemberId(command.memberId()));
        savePostPort.save(post);
    }

    @Override
    public void delete(DeletePostCommand command) {
        Post post = loadPostPort.findById(new PostId(command.postId()))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + command.postId()));

        if (!post.getAuthorId().equals(new MemberId(command.requesterId()))) {
            throw new PostException("게시글을 삭제할 권한이 없습니다");
        }
        post.delete();
        savePostPort.save(post);
    }
}
