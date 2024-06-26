package project.domain.article.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.domain.article.collection.CommentCollection;
import project.domain.article.dto.request.ArticleCreateDto;
import project.domain.article.dto.request.ArticleUpdateDto;
import project.domain.article.dto.request.CommentDto;
import project.domain.article.dto.response.ArticleCreateResDto;
import project.domain.article.dto.response.ArticlePageDto;
import project.domain.article.dto.response.SimpleArticleDto;
import project.domain.article.service.ArticleService;
import project.domain.article.service.CommentService;
import project.domain.photo.entity.Photo;
import project.global.result.ResultCode;
import project.global.result.ResultResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/article")
public class ArticleController implements ArticleDocs{


    private final ArticleService articleService;
    private final CommentService commentService;


    @Override
    @PostMapping(value = "/create/{partyId}", consumes = {"application/json", "multipart/form-data"})
    public ResponseEntity<ResultResponse> create(@PathVariable(name = "partyId") Long partyId, @RequestPart(name = "articleCreateDto") ArticleCreateDto articleCreateDto, @RequestPart(name = "multipartFiles", required = false) List<MultipartFile> multipartFiles) {
        ArticleCreateResDto res = articleService.createArticle(partyId, articleCreateDto, multipartFiles);
        return ResponseEntity.ok(new ResultResponse(ResultCode.CREATE_ARTICLE_SUCCESS, res));
    }

    @Override
    @GetMapping("/detail/{partyId}/{articleId}")
    public ResponseEntity<ResultResponse> articleDetail(@PathVariable(name = "partyId") Long partyId, @PathVariable(name = "articleId") Long articleId) {
        SimpleArticleDto res = articleService.getArticleDetail(partyId, articleId);
        return ResponseEntity.ok(new ResultResponse(ResultCode.GET_ARTICLE_SUCCESS, res));
    }
    @Override
    @PatchMapping("/update/{partyId}/{articleId}")
    public ResponseEntity<SimpleArticleDto> updateArticle(@PathVariable(name = "partyId")Long partyId, @PathVariable(name = "articleId") Long articleId, @RequestPart ArticleUpdateDto articleUpdateDto, @RequestPart(required = false) List<MultipartFile> multipartFiles) {
        SimpleArticleDto res = articleService.updateArticle(partyId, articleId, articleUpdateDto, multipartFiles);
        return ResponseEntity.ok(res);
    }

    @Override
    @DeleteMapping("/delete/{partyId}/{articleId}")
    public ResponseEntity<ResultResponse> deleteArticle(@PathVariable(name = "partyId") Long partyId, @PathVariable(name = "articleId") Long articleId) {
        articleService.deleteArticle(partyId, articleId);
        return null;
    }

    @Override
    @GetMapping("/move-photo/{fileId}")
    public String movePhoto(HttpServletResponse response, @PathVariable(name = "fileId")String fileId) throws IOException {
//        response.sendRedirect(String.format("https://k10d202.p.ssafy.io/album/detail/%s", fileId));
//        response.sendRedirect(String.format("http://localhost:5173/album/detail/%s", fileId));

        return String.format("https://k10d202.p.ssafy.io/album/detail/%s", fileId);
//        return String.format("http://localhost:5173/album/detail/%s", fileId);
    }

    @Override
    @GetMapping("/move-article/{partyId}/{articleId}")
    public String moveArticle(HttpServletResponse response, Long articleId) throws IOException {

//        response.sendRedirect(String.format("https://k10d202.p.ssafy.io/feed/%d", articleId));
//        response.sendRedirect(String.format("http://localhost:5173/feed/%d", articleId));

        return String.format("https://k10d202.p.ssafy.io/feed/%d", articleId);
//        return String.format("http://localhost:5173/feed/%d", articleId);
    }

    @Override
    @GetMapping("/comment/{articleId}")
    public ResponseEntity<ResultResponse> articleComment(@PathVariable(name = "articleId") Long articleId) {

        CommentCollection res = commentService.articleComment(articleId);

        return ResponseEntity.ok(new ResultResponse(ResultCode.GET_COMMENT_SUCCESS, res));
    }

    @Override
    @PostMapping("/comment/enter/{articleId}")
    public ResponseEntity<ResultResponse> enterComment(@PathVariable(name = "articleId")Long articleId, @RequestBody CommentDto commentDto) {

        commentService.enterComment(articleId, commentDto);

        return ResponseEntity.ok(new ResultResponse(ResultCode.ENTER_COMMENT_SUCCESS));
    }

    @Override
    @PatchMapping("/comment/modify/{articleId}/{commentIdx}")
    public ResponseEntity<ResultResponse> modifyComment(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "commentIdx") Integer commentIdx, @RequestBody CommentDto commentDto) {

        commentService.modifyComment(articleId,commentIdx,commentDto);

        return ResponseEntity.ok(new ResultResponse(ResultCode.MODIFY_MEMO_SUCCESS));
    }

    @Override
    @DeleteMapping("/comment/{articleId}/{commentIdx}")
    public ResponseEntity<ResultResponse> deleteComment(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "commentIdx") Integer commentIdx) {

        commentService.deleteComment(articleId, commentIdx);

        return ResponseEntity.ok(new ResultResponse(ResultCode.DELETE_COMMENT_SUCCESS));
    }

    @Override
    @PostMapping("/reply/enter/{articleId}/{commentIdx}")
    public ResponseEntity<ResultResponse> enterReply(@PathVariable(name = "articleId") Long articleId,@PathVariable(name = "commentIdx") Integer commentIdx, @RequestBody CommentDto commentDto) {
        commentService.enterReply(articleId,commentIdx,commentDto);

        return ResponseEntity.ok(new ResultResponse(ResultCode.ENTER_COMMENT_SUCCESS));
    }

    @Override
    @PatchMapping("/reply/modify/{articleId}/{commentIdx}/{replyIdx}")
    public ResponseEntity<ResultResponse> modifyReply(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "commentIdx") Integer commentIdx, @PathVariable(name = "replyIdx") Integer replyIdx, @RequestBody CommentDto commentDto) {
        commentService.modifyReply(articleId,commentIdx,replyIdx,commentDto);
        return ResponseEntity.ok(new ResultResponse(ResultCode.MODIFY_MEMO_SUCCESS));
    }

    @Override
    @DeleteMapping("/reply/{articleId}/{commentIdx}/{replyIdx}")
    public ResponseEntity<ResultResponse> deleteReply(@PathVariable(name = "articleId") Long articleId, @PathVariable(name = "commentIdx") Integer commentIdx, @PathVariable(name = "replyIdx") Integer replyIdx) {
        commentService.deleteReply(articleId,commentIdx,replyIdx);
        return ResponseEntity.ok(new ResultResponse(ResultCode.DELETE_COMMENT_SUCCESS));
    }

    @Override
    @GetMapping("/article-list/{partyId}")
    public ResponseEntity<ArticlePageDto> getArticleList(@PathVariable(name = "partyId") Long partyId, @PageableDefault(sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ArticlePageDto res = articleService.getArticleList(partyId, pageable);
        return ResponseEntity.ok(res);
    }




}
